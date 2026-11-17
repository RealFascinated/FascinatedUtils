package cc.fascinated.fascinatedutils.gui.layout;

import cc.fascinated.fascinatedutils.gui.core.UiNode;

import cc.fascinated.fascinatedutils.gui.layout.Axis;
import cc.fascinated.fascinatedutils.gui.layout.AxisConstraints;
import cc.fascinated.fascinatedutils.gui.layout.BoxLayoutSpec;
import cc.fascinated.fascinatedutils.gui.layout.MeasuredSize;
import cc.fascinated.fascinatedutils.gui.render.AlumiteGuiRenderer;

import java.util.function.Supplier;

/**
 * A node with a fluent box: size, edge offsets, alignment and an optional row or column flow over its children.
 *
 * <p>Layout is two passes. {@link #measure} yields the desired size from the box constraints, the content size from
 * {@link #intrinsicWidth} and {@link #intrinsicHeight} for leaves, or the stacked children for a row or column. Then
 * {@code arrange} places the node and its children once, top-down. Percentages ({@code widthRel}, {@code leftRel}, ...)
 * resolve against the size available to the node: the parent's resolved size, or the parent's own available size when
 * the parent is auto-sized. A node that is not auto-sized never sees its children's size.
 */
public class PositionedNode<T extends PositionedNode<T>> extends UiNode {
    private BoxLayoutSpec boxLayout;
    private int columnGap = -1;
    private int rowGap = -1;
    private boolean intrinsicWidthUsed;
    private boolean intrinsicHeightUsed;
    private int trackedIntrinsicWidth;
    private int trackedIntrinsicHeight;
    private int trackedWidthAvailable;
    private int trackedHeightAvailable;
    private int trackedHeightWidth;

    /**
     * The mutable box constraints of this node, created on first use.
     *
     * @return the node's constraints
     */
    protected BoxLayoutSpec boxLayout() {
        if (boxLayout == null) {
            boxLayout = new BoxLayoutSpec();
        }
        return boxLayout;
    }

    private AxisConstraints horizontalConstraints() {
        return boxLayout == null ? AxisConstraints.NONE : boxLayout.horizontal();
    }

    private AxisConstraints verticalConstraints() {
        return boxLayout == null ? AxisConstraints.NONE : boxLayout.vertical();
    }

    private AxisConstraints constraints(Axis axis) {
        return axis == Axis.HORIZONTAL ? horizontalConstraints() : verticalConstraints();
    }

    @SuppressWarnings("unchecked")
    protected T self() {
        return (T) this;
    }

    public T width(int width) {
        boxLayout().horizontal().sizePx(width);
        markLayoutDirty();
        return self();
    }

    public T widthRel(float relativeWidth) {
        boxLayout().horizontal().sizeRel(relativeWidth);
        markLayoutDirty();
        return self();
    }

    public T height(int height) {
        boxLayout().vertical().sizePx(height);
        markLayoutDirty();
        return self();
    }

    public T heightRel(float relativeHeight) {
        boxLayout().vertical().sizeRel(relativeHeight);
        markLayoutDirty();
        return self();
    }

    public T size(int width, int height) {
        return width(width).height(height);
    }

    public T size(int size) {
        return size(size, size);
    }

    public T sizeRel(float relativeWidth, float relativeHeight) {
        return widthRel(relativeWidth).heightRel(relativeHeight);
    }

    public T sizeRel(float relativeSize) {
        return sizeRel(relativeSize, relativeSize);
    }

    public T fullWidth() {
        boxLayout().horizontal().startPx(0).endPx(0);
        markLayoutDirty();
        return self();
    }

    public T fullHeight() {
        boxLayout().vertical().startPx(0).endPx(0);
        markLayoutDirty();
        return self();
    }

    public T full() {
        return fullWidth().fullHeight();
    }

    public T left(int leftPixels) {
        boxLayout().horizontal().startPx(leftPixels);
        markLayoutDirty();
        return self();
    }

