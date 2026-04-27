package cc.fascinated.fascinatedutils.systems.hud.anchor;

/**
 * Derives how HUD panel content should align and which logical edge stays fixed when intrinsic
 * width/height changes, from the widget's {@link HUDWidgetAnchor}.
 */
public class HudAnchorContentAlignment {
    private HudAnchorContentAlignment() {
    }

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
     * Vertical alignment for the given anchor: top-edge widgets grow downward, bottom-edge upward,
     * vertical center strip uses vertically centered content.
     */
    public static Vertical vertical(HUDWidgetAnchor anchor) {
        return switch (anchor) {
            case TOP_LEFT, TOP_RIGHT, TOP -> Vertical.TOP;
            case BOTTOM_LEFT, BOTTOM_RIGHT, BOTTOM -> Vertical.BOTTOM;
            case LEFT, RIGHT, CENTER -> Vertical.CENTER;
        };
    }

    /**
     * Horizontal alignment of text and row content inside the panel.
     */
    public enum Horizontal {
        LEFT, CENTER, RIGHT
    }

    /**
     * Vertical alignment of the content block inside the panel.
     */
    public enum Vertical {
        TOP, CENTER, BOTTOM
    }
}
