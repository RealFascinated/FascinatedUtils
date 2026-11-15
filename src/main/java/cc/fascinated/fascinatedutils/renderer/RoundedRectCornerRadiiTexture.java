package cc.fascinated.fascinatedutils.renderer;

import cc.fascinated.fascinatedutils.AlumiteMod;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.renderer.texture.DynamicTexture;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

public class RoundedRectCornerRadiiTexture {
    private static final AtomicLong DISPOSABLE_LUT_SEQUENCE = new AtomicLong();
    private static final int MAX_CACHED_LUTS = 256;
    private static final Map<Long, DynamicTexture> CACHED_LUTS = new HashMap<>();

    private RoundedRectCornerRadiiTexture() {
    }

    /**
     * Small GPU texture unique to one LUT rounded draw. Vanilla runs all {@code prepareSimpleElement} uploads before
     * any fragment sampling, so a single shared LUT texture cannot work for batched quads; bake radii at enqueue time
     * instead.
     */
    public static DynamicTexture createDisposableRadiiLut(int packedCornerRadii, float lutRingStrokePx, float lutExpansionShaderPx) {
        NativeImage image = new NativeImage(NativeImage.Format.RGBA, 6, 1, false);
        writeRadiiLutPixels(image, packedCornerRadii, lutRingStrokePx, lutExpansionShaderPx);
        long sequence = DISPOSABLE_LUT_SEQUENCE.getAndIncrement();
        DynamicTexture texture = new DynamicTexture(() -> AlumiteMod.MOD_ID + "/disposable_corner_lut/" + sequence, image);
        texture.upload();
        return texture;
    }

    /**
     * Returns a shared radii LUT for the given parameters, creating it on first use. The texture stays alive across
     * frames; callers must not close it. Identical parameters always produce identical pixels, so sharing is safe.
     *
     * @param packedCornerRadii    packed corner radius bytes
     * @param lutRingStrokePx      logical stroke thickness in pixels
     * @param lutExpansionShaderPx shader expansion in pixels
     * @return the cached texture
     */
    public static DynamicTexture cachedRadiiLut(int packedCornerRadii, float lutRingStrokePx, float lutExpansionShaderPx) {
        long key = ((long) packedCornerRadii << 16) | ((long) lutByte(lutRingStrokePx) << 8) | lutByte(lutExpansionShaderPx);
        DynamicTexture texture = CACHED_LUTS.get(key);
        if (texture == null) {
            texture = createDisposableRadiiLut(packedCornerRadii, lutRingStrokePx, lutExpansionShaderPx);
            CACHED_LUTS.put(key, texture);
        }
        return texture;
    }

    /**
     * Closes every cached LUT when the cache has grown past its limit. Call only after the GUI render pass finished.
     */
    public static void trimCachedLuts() {
        if (CACHED_LUTS.size() <= MAX_CACHED_LUTS) {
            return;
        }
        for (DynamicTexture texture : CACHED_LUTS.values()) {
            texture.close();
        }
        CACHED_LUTS.clear();
    }

    private static int lutByte(float pixels) {
        return pixels > 0f ? Math.max(1, Math.min(255, Math.round(pixels))) : 0;
    }

    private static void writeRadiiLutPixels(NativeImage image, int packedCornerRadii, float lutRingStrokePx, float lutExpansionShaderPx) {
        int topLeft = (packedCornerRadii >>> 24) & 0xFF;
        int topRight = (packedCornerRadii >>> 16) & 0xFF;
        int bottomRight = (packedCornerRadii >>> 8) & 0xFF;
        int bottomLeft = packedCornerRadii & 0xFF;
        image.setPixel(0, 0, grey(topLeft));
        image.setPixel(1, 0, grey(topRight));
        image.setPixel(2, 0, grey(bottomRight));
        image.setPixel(3, 0, grey(bottomLeft));
        image.setPixel(4, 0, grey(lutByte(lutRingStrokePx)));
        image.setPixel(5, 0, grey(lutByte(lutExpansionShaderPx)));
    }

    private static int grey(int channel) {
        return 0xFF000000 | (channel << 16) | (channel << 8) | channel;
    }
}
