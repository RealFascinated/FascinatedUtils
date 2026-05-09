package cc.fascinated.fascinatedutils.gui2.core;

import cc.fascinated.fascinatedutils.gui2.layout.BoxLayoutSpec;
import cc.fascinated.fascinatedutils.gui2.render.RenderFrame;

/**
 * Node with fluent size and position builders inspired by the old UI ergonomics.
 */
public class PositionedNode extends UiNode {
    private final BoxLayoutSpec boxLayout = new BoxLayoutSpec();

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

    @Override
    public void layout(RenderFrame renderFrame, int parentX, int parentY, int parentWidth, int parentHeight) {
        int resolvedWidth = boxLayout.horizontal().resolveSize(parentWidth, "horizontal", debugPath());
        int resolvedHeight = boxLayout.vertical().resolveSize(parentHeight, "vertical", debugPath());
        int resolvedX = boxLayout.horizontal().resolvePosition(parentX, parentWidth, resolvedWidth);
        int resolvedY = boxLayout.vertical().resolvePosition(parentY, parentHeight, resolvedHeight);
        bounds().set(resolvedX, resolvedY, resolvedWidth, resolvedHeight);

        for (UiNode childNode : childrenView()) {
            childNode.layout(renderFrame, resolvedX, resolvedY, resolvedWidth, resolvedHeight);
        }
    }
}
