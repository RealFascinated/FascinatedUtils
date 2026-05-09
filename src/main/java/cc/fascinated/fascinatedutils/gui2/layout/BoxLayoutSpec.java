package cc.fascinated.fascinatedutils.gui2.layout;

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
