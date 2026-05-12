package cc.fascinated.fascinatedutils.gui2.core;

import cc.fascinated.fascinatedutils.gui2.layout.AxisConstraints;
import cc.fascinated.fascinatedutils.gui2.layout.BoxLayoutSpec;
import cc.fascinated.fascinatedutils.gui2.render.RenderFrame;

public class PositionedNode<T extends PositionedNode<T>> extends UiNode {
    private final BoxLayoutSpec boxLayout = new BoxLayoutSpec();
    private int columnGap = -1;
    private int rowGap = -1;

    protected BoxLayoutSpec boxLayout() {
        return boxLayout;
    }

    @SuppressWarnings("unchecked")
    protected T self() {
        return (T) this;
    }

    public T width(int width) {
        boxLayout.horizontal().sizePx(width);
        return self();
    }

    public T widthRel(float relativeWidth) {
        boxLayout.horizontal().sizeRel(relativeWidth);
        return self();
    }

    public T height(int height) {
        boxLayout.vertical().sizePx(height);
        return self();
    }

    public T heightRel(float relativeHeight) {
        boxLayout.vertical().sizeRel(relativeHeight);
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
        boxLayout.horizontal().startPx(0).endPx(0);
        return self();
    }

    public T fullHeight() {
        boxLayout.vertical().startPx(0).endPx(0);
        return self();
    }

    public T full() {
        return fullWidth().fullHeight();
    }

    public T left(int leftPixels) {
        boxLayout.horizontal().startPx(leftPixels);
        return self();
    }

    public T leftRel(float relativeLeft) {
        boxLayout.horizontal().startRel(relativeLeft);
        return self();
    }

    public T leftRel(float relativeLeft, int offsetPixels, float anchor) {
        boxLayout.horizontal().startRel(relativeLeft, offsetPixels, anchor);
        return self();
    }

    public T right(int rightPixels) {
        boxLayout.horizontal().endPx(rightPixels);
        return self();
    }

    public T rightRel(float relativeRight) {
        boxLayout.horizontal().endRel(relativeRight);
        return self();
    }

    public T rightRel(float relativeRight, int offsetPixels, float anchor) {
        boxLayout.horizontal().endRel(relativeRight, offsetPixels, anchor);
        return self();
    }

    public T top(int topPixels) {
        boxLayout.vertical().startPx(topPixels);
        return self();
    }

    public T topRel(float relativeTop) {
        boxLayout.vertical().startRel(relativeTop);
        return self();
    }

    public T topRel(float relativeTop, int offsetPixels, float anchor) {
        boxLayout.vertical().startRel(relativeTop, offsetPixels, anchor);
        return self();
    }

    public T bottom(int bottomPixels) {
        boxLayout.vertical().endPx(bottomPixels);
        return self();
    }

    public T bottomRel(float relativeBottom) {
        boxLayout.vertical().endRel(relativeBottom);
        return self();
    }

    public T bottomRel(float relativeBottom, int offsetPixels, float anchor) {
        boxLayout.vertical().endRel(relativeBottom, offsetPixels, anchor);
        return self();
    }

    public T margin(int pixels) {
        return left(pixels).right(pixels).top(pixels).bottom(pixels);
    }

    public T pos(int positionX, int positionY) {
        return left(positionX).top(positionY);
    }

    public T posRel(float relativeX, float relativeY) {
        return leftRel(relativeX).topRel(relativeY);
    }

    public T alignX(float alignment) {
        boxLayout.horizontal().startRel(alignment, 0, alignment);
        return self();
    }

    public T alignY(float alignment) {
        boxLayout.vertical().startRel(alignment, 0, alignment);
        return self();
    }

    public T center() {
        return alignX(0.5f).alignY(0.5f);
    }

    public T columnGap(int gap) {
        this.columnGap = gap;
        return self();
    }

    public T rowGap(int gap) {
        this.rowGap = gap;
        return self();
    }

    @Override
    public T addChild(UiNode childNode) {
        super.addChild(childNode);
        return self();
    }

    protected int intrinsicWidth(RenderFrame renderFrame, int parentWidth) {
        return 0;
    }

