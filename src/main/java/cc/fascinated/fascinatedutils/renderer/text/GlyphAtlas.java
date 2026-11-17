package cc.fascinated.fascinatedutils.renderer.text;

import cc.fascinated.fascinatedutils.AlumiteMod;
import cc.fascinated.fascinatedutils.client.Client;
import cc.fascinated.fascinatedutils.renderer.MeshRenderer;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import org.joml.Matrix3x2f;
import org.jspecify.annotations.Nullable;
import org.lwjgl.stb.STBTTPackContext;
import org.lwjgl.stb.STBTTPackedchar;
import org.lwjgl.stb.STBTTAlignedQuad;
import org.lwjgl.stb.STBTruetype;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

/**
 * A TrueType font baked once with stb_truetype (2x2 oversampled) into a linearly filtered coverage atlas and drawn as
 * float-positioned quads. This avoids vanilla's nearest-filtered glyph sampling, which makes custom fonts look thin and
 * jagged on the fixed-scale GUI canvas.
 */
public class GlyphAtlas {
    private static final int ATLAS_SIZE = 1024;
    private static final float EM_LOGICAL_PX = 10f;
    private static final float TEXELS_PER_LOGICAL_PX = 3f;
    private static final float BASELINE_OFFSET = 8f;
    private static final double COVERAGE_GAMMA = 0.8;
    private static final int[][] RANGES = {{32, 95}, {160, 96}};
    private static final GlyphAtlas REGULAR = new GlyphAtlas("font/inter_regular.ttf");
    private static final GlyphAtlas BOLD = new GlyphAtlas("font/inter_bold.ttf");

    private final String resourcePath;
    private final Glyph[] glyphs = new Glyph[256];
    private boolean baked;
    private boolean failed;
    @Nullable
    private DynamicTexture texture;
    @Nullable
    private TextureSetup textureSetup;

    private GlyphAtlas(String resourcePath) {
        this.resourcePath = resourcePath;
    }

    public static GlyphAtlas get(boolean bold) {
        return bold ? BOLD : REGULAR;
    }

    /**
     * Whether every character of the text has a baked glyph; callers fall back to the vanilla font otherwise.
     *
     * @param text the text to check
     * @return true when the atlas can measure and draw the whole string
     */
    public boolean canRender(String text) {
        ensureBaked();
        if (failed) {
            return false;
        }
        for (int index = 0; index < text.length(); index++) {
            char character = text.charAt(index);
            if (character >= glyphs.length || glyphs[character] == null) {
                return false;
            }
        }
        return true;
    }

    /**
     * Measures the text in logical pixels. Only valid when {@link #canRender} returned true.
     *
     * @param text the text to measure
     * @return the advance width in logical pixels
     */
    public float measure(String text) {
        float width = 0f;
        for (int index = 0; index < text.length(); index++) {
            width += glyphs[text.charAt(index)].advance;
        }
        return width;
    }

    /**
     * Queues one quad per glyph. Only valid when {@link #canRender} returned true.
     *
     * @param drawContext active draw context
     * @param scissor     active scissor or null
     * @param text        text to draw
     * @param originX     left origin in logical pixels
     * @param lineTopY    layout line top in logical pixels
     * @param colorArgb   final packed ARGB color
     */
    public void draw(GuiGraphicsExtractor drawContext, @Nullable ScreenRectangle scissor, String text, float originX, float lineTopY, int colorArgb) {
        Matrix3x2f pose = new Matrix3x2f(drawContext.pose());
        float penX = originX;
        float baselineY = lineTopY + BASELINE_OFFSET;
        for (int index = 0; index < text.length(); index++) {
            Glyph glyph = glyphs[text.charAt(index)];
            if (glyph.visible) {
                MeshRenderer.INSTANCE.enqueueGlyphQuad(scissor, textureSetup, pose, penX + glyph.x0, baselineY + glyph.y0, penX + glyph.x1, baselineY + glyph.y1, glyph.u0, glyph.v0, glyph.u1, glyph.v1, colorArgb);
            }
            penX += glyph.advance;
        }
    }

    private void ensureBaked() {
        if (baked || failed) {
            return;
        }
        try {
            bake();
            baked = true;
        }
        catch (IOException | RuntimeException exception) {
            Client.LOG.error("Failed to bake glyph atlas {}", resourcePath, exception);
            failed = true;
        }
    }

