package cc.fascinated.fascinatedutils.gui.node.layout;

import cc.fascinated.fascinatedutils.gui.layout.PositionedNode;
import cc.fascinated.fascinatedutils.gui.core.UiNode;
import cc.fascinated.fascinatedutils.gui.layout.Axis;
import cc.fascinated.fascinatedutils.gui.layout.MeasuredSize;
import cc.fascinated.fascinatedutils.gui.render.AlumiteGuiRenderer;

/**
 * Insets its children from its own edges. Unless a size is set it is as large as its largest child plus the padding,
 * and never smaller than its minimum size. Each child is placed inside the padded box by its own constraints.
 */
public class PadNode extends PositionedNode<PadNode> {
    private int paddingTop;
    private int paddingRight;
    private int paddingBottom;
    private int paddingLeft;
    private int minWidth;
    private int minHeight;

    public PadNode padding(int allSides) {
        return padding(allSides, allSides, allSides, allSides);
    }

    public PadNode padding(int vertical, int horizontal) {
        return padding(vertical, horizontal, vertical, horizontal);
    }

    public PadNode padding(int top, int right, int bottom, int left) {
        this.paddingTop = Math.max(0, top);
        this.paddingRight = Math.max(0, right);
        this.paddingBottom = Math.max(0, bottom);
        this.paddingLeft = Math.max(0, left);
        markLayoutDirty();
        return this;
    }

    public PadNode minSize(int minWidth, int minHeight) {
        this.minWidth = Math.max(0, minWidth);
        this.minHeight = Math.max(0, minHeight);
        markLayoutDirty();
        return this;
    }

    @Override
    protected MeasuredSize doMeasure(AlumiteGuiRenderer renderFrame, int maxWidth, int maxHeight) {
        MeasuredSize ownSize = super.doMeasure(renderFrame, maxWidth, maxHeight);
        boolean definedWidth = boxLayout().horizontal().hasDefinedSize();
        boolean definedHeight = boxLayout().vertical().hasDefinedSize();
        int innerMaxWidth = Math.max(0, (definedWidth ? ownSize.width() : maxWidth) - paddingLeft - paddingRight);
        int innerMaxHeight = Math.max(0, (definedHeight ? ownSize.height() : maxHeight) - paddingTop - paddingBottom);
        int contentWidth = 0;
        int contentHeight = 0;
        for (UiNode childNode : childrenView()) {
            if (!childNode.visible()) {
                continue;
            }
            MeasuredSize childSize = childNode.measure(renderFrame, innerMaxWidth, innerMaxHeight);
            int childFootprintWidth = childNode.leadingMargin(Axis.HORIZONTAL, innerMaxWidth) + childSize.width() + childNode.trailingMargin(Axis.HORIZONTAL, innerMaxWidth);
            int childFootprintHeight = childNode.leadingMargin(Axis.VERTICAL, innerMaxHeight) + childSize.height() + childNode.trailingMargin(Axis.VERTICAL, innerMaxHeight);
            contentWidth = Math.max(contentWidth, childFootprintWidth);
            contentHeight = Math.max(contentHeight, childFootprintHeight);
        }
        int width = definedWidth ? ownSize.width() : Math.max(minWidth, contentWidth + paddingLeft + paddingRight);
        int height = definedHeight ? ownSize.height() : Math.max(minHeight, contentHeight + paddingTop + paddingBottom);
        return new MeasuredSize(width, height);
    }

    @Override
    protected void arrangeChildren(AlumiteGuiRenderer renderFrame, int positionX, int positionY, int width, int height) {
        int innerWidth = Math.max(0, width - paddingLeft - paddingRight);
        int innerHeight = Math.max(0, height - paddingTop - paddingBottom);
        for (UiNode childNode : childrenView()) {
            if (childNode.visible()) {
                childNode.layout(renderFrame, positionX + paddingLeft, positionY + paddingTop, innerWidth, innerHeight);
            }
        }
    }
}
