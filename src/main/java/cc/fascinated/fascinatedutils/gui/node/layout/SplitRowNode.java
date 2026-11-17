package cc.fascinated.fascinatedutils.gui.node.layout;

import cc.fascinated.fascinatedutils.gui.layout.PositionedNode;
import cc.fascinated.fascinatedutils.gui.layout.Axis;
import cc.fascinated.fascinatedutils.gui.layout.AxisConstraints;
import cc.fascinated.fascinatedutils.gui.layout.MeasuredSize;
import cc.fascinated.fascinatedutils.gui.render.AlumiteGuiRenderer;
import org.jspecify.annotations.Nullable;

/**
 * A row with one node pushed to the left edge and an optional second node pushed to the right edge. Its own size is
 * the two nodes side by side with a minimum gap, but it takes the whole width of the slot it is placed in, so inside
 * a {@link ColumnNode} every split row spans the column and the right nodes line up, like labels and values.
 */
public class SplitRowNode extends PositionedNode<SplitRowNode> {
    private final PositionedNode<?> start;
    private final @Nullable PositionedNode<?> end;
    private final int minGap;

    /**
     * Creates a split row.
     *
     * @param start  node on the left edge
     * @param end    node on the right edge, or {@code null} for a row that only has a left node
     * @param minGap smallest space between the two nodes in pixels, used for the row's own width
     */
    public SplitRowNode(PositionedNode<?> start, @Nullable PositionedNode<?> end, int minGap) {
        this.start = start;
        this.end = end;
        this.minGap = minGap;
        start.alignX(0f);
        addChild(start);
        if (end != null) {
            end.alignX(1f);
            addChild(end);
        }
    }

    @Override
    protected MeasuredSize doMeasure(AlumiteGuiRenderer renderFrame, int maxWidth, int maxHeight) {
        MeasuredSize startSize = start.measure(renderFrame, maxWidth, maxHeight);
        if (end == null) {
            return startSize;
        }
        MeasuredSize endSize = end.measure(renderFrame, maxWidth, maxHeight);
        return new MeasuredSize(startSize.width() + minGap + endSize.width(), Math.max(startSize.height(), endSize.height()));
    }

    @Override
    public long resolveBox(Axis axis, int slotStart, int slotSize, int measuredSize) {
        if (axis == Axis.HORIZONTAL) {
            return AxisConstraints.packBox(slotStart, slotSize);
        }
        return super.resolveBox(axis, slotStart, slotSize, measuredSize);
    }
}
