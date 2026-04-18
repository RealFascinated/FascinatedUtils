package cc.fascinated.fascinatedutils.gui.core;

public enum TextOverflow {
    /**
     * Draw the full string with no clipping; content may extend past the laid-out width.
     */
    VISIBLE,
    /**
     * Clip painting to the node's width and height (similar to CSS {@code text-overflow: clip} with
     * {@code overflow: hidden}).
     */
    CLIP,
    /**
     * Replace the tail with an ellipsis when the string is wider than the box (similar to CSS
     * {@code text-overflow: ellipsis}).
     */
    ELLIPSIS,
    /**
     * Break the string across lines that fit within the node's width (requires a known layout width); height is one
     * line per wrapped row unless a fixed height is set, in which case extra lines are clipped.
     */
    WRAP
}
