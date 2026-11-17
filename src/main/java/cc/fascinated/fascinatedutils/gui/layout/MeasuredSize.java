package cc.fascinated.fascinatedutils.gui.layout;

/**
 * The size a node wants for a given set of constraints, as returned by the measure pass. It excludes the node's own
 * margins, which the parent adds when it stacks the node.
 *
 * @param width  the desired border-box width in pixels
 * @param height the desired border-box height in pixels
 */
public record MeasuredSize(int width, int height) {
}
