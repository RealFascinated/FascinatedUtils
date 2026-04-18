package cc.fascinated.fascinatedutils.renderer.text;

import cc.fascinated.fascinatedutils.gui.GuiDesignSpace;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.util.Mth;

/**
 * Converts GUI {@linkplain TextRenderer} vertical coordinates from a
 * <strong>layout line top</strong> (the Y of the top edge of the nominal line box used with {@link
 * TextRenderer#getHeight(boolean)}) to the Y coordinate expected by vanilla
 * {@link net.minecraft.client.gui.GuiGraphicsExtractor#drawString} (text baseline).
 */
public final class VanillaLineLayoutY {

    private static final float MEASUREMENT_BASELINE_Y = 4096f;
    private static final String MEASUREMENT_SAMPLE = "Mg";
    // Small nudge: layout uses fontHeight line box; prepare() tops out at ink. Not (fontHeight - 1) — that overshoots.
    private static final float LINE_BOX_TOP_ABOVE_INK_PX = 0.5f;

    private static int cachedFontHeightKey = Integer.MIN_VALUE;
    private static float cachedDeltaNoShadow = Float.NaN;
    private static float cachedDeltaShadow = Float.NaN;

    private VanillaLineLayoutY() {
    }

    /**
     * Converts a layout line top to an integer vanilla {@code drawText} Y coordinate.
     *
     * @param layoutLineTopY top of the nominal line box in logical GUI pixels (matches {@link
     *                       TextRenderer#getHeight(boolean)} per line)
     * @param shadow         whether the draw call uses vanilla text shadow (affects measured spacing)
     * @return Y coordinate suitable for vanilla {@link net.minecraft.client.gui.GuiGraphicsExtractor#drawString}
     */
    public static int layoutLineTopToVanillaDrawTextY(int layoutLineTopY, boolean shadow) {
        return Mth.floor(layoutLineTopY + verticalDelta(shadow));
    }

    /**
     * Converts a layout line top to a vanilla {@code drawText} Y coordinate without flooring.
     *
     * @param layoutLineTopY top of the nominal line box in logical GUI pixels
     * @param shadow         whether the draw call uses vanilla text shadow (affects measured spacing)
     * @return Y coordinate suitable for vanilla {@link net.minecraft.client.gui.GuiGraphicsExtractor#drawString} before integer
     * quantization
     */
    public static float layoutLineTopToVanillaDrawTextYFloat(float layoutLineTopY, boolean shadow) {
        return layoutLineTopY + verticalDelta(shadow);
    }

    private static float verticalDelta(boolean shadow) {
        float base = baseDeltaFromFontTopToBaseline(shadow);
        return GuiDesignSpace.isActive() ? base * GuiDesignSpace.scaleY() : base;
    }

    private static float baseDeltaFromFontTopToBaseline(boolean shadow) {
        Minecraft client = Minecraft.getInstance();
        int fontHeight = client != null ? client.font.lineHeight : 9;
        if (fontHeight != cachedFontHeightKey) {
            cachedFontHeightKey = fontHeight;
            cachedDeltaNoShadow = Float.NaN;
            cachedDeltaShadow = Float.NaN;
        }
        if (shadow) {
            if (Float.isNaN(cachedDeltaShadow)) {
                cachedDeltaShadow = measureDelta(shadow);
            }
            return cachedDeltaShadow;
        }
        if (Float.isNaN(cachedDeltaNoShadow)) {
            cachedDeltaNoShadow = measureDelta(shadow);
        }
        return cachedDeltaNoShadow;
    }

    private static float measureDelta(boolean shadow) {
        Minecraft client = Minecraft.getInstance();
        if (client == null) {
            return fallbackLineTopToBaselinePx(fontHeightFallback());
        }
        net.minecraft.client.gui.Font vanilla = client.font;
        try {
            net.minecraft.client.gui.Font.PreparedText drawable = vanilla.prepareText(MEASUREMENT_SAMPLE, 0f, MEASUREMENT_BASELINE_Y, 0xFFFFFF, shadow, 0);
            ScreenRectangle rect = drawable.bounds();
            assert rect != null;
            if (rect.width() <= 0 || rect.height() <= 0) {
                return fallbackLineTopToBaselinePx(vanilla.lineHeight);
            }
            /*
             * rect.getTop() is tight ink; layout uses the full fontHeight line box, whose top sits slightly above the
             * ink. Add a small constant — do not floor at (fontHeight - 1), which overshoots for typical measured deltas.
             */
            float inkTopToBaseline = MEASUREMENT_BASELINE_Y - rect.top();
            return inkTopToBaseline + LINE_BOX_TOP_ABOVE_INK_PX;
        } catch (RuntimeException ignored) {
            return fallbackLineTopToBaselinePx(vanilla.lineHeight);
        }
    }

    private static float fallbackLineTopToBaselinePx(int fontHeight) {
        return Math.max(1f, fontHeight - 1.5f);
    }

    private static int fontHeightFallback() {
        Minecraft client = Minecraft.getInstance();
        return client != null ? client.font.lineHeight : 9;
    }
}
