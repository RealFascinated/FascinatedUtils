package cc.fascinated.fascinatedutils.systems.hud;

import net.minecraft.client.gui.GuiGraphicsExtractor;

public class HudLayoutCanvas {

    private HudLayoutCanvas() {
    }

    /**
     * Clamps a width or height to a safe finite range for HUD layout and anchor math.
     *
     * @param extent extent in logical pixels
     *
     * @return clamped extent in logical pixels, at least {@code 1f}
     */
    public static float clampExtent(float extent) {
        if (!Float.isFinite(extent) || extent <= 0f) {
            return 1f;
        }
        return Math.min(extent, 8192f);
    }

    /**
     * Logical canvas width for the active {@link GuiGraphicsExtractor} pass (matches vanilla HUD extract bounds).
     *
     * @param graphics current extract context
     *
     * @return clamped logical width for layout and anchors
     */
    public static float width(GuiGraphicsExtractor graphics) {
        return clampExtent((float) graphics.guiWidth());
    }

    /**
     * Logical canvas height for the active {@link GuiGraphicsExtractor} pass.
     *
     * @param graphics current extract context
     *
     * @return clamped logical height for layout and anchors
     */
    public static float height(GuiGraphicsExtractor graphics) {
        return clampExtent((float) graphics.guiHeight());
    }
}