    public T leftRel(float relativeLeft) {
        boxLayout().horizontal().startRel(relativeLeft);
        markLayoutDirty();
        return self();
    }

    public T leftRel(float relativeLeft, int offsetPixels, float anchor) {
        boxLayout().horizontal().startRel(relativeLeft, offsetPixels, anchor);
        markLayoutDirty();
        return self();
    }

    public T right(int rightPixels) {
        boxLayout().horizontal().endPx(rightPixels);
        markLayoutDirty();
        return self();
    }

    public T rightRel(float relativeRight) {
        boxLayout().horizontal().endRel(relativeRight);
        markLayoutDirty();
        return self();
    }

    public T rightRel(float relativeRight, int offsetPixels, float anchor) {
        boxLayout().horizontal().endRel(relativeRight, offsetPixels, anchor);
        markLayoutDirty();
        return self();
    }

    public T top(int topPixels) {
        boxLayout().vertical().startPx(topPixels);
        markLayoutDirty();
        return self();
    }

    public T topRel(float relativeTop) {
        boxLayout().vertical().startRel(relativeTop);
        markLayoutDirty();
        return self();
    }

    public T topRel(float relativeTop, int offsetPixels, float anchor) {
        boxLayout().vertical().startRel(relativeTop, offsetPixels, anchor);
        markLayoutDirty();
        return self();
    }

    public T bottom(int bottomPixels) {
        boxLayout().vertical().endPx(bottomPixels);
        markLayoutDirty();
        return self();
    }

    public T bottomRel(float relativeBottom) {
        boxLayout().vertical().endRel(relativeBottom);
        markLayoutDirty();
        return self();
    }

    public T bottomRel(float relativeBottom, int offsetPixels, float anchor) {
        boxLayout().vertical().endRel(relativeBottom, offsetPixels, anchor);
        markLayoutDirty();
        return self();
    }

    public T margin(int pixels) {
        return left(pixels).right(pixels).top(pixels).bottom(pixels);
    }

    public T pos(int positionX, int positionY) {
        return left(positionX).top(positionY);
    }

    /**
     * Anchors this node horizontally: places the point {@code alignment} of the way across the node at that point of
     * the available width, so 0 is left, 0.5 centred and 1 right.
     *
     * @param alignment 0 for left, 1 for right
     * @return this node
     */
    public T alignX(float alignment) {
        boxLayout().horizontal().startRel(alignment, 0, alignment);
        markLayoutDirty();
        return self();
    }

    public T alignY(float alignment) {
        boxLayout().vertical().startRel(alignment, 0, alignment);
        markLayoutDirty();
        return self();
    }

    public T center() {
        return alignX(0.5f).alignY(0.5f);
    }

    /**
     * Turns this node into a vertical stack, a column, with the given space between children. The name follows the
     * gap between rows of one column. Children are laid top to bottom; their top and bottom edge constraints act as
     * margins, and their left, right and alignment constraints place them across the column. Invisible children take
     * no space and a {@link SpacerNode} takes the leftover height. Without a height the column is as tall as its
     * children, and without a width as wide as its widest child.
     *
     * @param gap the space between consecutive children in pixels
     * @return this node
     */
    public T columnGap(int gap) {
        this.columnGap = gap;
        markLayoutDirty();
        return self();
    }

    /**
     * Turns this node into a horizontal stack, a row, with the given space between children. The name follows the
     * gap between columns of one row. Children are laid left to right; their left and right edge constraints act as
     * margins, and their top, bottom and alignment constraints place them across the row. Invisible children take no
     * space and a {@link SpacerNode} takes the leftover width. Without a width the row is as wide as its children, and
     * without a height as tall as its tallest child. When both gaps are set the column wins.
     *
     * @param gap the space between consecutive children in pixels
     * @return this node
     */
    public T rowGap(int gap) {
        this.rowGap = gap;
        markLayoutDirty();
        return self();
    }

    @Override
    public T setNodeId(String nodeId) {
        super.setNodeId(nodeId);
        return self();
    }

