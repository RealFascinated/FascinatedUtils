package cc.fascinated.fascinatedutils.systems.hud.anchor;

public enum HUDWidgetAnchor {
    TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT, TOP, BOTTOM, LEFT, RIGHT, CENTER;

    /**
     * Reference X in screen space used to pick the nearest anchor from a widget center (pixels).
     *
     * @param canvasWidth logical canvas width
     * @return reference X
     */
    public float referenceX(float canvasWidth) {
        return switch (this) {
            case TOP_LEFT, BOTTOM_LEFT, LEFT -> 0f;
            case TOP_RIGHT, BOTTOM_RIGHT, RIGHT -> canvasWidth;
            case TOP, BOTTOM, CENTER -> canvasWidth * 0.5f;
        };
    }

    /**
     * Reference Y in screen space used to pick the nearest anchor from a widget center (pixels).
     * \
     *
     * @param canvasHeight logical canvas height
     * @return reference Y
     */
    public float referenceY(float canvasHeight) {
        return switch (this) {
            case TOP_LEFT, TOP_RIGHT, TOP -> 0f;
            case BOTTOM_LEFT, BOTTOM_RIGHT, BOTTOM -> canvasHeight;
            case LEFT, RIGHT, CENTER -> canvasHeight * 0.5f;
        };
    }
}
