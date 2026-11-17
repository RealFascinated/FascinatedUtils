package cc.fascinated.fascinatedutils.gui.render;

import lombok.experimental.UtilityClass;

@UtilityClass
public class RectCornerRoundMask {

    /**
     * No rounded corners (draws an axis-aligned rectangle when this mask alone is used).
     */
    public static final int NONE = 0;
    /**
     * Rounds the top left corner.
     */
    public static final int TOP_LEFT = 1;
    /**
     * Rounds the top right corner.
     */
    public static final int TOP_RIGHT = 2;
    /**
     * Rounds the bottom left corner.
     */
    public static final int BOTTOM_LEFT = 4;
    /**
     * Rounds the bottom right corner.
     */
    public static final int BOTTOM_RIGHT = 8;
    /**
     * All four corners filleted (default when {@code CORNER_ROUND_MASK} is unset).
     */
    public static final int ALL = TOP_LEFT | TOP_RIGHT | BOTTOM_LEFT | BOTTOM_RIGHT;
    /**
     * Rounds both top corners.
     */
    public static final int TOP = TOP_LEFT | TOP_RIGHT;
    /**
     * Rounds both bottom corners.
     */
    public static final int BOTTOM = BOTTOM_LEFT | BOTTOM_RIGHT;
    /**
     * Rounds both left corners.
     */
    public static final int LEFT = TOP_LEFT | BOTTOM_LEFT;
    /**
     * Rounds both right corners.
     */
    public static final int RIGHT = TOP_RIGHT | BOTTOM_RIGHT;
}