    @Override
    public T setTooltip(Supplier<String> tooltipSupplier) {
        super.setTooltip(tooltipSupplier);
        return self();
    }

    @Override
    public T setTooltip(String tooltipText) {
        super.setTooltip(tooltipText);
        return self();
    }

    @Override
    public T clearTooltip() {
        super.clearTooltip();
        return self();
    }

    @Override
    public T setOnPointerEnter(Runnable callback) {
        super.setOnPointerEnter(callback);
        return self();
    }

    @Override
    public T setOnPointerLeave(Runnable callback) {
        super.setOnPointerLeave(callback);
        return self();
    }

    @Override
    public T addChild(UiNode childNode) {
        super.addChild(childNode);
        markLayoutDirty();
        return self();
    }

    protected int intrinsicWidth(AlumiteGuiRenderer renderFrame, int parentWidth) {
        return 0;
    }

    protected int intrinsicHeight(AlumiteGuiRenderer renderFrame, int parentHeight, int resolvedWidth) {
        return 0;
    }

    @Override
    public void layout(AlumiteGuiRenderer renderFrame, int slotX, int slotY, int slotWidth, int slotHeight) {
        if (!visible()) {
            return;
        }
        MeasuredSize size = measure(renderFrame, slotWidth, slotHeight);
        long horizontalBox = horizontalConstraints().resolveBox(slotX, slotWidth, size.width());
        long verticalBox = verticalConstraints().resolveBox(slotY, slotHeight, size.height());
        arrange(renderFrame, AxisConstraints.boxStart(horizontalBox), AxisConstraints.boxStart(verticalBox), AxisConstraints.boxSize(horizontalBox), AxisConstraints.boxSize(verticalBox));
    }

    @Override
    protected MeasuredSize doMeasure(AlumiteGuiRenderer renderFrame, int maxWidth, int maxHeight) {
        intrinsicWidthUsed = false;
        intrinsicHeightUsed = false;
        if (columnGap >= 0) {
            return measureStack(renderFrame, maxWidth, maxHeight, Axis.VERTICAL, columnGap);
        }
        if (rowGap >= 0) {
            return measureStack(renderFrame, maxWidth, maxHeight, Axis.HORIZONTAL, rowGap);
        }
        int width = horizontalConstraints().hasDefinedSize() ? horizontalConstraints().resolveSize(maxWidth, 0) : trackedIntrinsicWidth(renderFrame, maxWidth);
        int height = verticalConstraints().hasDefinedSize() ? verticalConstraints().resolveSize(maxHeight, 0) : trackedIntrinsicHeight(renderFrame, maxHeight, width);
        return new MeasuredSize(width, height);
    }

    private int trackedIntrinsicWidth(AlumiteGuiRenderer renderFrame, int maxWidth) {
        intrinsicWidthUsed = true;
        trackedWidthAvailable = maxWidth;
        trackedIntrinsicWidth = intrinsicWidth(renderFrame, maxWidth);
        return trackedIntrinsicWidth;
    }

    private int trackedIntrinsicHeight(AlumiteGuiRenderer renderFrame, int maxHeight, int resolvedWidth) {
        intrinsicHeightUsed = true;
        trackedHeightAvailable = maxHeight;
        trackedHeightWidth = resolvedWidth;
        trackedIntrinsicHeight = intrinsicHeight(renderFrame, maxHeight, resolvedWidth);
        return trackedIntrinsicHeight;
    }

    @Override
    protected boolean layoutInputsChanged(AlumiteGuiRenderer renderFrame) {
        if (super.layoutInputsChanged(renderFrame)) {
            return true;
        }
        if (intrinsicWidthUsed && intrinsicWidth(renderFrame, trackedWidthAvailable) != trackedIntrinsicWidth) {
            return true;
        }
        return intrinsicHeightUsed && intrinsicHeight(renderFrame, trackedHeightAvailable, trackedHeightWidth) != trackedIntrinsicHeight;
    }

