package cc.fascinated.fascinatedutils.gui.node.layout;

import cc.fascinated.fascinatedutils.gui.layout.PositionedNode;
import cc.fascinated.fascinatedutils.gui.core.UiNode;
import cc.fascinated.fascinatedutils.gui.layout.MeasuredSize;
import cc.fascinated.fascinatedutils.gui.render.AlumiteGuiRenderer;

import java.util.List;

/**
 * A box that is as large as the largest of a set of candidate nodes, so a slot keeps the same size whichever
 * candidate it currently shows. The candidates are only measured, never placed or drawn; the node's own children are
 * placed inside the box by their constraints, for example a label aligned to the right of a column that has to fit
 * every possible label.
 */
public class WidestNode extends PositionedNode<WidestNode> {
    private final List<? extends UiNode> candidates;

    /**
     * Creates a slot sized by candidates.
     *
     * @param candidates nodes to measure; the slot is as wide as the widest and as tall as the tallest
     */
    public WidestNode(List<? extends UiNode> candidates) {
        this.candidates = candidates;
    }

    @Override
    protected MeasuredSize doMeasure(AlumiteGuiRenderer renderFrame, int maxWidth, int maxHeight) {
        int width = 0;
        int height = 0;
        for (UiNode candidate : candidates) {
            MeasuredSize candidateSize = candidate.measure(renderFrame, maxWidth, maxHeight);
            width = Math.max(width, candidateSize.width());
            height = Math.max(height, candidateSize.height());
        }
        return new MeasuredSize(width, height);
    }
}
