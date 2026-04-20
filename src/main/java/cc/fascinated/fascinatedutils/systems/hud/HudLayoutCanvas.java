package cc.fascinated.fascinatedutils.systems.hud;

import cc.fascinated.fascinatedutils.gui.UIScale;

public class HudLayoutCanvas {

    private HudLayoutCanvas() {
    }

    /**
     * Clamps a width or height to a safe finite range for HUD layout and anchor math.
     *
     * @param extent extent in UI pixels
     * @return clamped extent in UI pixels, at least {@code 1f}
     */
    public static float clampExtent(float extent) {
        if (!Float.isFinite(extent) || extent <= 0f) {
            return 1f;
        }
        return Math.min(extent, 8192f);
    }

    /**
     * Canvas width for the current frame in fixed scale-2 UI pixels.
     *
     * @return clamped UI canvas width
     */
    public static float width() {
        return clampExtent(UIScale.uiWidth());
    }

    /**
     * Canvas height for the current frame in fixed scale-2 UI pixels.
     *
     * @return clamped UI canvas height
     */
    public static float height() {
        return clampExtent(UIScale.uiHeight());
    }
}