    protected int intrinsicHeight(RenderFrame renderFrame, int parentHeight, int resolvedWidth) {
        if (columnGap >= 0) {
            int totalH = 0;
            int visibleCount = 0;
            for (UiNode child : childrenView()) {
                if (!child.visible()) continue;
                visibleCount++;
                if (!(child instanceof SpacerNode)) {
                    child.layout(renderFrame, 0, 0, resolvedWidth, parentHeight);
                    totalH += child.bounds().height();
                }
            }
            return totalH + columnGap * Math.max(0, visibleCount - 1);
        }
        return 0;
    }

    protected final int resolveAxisSize(AxisConstraints axisConstraints, int parentSize, int intrinsicSize, String axisLabel) {
        if (axisConstraints.hasSizeConstraint() || (axisConstraints.hasStartConstraint() && axisConstraints.hasEndConstraint())) {
            return axisConstraints.resolveSize(parentSize, axisLabel, debugPath());
        }
        return intrinsicSize;
    }

    @Override
    public void layout(RenderFrame renderFrame, int parentX, int parentY, int parentWidth, int parentHeight) {
        int resolvedWidth = resolveAxisSize(boxLayout.horizontal(), parentWidth, intrinsicWidth(renderFrame, parentWidth), "horizontal");
        int resolvedHeight = resolveAxisSize(boxLayout.vertical(), parentHeight, intrinsicHeight(renderFrame, parentHeight, resolvedWidth), "vertical");
        int resolvedX = boxLayout.horizontal().resolvePosition(parentX, parentWidth, resolvedWidth);
        int resolvedY = boxLayout.vertical().resolvePosition(parentY, parentHeight, resolvedHeight);
        bounds().set(resolvedX, resolvedY, resolvedWidth, resolvedHeight);

        if (columnGap >= 0) {
            int visibleCount = 0;
            int spacerCount = 0;
            int totalFixed = 0;
            for (UiNode childNode : childrenView()) {
                if (!childNode.visible()) continue;
                visibleCount++;
                if (childNode instanceof SpacerNode) {
                    spacerCount++;
                } else {
                    childNode.layout(renderFrame, resolvedX, resolvedY, resolvedWidth, resolvedHeight);
                    totalFixed += childNode.bounds().height();
                }
            }
            int totalGap = columnGap * Math.max(0, visibleCount - 1);
            int spacerHeight = spacerCount > 0 ? Math.max(0, (resolvedHeight - totalFixed - totalGap) / spacerCount) : 0;
            int cursorY = resolvedY;
            for (UiNode childNode : childrenView()) {
                if (!childNode.visible()) continue;
                if (childNode instanceof SpacerNode) {
                    childNode.layout(renderFrame, resolvedX, cursorY, resolvedWidth, spacerHeight);
                    cursorY += spacerHeight + columnGap;
                } else {
                    childNode.layout(renderFrame, resolvedX, cursorY, resolvedWidth, resolvedHeight);
                    cursorY += childNode.bounds().height() + columnGap;
                }
            }
        } else if (rowGap >= 0) {
            int visibleCount = 0;
            int spacerCount = 0;
            int totalFixed = 0;
            for (UiNode childNode : childrenView()) {
                if (!childNode.visible()) continue;
                visibleCount++;
                if (childNode instanceof SpacerNode) {
                    spacerCount++;
                } else {
                    childNode.layout(renderFrame, resolvedX, resolvedY, resolvedWidth, resolvedHeight);
                    totalFixed += childNode.bounds().width();
                }
            }
            int totalGap = rowGap * Math.max(0, visibleCount - 1);
            int spacerWidth = spacerCount > 0 ? Math.max(0, (resolvedWidth - totalFixed - totalGap) / spacerCount) : 0;
            int cursorX = resolvedX;
            for (UiNode childNode : childrenView()) {
                if (!childNode.visible()) continue;
                if (childNode instanceof SpacerNode) {
                    childNode.layout(renderFrame, cursorX, resolvedY, spacerWidth, resolvedHeight);
                    cursorX += spacerWidth + rowGap;
                } else {
                    childNode.layout(renderFrame, cursorX, resolvedY, resolvedWidth, resolvedHeight);
                    cursorX += childNode.bounds().width() + rowGap;
                }
            }
        } else {
            for (UiNode childNode : childrenView()) {
                childNode.layout(renderFrame, resolvedX, resolvedY, resolvedWidth, resolvedHeight);
            }
        }
    }
}
