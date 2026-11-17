package cc.fascinated.fascinatedutils.gui.core;

import com.mojang.blaze3d.platform.Window;
import lombok.experimental.UtilityClass;
import net.minecraft.client.Minecraft;

@UtilityClass
public class UiScale {
    public static final float SCALE = 2f;
    private static final int MIN_ROOMY_HEIGHT = 480;
    private static final float HEIGHT_PER_SCALE_STEP = 540f;
    private static final float GUI_SCALE_FRACTION = 0.5f;

    private static int cachedWindowWidth = -1;
    private static int cachedWindowHeight = -1;
    private static int cachedGuiScale = -1;
    private static float cachedScreenScale = 1f;

    /**
     * Pixels per UI pixel for mod screens. Follows Minecraft's GUI scale setting (half of it, rounded to a whole number; 1080p
     * GUI scale 4 gives 2x) but never drops below a legibility floor that grows with
     * the window height. The HUD canvas does not use this; it stays at {@link #SCALE}. The value is cached until the
     * window size or GUI scale changes.
     *
     * @return framebuffer pixels per screen UI pixel
     */
    public static float screenScale() {
        Window window = Minecraft.getInstance().getWindow();
        int windowWidth = window.getWidth();
        int windowHeight = window.getHeight();
        int guiScale = window.getGuiScale();
        if (windowWidth != cachedWindowWidth || windowHeight != cachedWindowHeight || guiScale != cachedGuiScale) {
            cachedScreenScale = computeScreenScale(windowHeight, guiScale);
            cachedWindowWidth = windowWidth;
            cachedWindowHeight = windowHeight;
            cachedGuiScale = guiScale;
        }
        return cachedScreenScale;
    }

    private static float computeScreenScale(int windowHeight, int guiScale) {
        // Legibility floor: a small GUI scale on a tall window would otherwise draw 8 px text. Height / 540 scales
        // up on 1440p and 4K. The result is snapped to a whole number because the vanilla font is a bitmap: one font
        // pixel must cover a whole number of framebuffer pixels or the text blurs. Windows at least
        // MIN_ROOMY_HEIGHT tall prefer at least 2 (about 18 px text).
        float legibleMinimum = windowHeight / HEIGHT_PER_SCALE_STEP;
        int snapped = Math.round(Math.max(legibleMinimum, guiScale * GUI_SCALE_FRACTION));
        snapped = Math.max(windowHeight >= MIN_ROOMY_HEIGHT ? 2 : 1, snapped);
        // Never draw bigger than vanilla text: one vanilla font pixel is guiScale framebuffer pixels. This cap wins over the floor.
        return Math.min(snapped, Math.max(1, guiScale));
    }

    /**
     * Screen canvas width in screen UI pixels.
     *
     * @return canvas width in UI pixels
     */
    public static float screenWidth() {
        return Math.max(1f, Minecraft.getInstance().getWindow().getWidth() / screenScale());
    }

    /**
     * Screen canvas height in screen UI pixels.
     *
     * @return canvas height in UI pixels
     */
    public static float screenHeight() {
        return Math.max(1f, Minecraft.getInstance().getWindow().getHeight() / screenScale());
    }

    /**
     * Pointer X in screen UI pixels.
     *
     * @return pointer X in UI pixels
     */
    public static float screenPointerX() {
        return (float) (hiResX() / screenScale());
    }

    /**
     * Pointer Y in screen UI pixels.
     *
     * @return pointer Y in UI pixels
     */
    public static float screenPointerY() {
        return (float) (hiResY() / screenScale());
    }

    /**
     * Fixed UI canvas width: half the framebuffer width, unconditionally.
     *
     * @return canvas width in UI pixels
     */
    public static float uiWidth() {
        return Math.max(1f, Minecraft.getInstance().getWindow().getWidth() / SCALE);
    }

    /**
     * Fixed UI canvas height: half the framebuffer height, unconditionally.
     *
     * @return canvas height in UI pixels
     */
    public static float uiHeight() {
        return Math.max(1f, Minecraft.getInstance().getWindow().getHeight() / SCALE);
    }

    /**
     * Pointer X in fixed scale-2 UI space.
     *
     * @return pointer X in UI pixels
     */
    public static float uiPointerX() {
        return (float) (hiResX() / SCALE);
    }

    /**
     * Pointer Y in fixed scale-2 UI space.
     *
     * @return pointer Y in UI pixels
     */
    public static float uiPointerY() {
        return (float) (hiResY() / SCALE);
    }

    /**
     * Pointer X in framebuffer-pixel-equivalent units (logical GUI pointer scaled by the framebuffer ratio).
     *
     * @return pointer X in framebuffer pixels
     */
    public static float hiResPointerX() {
        return (float) hiResX();
    }

    private static double hiResX() {
        Window window = Minecraft.getInstance().getWindow();
        double framebufferRatio = window.getWidth() / (double) Math.max(1, window.getGuiScaledWidth());
        return Minecraft.getInstance().mouseHandler.getScaledXPos(window) * framebufferRatio;
    }

    /**
     * Pointer Y in framebuffer-pixel-equivalent units (logical GUI pointer scaled by the framebuffer ratio).
     *
     * @return pointer Y in framebuffer pixels
     */
    public static float hiResPointerY() {
        return (float) hiResY();
    }

    private static double hiResY() {
        Window window = Minecraft.getInstance().getWindow();
        double framebufferRatio = window.getHeight() / (double) Math.max(1, window.getGuiScaledHeight());
        return Minecraft.getInstance().mouseHandler.getScaledYPos(window) * framebufferRatio;
    }
}