    private void bake() throws IOException {
        byte[] fontBytes;
        Identifier fontId = Identifier.fromNamespaceAndPath(AlumiteMod.MOD_ID, resourcePath);
        try (InputStream stream = Minecraft.getInstance().getResourceManager().open(fontId)) {
            fontBytes = stream.readAllBytes();
        }
        ByteBuffer fontBuffer = MemoryUtil.memAlloc(fontBytes.length);
        ByteBuffer bitmap = MemoryUtil.memAlloc(ATLAS_SIZE * ATLAS_SIZE);
        try (MemoryStack stack = MemoryStack.stackPush()) {
            fontBuffer.put(fontBytes).flip();
            STBTTPackContext packContext = STBTTPackContext.malloc(stack);
            if (!STBTruetype.stbtt_PackBegin(packContext, bitmap, ATLAS_SIZE, ATLAS_SIZE, 0, 1, MemoryUtil.NULL)) {
                throw new IllegalStateException("stbtt_PackBegin failed");
            }
            STBTruetype.stbtt_PackSetOversampling(packContext, 2, 2);
            STBTTPackedchar.Buffer[] packed = new STBTTPackedchar.Buffer[RANGES.length];
            for (int rangeIndex = 0; rangeIndex < RANGES.length; rangeIndex++) {
                packed[rangeIndex] = STBTTPackedchar.malloc(RANGES[rangeIndex][1]);
                // A negative size asks stb for an em-based pixel size instead of ascent-to-descent.
                if (!STBTruetype.stbtt_PackFontRange(packContext, fontBuffer, 0, -EM_LOGICAL_PX * TEXELS_PER_LOGICAL_PX, RANGES[rangeIndex][0], packed[rangeIndex])) {
                    throw new IllegalStateException("stbtt_PackFontRange failed, atlas too small");
                }
            }
            STBTruetype.stbtt_PackEnd(packContext);
            FloatBuffer penX = stack.mallocFloat(1);
            FloatBuffer penY = stack.mallocFloat(1);
            STBTTAlignedQuad quad = STBTTAlignedQuad.malloc(stack);
            float inverseScale = 1f / TEXELS_PER_LOGICAL_PX;
            for (int rangeIndex = 0; rangeIndex < RANGES.length; rangeIndex++) {
                for (int offset = 0; offset < RANGES[rangeIndex][1]; offset++) {
                    penX.put(0, 0f);
                    penY.put(0, 0f);
                    STBTruetype.stbtt_GetPackedQuad(packed[rangeIndex], ATLAS_SIZE, ATLAS_SIZE, offset, penX, penY, quad, false);
                    Glyph glyph = new Glyph();
                    glyph.x0 = quad.x0() * inverseScale;
                    glyph.y0 = quad.y0() * inverseScale;
                    glyph.x1 = quad.x1() * inverseScale;
                    glyph.y1 = quad.y1() * inverseScale;
                    glyph.u0 = quad.s0();
                    glyph.v0 = quad.t0();
                    glyph.u1 = quad.s1();
                    glyph.v1 = quad.t1();
                    glyph.advance = penX.get(0) * inverseScale;
                    glyph.visible = quad.x1() > quad.x0() && quad.y1() > quad.y0();
                    glyphs[RANGES[rangeIndex][0] + offset] = glyph;
                }
                packed[rangeIndex].free();
            }
            NativeImage image = new NativeImage(NativeImage.Format.RGBA, ATLAS_SIZE, ATLAS_SIZE, false);
            for (int pixelY = 0; pixelY < ATLAS_SIZE; pixelY++) {
                for (int pixelX = 0; pixelX < ATLAS_SIZE; pixelX++) {
                    double coverage = (bitmap.get(pixelY * ATLAS_SIZE + pixelX) & 0xFF) / 255.0;
                    int alpha = (int) Math.round(Math.pow(coverage, COVERAGE_GAMMA) * 255.0);
                    image.setPixel(pixelX, pixelY, (alpha << 24) | 0x00FFFFFF);
                }
            }
            String textureName = resourcePath;
            texture = new DynamicTexture(() -> AlumiteMod.MOD_ID + "/glyph_atlas/" + textureName, image);
            texture.upload();
            textureSetup = MeshRenderer.INSTANCE.linearTextureSetup(texture);
        }
        finally {
            MemoryUtil.memFree(fontBuffer);
            MemoryUtil.memFree(bitmap);
        }
    }

    private static class Glyph {
        private float x0;
        private float y0;
        private float x1;
        private float y1;
        private float u0;
        private float v0;
        private float u1;
        private float v1;
        private float advance;
        private boolean visible;
    }
}