    @Override
    public int leadingMargin(Axis axis, int availableSize) {
        return constraints(axis).leadingMargin(availableSize);
    }

    @Override
    public int trailingMargin(Axis axis, int availableSize) {
        return constraints(axis).trailingMargin(availableSize);
    }

    @Override
    public float exactSize(Axis axis, int availableSize, int measuredSize) {
        AxisConstraints axisConstraints = constraints(axis);
        return axisConstraints.hasDefinedSize() ? axisConstraints.resolveExactSize(availableSize, measuredSize) : measuredSize;
    }

    @Override
    public long resolveBox(Axis axis, int slotStart, int slotSize, int measuredSize) {
        return constraints(axis).resolveBox(slotStart, slotSize, measuredSize);
    }

    @Override
    protected void arrangeChildren(AlumiteGuiRenderer renderFrame, int positionX, int positionY, int width, int height) {
        if (columnGap >= 0) {
            arrangeStack(renderFrame, positionX, positionY, width, height, Axis.VERTICAL, columnGap);
        }
        else if (rowGap >= 0) {
            arrangeStack(renderFrame, positionX, positionY, width, height, Axis.HORIZONTAL, rowGap);
        }
        else {
            super.arrangeChildren(renderFrame, positionX, positionY, width, height);
        }
    }

    /**
     * Sizes a stack from its children. Children are measured against the stack's own size when it has one, otherwise
     * against what is available to the stack, which is also what arrange uses, so each child is measured once.
     */
    private MeasuredSize measureStack(AlumiteGuiRenderer renderFrame, int maxWidth, int maxHeight, Axis mainAxis, int gap) {
        Axis crossAxis = mainAxis == Axis.HORIZONTAL ? Axis.VERTICAL : Axis.HORIZONTAL;
        int maxMain = mainAxis == Axis.HORIZONTAL ? maxWidth : maxHeight;
        int maxCross = mainAxis == Axis.HORIZONTAL ? maxHeight : maxWidth;
        AxisConstraints mainConstraints = constraints(mainAxis);
        AxisConstraints crossConstraints = constraints(crossAxis);
        int availableMain = mainConstraints.hasDefinedSize() ? mainConstraints.resolveSize(maxMain, 0) : maxMain;
        int availableCross = crossConstraints.hasDefinedSize() ? crossConstraints.resolveSize(maxCross, 0) : maxCross;

        int visibleCount = 0;
        int mainTotal = 0;
        int crossMax = 0;
        for (UiNode childNode : childrenView()) {
            if (!childNode.visible()) {
                continue;
            }
            visibleCount++;
            if (childNode instanceof SpacerNode) {
                continue;
            }
            MeasuredSize childSize = measureChild(renderFrame, childNode, mainAxis, availableMain, availableCross);
            int childMain = mainAxis == Axis.HORIZONTAL ? childSize.width() : childSize.height();
            int childCross = mainAxis == Axis.HORIZONTAL ? childSize.height() : childSize.width();
            mainTotal += childNode.leadingMargin(mainAxis, availableMain) + childMain + childNode.trailingMargin(mainAxis, availableMain);
            crossMax = Math.max(crossMax, childNode.leadingMargin(crossAxis, availableCross) + childCross + childNode.trailingMargin(crossAxis, availableCross));
        }
        mainTotal += gap * Math.max(0, visibleCount - 1);

        int main;
        if (mainConstraints.hasDefinedSize()) {
            main = availableMain;
        }
        else if (visibleCount > 0) {
            main = mainTotal;
        }
        else {
            main = mainAxis == Axis.HORIZONTAL ? trackedIntrinsicWidth(renderFrame, maxWidth) : trackedIntrinsicHeight(renderFrame, maxHeight, 0);
        }
        int cross;
        if (crossConstraints.hasDefinedSize()) {
            cross = availableCross;
        }
        else if (visibleCount > 0) {
            cross = crossMax;
        }
        else {
            cross = mainAxis == Axis.HORIZONTAL ? trackedIntrinsicHeight(renderFrame, maxHeight, 0) : trackedIntrinsicWidth(renderFrame, maxWidth);
        }
        return mainAxis == Axis.HORIZONTAL ? new MeasuredSize(main, cross) : new MeasuredSize(cross, main);
    }

