package cc.fascinated.fascinatedutils.gui2.layout;

/**
 * Horizontal and vertical axis constraints for one node.
 */
public class BoxLayoutSpec {
    private final AxisConstraints horizontal = new AxisConstraints();
    private final AxisConstraints vertical = new AxisConstraints();

    public AxisConstraints horizontal() {
        return horizontal;
    }

    public AxisConstraints vertical() {
        return vertical;
    }
}
