package cc.fascinated.fascinatedutils.gui.node.layout;

import cc.fascinated.fascinatedutils.gui.layout.PositionedNode;
import cc.fascinated.fascinatedutils.gui.core.UiNode;
import cc.fascinated.fascinatedutils.gui.layout.MeasuredSize;
import cc.fascinated.fascinatedutils.gui.render.AlumiteGuiRenderer;

/**
 * Layers its children in one box. The first child decides the size of the box; every later child is drawn over it and
 * positioned inside the box by its own constraints, without adding to the size. This lets a child such as a bar hug
 * the bottom edge of a block with {@code fullWidth} and {@code alignY}, without needing to know the block's size.
 */
public class OverlayNode extends PositionedNode<OverlayNode> {

    @Override
    protected MeasuredSize doMeasure(AlumiteGuiRenderer renderFrame, int maxWidth, int maxHeight) {
        for (UiNode childNode : childrenView()) {
            if (childNode.visible()) {
                return childNode.measure(renderFrame, maxWidth, maxHeight);
            }
        }
        return new MeasuredSize(0, 0);
    }
}
