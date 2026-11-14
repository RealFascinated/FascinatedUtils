package cc.fascinated.fascinatedutils.gui.core;

import com.mojang.blaze3d.platform.Window;
import lombok.experimental.UtilityClass;
import net.minecraft.client.Minecraft;

@UtilityClass
public class UIScale {
    public static final float SCALE = 2f;
    private static final int MIN_ROOMY_HEIGHT = 480;
    private static final float HEIGHT_PER_SCALE_STEP = 540f;
    private static final float GUI_SCALE_FRACTION = 0.5f;

    /**
     * Pixels per UI pixel for mod screens. Follows Minecraft's GUI scale setting (half of it, rounded to a whole number; 1080p
     * GUI scale 4 gives 2x) but never drops below a legibility floor that grows with
     * the window height. The HUD canvas does not use this; it stays at {@link #SCALE}.
     *
     * @return framebuffer pixels per screen UI pixel
     */
    public static float screenScale() {
        Window window = Minecraft.getInstance().getWindow();
        // Legibility floor: a small GUI scale on a tall window would otherwise draw 8 px text. Height / 540 scales
        // up on 1440p and 4K. The result is snapped to a whole number because the vanilla font is a bitmap: one font
        // pixel must cover a whole number of framebuffer pixels or the text blurs. Windows at least
        // MIN_ROOMY_HEIGHT tall go no lower than 2 (about 18 px text), but never above the GUI scale.
        float legibleMinimum = window.getHeight() / HEIGHT_PER_SCALE_STEP;
        int snapped = Math.max(1, Math.round(Math.max(legibleMinimum, window.getGuiScale() * GUI_SCALE_FRACTION)));
        if (window.getHeight() >= MIN_ROOMY_HEIGHT) {
            snapped = Math.max(2, snapped);
        }
        // Never draw bigger than vanilla text: one vanilla font pixel is guiScale framebuffer pixels.
        return Math.min(snapped, Math.max(1, window.getGuiScale()));
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
        return hiResPointerX() / screenScale();
    }

    /**
     * Pointer Y in screen UI pixels.
     *
     * @return pointer Y in UI pixels
     */
    public static float screenPointerY() {
        return hiResPointerY() / screenScale();
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
        return hiResPointerX() / SCALE;
    }

    /**
     * Pointer Y in fixed scale-2 UI space.
     *
     * @return pointer Y in UI pixels
     */
    public static float uiPointerY() {
        return hiResPointerY() / SCALE;
    }

    /**
     * Pointer X in framebuffer-pixel-equivalent units (logical GUI pointer scaled by the framebuffer ratio).
     *
     * @return pointer X in framebuffer pixels
     */
    public static float hiResPointerX() {
        Window window = Minecraft.getInstance().getWindow();
        return (float) Minecraft.getInstance().mouseHandler.getScaledXPos(window) * (window.getWidth() / Math.max(1f, (float) window.getGuiScaledWidth()));
    }

    /**
     * Pointer Y in framebuffer-pixel-equivalent units (logical GUI pointer scaled by the framebuffer ratio).
     *
     * @return pointer Y in framebuffer pixels
     */
    public static float hiResPointerY() {
        Window window = Minecraft.getInstance().getWindow();
        return (float) Minecraft.getInstance().mouseHandler.getScaledYPos(window) * (window.getHeight() / Math.max(1f, (float) window.getGuiScaledHeight()));
    }

}
