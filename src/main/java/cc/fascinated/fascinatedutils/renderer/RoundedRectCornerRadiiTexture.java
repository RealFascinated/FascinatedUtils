package cc.fascinated.fascinatedutils.renderer;

import cc.fascinated.fascinatedutils.FascinatedUtils;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.Identifier;

import java.util.concurrent.atomic.AtomicLong;

public class RoundedRectCornerRadiiTexture {
    public static final RoundedRectCornerRadiiTexture INSTANCE = new RoundedRectCornerRadiiTexture();
    private static final Identifier TEXTURE_ID = Identifier.fromNamespaceAndPath(FascinatedUtils.MOD_ID, "gpu/rounded_corner_radii_lut");
    private static final AtomicLong DISPOSABLE_LUT_SEQUENCE = new AtomicLong();
    private DynamicTexture backing;

    private RoundedRectCornerRadiiTexture() {
    }

    /**
     * Small GPU texture unique to one LUT rounded draw. Vanilla runs all {@code prepareSimpleElement} uploads before
     * any fragment sampling, so a single shared LUT texture cannot work for batched quads; bake radii at enqueue time
     * instead and {@link MeshRenderer#releaseDisposableRadiiLutsAfterGuiRenderPass()} after {@code GuiRenderer#render}.
     */
    public static DynamicTexture createDisposableRadiiLut(int packedCornerRadii, float lutRingStrokePx) {
        NativeImage image = new NativeImage(NativeImage.Format.RGBA, 5, 1, false);
        writeRadiiLutPixels(image, packedCornerRadii, lutRingStrokePx);
        long sequence = DISPOSABLE_LUT_SEQUENCE.getAndIncrement();
        DynamicTexture texture = new DynamicTexture(() -> FascinatedUtils.MOD_ID + "/disposable_corner_lut/" + sequence, image);
        texture.upload();
        return texture;
    }

    private static void writeRadiiLutPixels(NativeImage image, int packedCornerRadii, float lutRingStrokePx) {
        int topLeft = (packedCornerRadii >>> 24) & 0xFF;
        int topRight = (packedCornerRadii >>> 16) & 0xFF;
        int bottomRight = (packedCornerRadii >>> 8) & 0xFF;
        int bottomLeft = packedCornerRadii & 0xFF;
        image.setPixel(0, 0, 0xFF000000 | (topLeft << 16) | (topLeft << 8) | topLeft);
        image.setPixel(1, 0, 0xFF000000 | (topRight << 16) | (topRight << 8) | topRight);
        image.setPixel(2, 0, 0xFF000000 | (bottomRight << 16) | (bottomRight << 8) | bottomRight);
        image.setPixel(3, 0, 0xFF000000 | (bottomLeft << 16) | (bottomLeft << 8) | bottomLeft);
        int strokeByte = Math.max(0, Math.min(255, Math.round(lutRingStrokePx)));
        image.setPixel(4, 0, 0xFF000000 | (strokeByte << 16) | (strokeByte << 8) | strokeByte);
    }

    /**
     * Register the radii LUT with the client texture manager (idempotent).
     *
     * <p>Do not call from {@code ClientModInitializer} entrypoints: {@link DynamicTexture} needs an
     * initialized render device. Prefer first use during rendering (see {@link MeshRenderer}).
     *
     * @param textureManager active client texture manager
     */
    public void register(TextureManager textureManager) {
        if (backing != null) {
            return;
        }
        NativeImage image = new NativeImage(NativeImage.Format.RGBA, 5, 1, false);
        for (int cornerIndex = 0; cornerIndex < 5; cornerIndex++) {
            image.setPixel(cornerIndex, 0, 0xFF000000);
        }
        backing = new DynamicTexture(TEXTURE_ID::toString, image);
        textureManager.register(TEXTURE_ID, backing);
    }

    /**
     * Upload four radius bytes (0–255) packed as {@code (tl<<24)|(tr<<16)|(br<<8)|bl}, and optional LUT stroke width
     * for {@code fui_rounded_rect} outer-ring mode (texel {@code (4,0)} red channel, 0 = solid fill).
     *
     * @param packedCornerRadii packed corner radius bytes
     * @param lutRingStrokePx   logical stroke thickness in pixels for GPU ring draws; clamped to 0–255
     */
    public void uploadPacked(int packedCornerRadii, float lutRingStrokePx) {
        DynamicTexture texture = backing;
        if (texture == null) {
            return;
        }
        NativeImage image = texture.getPixels();
        if (image == null) {
            return;
        }
        writeRadiiLutPixels(image, packedCornerRadii, lutRingStrokePx);
        texture.upload();
    }

    /**
     * Same as {@link #uploadPacked(int, float)} with no outer-ring stroke (solid fill path).
     */
    public void uploadPacked(int packedCornerRadii) {
        uploadPacked(packedCornerRadii, 0f);
    }

    /**
     * Build a {@link TextureSetup} pairing the white fill texture with this radii LUT for
     * {@link FascinatedUiPipelines#ROUNDED_RECT_TEX_LUT}.
     *
     * @param whiteBlock opaque white {@link AbstractTexture} used for {@code Sampler0}
     * @return combined setup, or {@link TextureSetup#noTexture()} if the client is unavailable
     */
    public TextureSetup dualWithWhite(AbstractTexture whiteBlock) {
        Minecraft client = Minecraft.getInstance();
        if (client == null || backing == null) {
            return TextureSetup.noTexture();
        }
        AbstractTexture radii = client.getTextureManager().getTexture(TEXTURE_ID);
        return TextureSetup.doubleTexture(whiteBlock.getTextureView(), whiteBlock.getSampler(), radii.getTextureView(), radii.getSampler());
    }
}