    private MeasuredSize measureChild(AlumiteGuiRenderer renderFrame, UiNode childNode, Axis mainAxis, int availableMain, int availableCross) {
        return mainAxis == Axis.HORIZONTAL
                ? childNode.measure(renderFrame, availableMain, availableCross)
                : childNode.measure(renderFrame, availableCross, availableMain);
    }

    private void arrangeStack(AlumiteGuiRenderer renderFrame, int positionX, int positionY, int width, int height, Axis mainAxis, int gap) {
        Axis crossAxis = mainAxis == Axis.HORIZONTAL ? Axis.VERTICAL : Axis.HORIZONTAL;
        boolean horizontal = mainAxis == Axis.HORIZONTAL;
        int mainStart = horizontal ? positionX : positionY;
        int crossStart = horizontal ? positionY : positionX;
        int mainSize = horizontal ? width : height;
        int crossSize = horizontal ? height : width;
        int availableMain = constraints(mainAxis).hasDefinedSize() ? mainSize : (horizontal ? measuredMaxWidth() : measuredMaxHeight());
        int availableCross = constraints(crossAxis).hasDefinedSize() ? crossSize : (horizontal ? measuredMaxHeight() : measuredMaxWidth());

        int visibleCount = 0;
        int spacerCount = 0;
        float fixedTotal = 0f;
        for (UiNode childNode : childrenView()) {
            if (!childNode.visible()) {
                continue;
            }
            visibleCount++;
            if (childNode instanceof SpacerNode) {
                spacerCount++;
                continue;
            }
            MeasuredSize childSize = measureChild(renderFrame, childNode, mainAxis, availableMain, availableCross);
            int childMain = horizontal ? childSize.width() : childSize.height();
            fixedTotal += childNode.leadingMargin(mainAxis, availableMain) + childNode.exactSize(mainAxis, availableMain, childMain) + childNode.trailingMargin(mainAxis, availableMain);
        }
        float spacerSize = spacerCount > 0 ? Math.max(0f, (mainSize - fixedTotal - gap * Math.max(0, visibleCount - 1)) / spacerCount) : 0f;

        float cursor = mainStart;
        for (UiNode childNode : childrenView()) {
            if (!childNode.visible()) {
                continue;
            }
            float childMainExact;
            long crossBox;
            if (childNode instanceof SpacerNode) {
                childMainExact = spacerSize;
                crossBox = AxisConstraints.packBox(crossStart, crossSize);
            }
            else {
                MeasuredSize childSize = measureChild(renderFrame, childNode, mainAxis, availableMain, availableCross);
                int childMain = horizontal ? childSize.width() : childSize.height();
                int childCross = horizontal ? childSize.height() : childSize.width();
                cursor += childNode.leadingMargin(mainAxis, availableMain);
                childMainExact = childNode.exactSize(mainAxis, availableMain, childMain);
                crossBox = childNode.resolveBox(crossAxis, crossStart, crossSize, childCross);
            }
            int childMainStart = Math.round(cursor);
            int childMainEnd = Math.round(cursor + childMainExact);
            int childMainSize = Math.max(0, childMainEnd - childMainStart);
            if (horizontal) {
                childNode.arrange(renderFrame, childMainStart, AxisConstraints.boxStart(crossBox), childMainSize, AxisConstraints.boxSize(crossBox));
            }
            else {
                childNode.arrange(renderFrame, AxisConstraints.boxStart(crossBox), childMainStart, AxisConstraints.boxSize(crossBox), childMainSize);
            }
            cursor += childMainExact + gap;
            if (!(childNode instanceof SpacerNode)) {
                cursor += childNode.trailingMargin(mainAxis, availableMain);
            }
        }
    }
}
