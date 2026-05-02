package cc.fascinated.fascinatedutils.gui.widgets;

import cc.fascinated.fascinatedutils.gui.renderer.UIRenderer;

/**
 * Centers a child within the host bounds and caps its size with {@code maxInnerWidth}/{@code maxInnerHeight}
 * after subtracting symmetric insets.
 */
public class FMaxCenterInsetsWidget extends FWidget {
    private final float insetHorizontal;
    private final float insetVertical;
    private final float maxInnerWidth;
    private final float maxInnerHeight;
    private final FWidget inner;

    public FMaxCenterInsetsWidget(float insetHorizontal, float insetVertical, float maxInnerWidth, float maxInnerHeight, FWidget inner) {
        this.insetHorizontal = Math.max(0f, insetHorizontal);
        this.insetVertical = Math.max(0f, insetVertical);
        this.maxInnerWidth = Math.max(0f, maxInnerWidth);
        this.maxInnerHeight = Math.max(0f, maxInnerHeight);
        this.inner = inner;
        addChild(inner);
    }

    public boolean matchesSpec(float insetHorizontalValue, float insetVerticalValue, float maxInnerWidthValue, float maxInnerHeightValue) {
        return Math.abs(insetHorizontalValue - insetHorizontal) < 1e-3f
                && Math.abs(insetVerticalValue - insetVertical) < 1e-3f
                && Math.abs(maxInnerWidthValue - maxInnerWidth) < 1e-3f
                && Math.abs(maxInnerHeightValue - maxInnerHeight) < 1e-3f;
    }

    public FWidget inner() {
        return inner;
    }

    @Override
    public boolean fillsHorizontalInRow() {
        return true;
    }

    @Override
    public boolean fillsVerticalInColumn() {
        return true;
    }

    @Override
    public void layout(UIRenderer measure, float layoutX, float layoutY, float layoutWidth, float layoutHeight) {
        setBounds(layoutX, layoutY, layoutWidth, layoutHeight);
        float availableWidth = Math.max(0f, layoutWidth - 2f * insetHorizontal);
        float availableHeight = Math.max(0f, layoutHeight - 2f * insetVertical);
        float childWidth = Math.min(availableWidth, maxInnerWidth);
        float childHeight = Math.min(availableHeight, maxInnerHeight);
        float childX = layoutX + insetHorizontal + (availableWidth - childWidth) * 0.5f;
        float childY = layoutY + insetVertical + (availableHeight - childHeight) * 0.5f;
        inner.layout(measure, childX, childY, childWidth, childHeight);
    }
}
