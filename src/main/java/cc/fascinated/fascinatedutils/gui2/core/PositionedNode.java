package cc.fascinated.fascinatedutils.gui2.core;

import cc.fascinated.fascinatedutils.gui2.layout.AxisConstraints;
import cc.fascinated.fascinatedutils.gui2.layout.BoxLayoutSpec;
import cc.fascinated.fascinatedutils.gui2.render.RenderFrame;

public class PositionedNode extends UiNode {
    private final BoxLayoutSpec boxLayout = new BoxLayoutSpec();
    private int columnGap = -1;
    private int rowGap = -1;

    protected BoxLayoutSpec boxLayout() {
        return boxLayout;
    }

    public PositionedNode width(int width) {
        boxLayout.horizontal().sizePx(width);
        return this;
    }

    public PositionedNode widthRel(float relativeWidth) {
        boxLayout.horizontal().sizeRel(relativeWidth);
        return this;
    }

    public PositionedNode height(int height) {
        boxLayout.vertical().sizePx(height);
        return this;
    }

    public PositionedNode heightRel(float relativeHeight) {
        boxLayout.vertical().sizeRel(relativeHeight);
        return this;
    }

    public PositionedNode size(int width, int height) {
        return width(width).height(height);
    }

    public PositionedNode size(int size) {
        return size(size, size);
    }

    public PositionedNode sizeRel(float relativeWidth, float relativeHeight) {
        return widthRel(relativeWidth).heightRel(relativeHeight);
    }

    public PositionedNode sizeRel(float relativeSize) {
        return sizeRel(relativeSize, relativeSize);
    }

    public PositionedNode fullWidth() {
        boxLayout.horizontal().startPx(0).endPx(0);
        return this;
    }

    public PositionedNode fullHeight() {
        boxLayout.vertical().startPx(0).endPx(0);
        return this;
    }

    public PositionedNode full() {
        return fullWidth().fullHeight();
    }

    public PositionedNode left(int leftPixels) {
        boxLayout.horizontal().startPx(leftPixels);
        return this;
    }

    public PositionedNode leftRel(float relativeLeft) {
        boxLayout.horizontal().startRel(relativeLeft);
        return this;
    }

    public PositionedNode leftRel(float relativeLeft, int offsetPixels, float anchor) {
        boxLayout.horizontal().startRel(relativeLeft, offsetPixels, anchor);
        return this;
    }

    public PositionedNode right(int rightPixels) {
        boxLayout.horizontal().endPx(rightPixels);
        return this;
    }

    public PositionedNode rightRel(float relativeRight) {
        boxLayout.horizontal().endRel(relativeRight);
        return this;
    }

    public PositionedNode rightRel(float relativeRight, int offsetPixels, float anchor) {
        boxLayout.horizontal().endRel(relativeRight, offsetPixels, anchor);
        return this;
    }

    public PositionedNode top(int topPixels) {
        boxLayout.vertical().startPx(topPixels);
        return this;
    }

    public PositionedNode topRel(float relativeTop) {
        boxLayout.vertical().startRel(relativeTop);
        return this;
    }

    public PositionedNode topRel(float relativeTop, int offsetPixels, float anchor) {
        boxLayout.vertical().startRel(relativeTop, offsetPixels, anchor);
        return this;
    }

    public PositionedNode bottom(int bottomPixels) {
        boxLayout.vertical().endPx(bottomPixels);
        return this;
    }

    public PositionedNode bottomRel(float relativeBottom) {
        boxLayout.vertical().endRel(relativeBottom);
        return this;
    }

    public PositionedNode bottomRel(float relativeBottom, int offsetPixels, float anchor) {
        boxLayout.vertical().endRel(relativeBottom, offsetPixels, anchor);
        return this;
    }

    public PositionedNode pos(int positionX, int positionY) {
        return left(positionX).top(positionY);
    }

    public PositionedNode posRel(float relativeX, float relativeY) {
        return leftRel(relativeX).topRel(relativeY);
    }

    public PositionedNode alignX(float alignment) {
        boxLayout.horizontal().startRel(alignment, 0, alignment);
        return this;
    }

    public PositionedNode alignY(float alignment) {
        boxLayout.vertical().startRel(alignment, 0, alignment);
        return this;
    }

    public PositionedNode center() {
        return alignX(0.5f).alignY(0.5f);
    }

    public PositionedNode columnGap(int gap) {
        this.columnGap = gap;
        return this;
    }

    public PositionedNode rowGap(int gap) {
        this.rowGap = gap;
        return this;
    }

    protected int intrinsicWidth(RenderFrame renderFrame, int parentWidth) {
        return 0;
    }

    protected int intrinsicHeight(RenderFrame renderFrame, int parentHeight, int resolvedWidth) {
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
