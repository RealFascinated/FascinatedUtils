package cc.fascinated.fascinatedutils.systems.hud.anchor;

import lombok.experimental.UtilityClass;

/**
 * Derives how HUD panel content should align from the widget's {@link HUDWidgetAnchor}.
 */
@UtilityClass
public class HudAnchorContentAlignment {

    /**
     * Horizontal alignment for the given anchor: left-edge widgets grow toward +X, right-edge toward -X,
     * center column uses centered text blocks.
     */
    public static Horizontal horizontal(HUDWidgetAnchor anchor) {
        return switch (anchor) {
            case TOP_LEFT, BOTTOM_LEFT, LEFT -> Horizontal.LEFT;
            case TOP_RIGHT, BOTTOM_RIGHT, RIGHT -> Horizontal.RIGHT;
            case TOP, BOTTOM, CENTER -> Horizontal.CENTER;
        };
    }

    /**
     * Horizontal alignment of text and row content inside the panel.
     */
    public enum Horizontal {
        LEFT, CENTER, RIGHT
    }
}
