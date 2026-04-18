package cc.fascinated.fascinatedutils.gui.widgets;

import cc.fascinated.fascinatedutils.gui.renderer.UIRenderer;

public class FMinWidthHostWidget extends FWidget {
    private final float minimumWidth;
    private final FWidget innerChild;

    public FMinWidthHostWidget(float minimumWidth, FWidget innerChild) {
        this.minimumWidth = Math.max(0f, minimumWidth);
        this.innerChild = innerChild;
        addChild(innerChild);
    }

    @Override
    public float intrinsicWidthForRow(UIRenderer measure, float heightBudget) {
        return Math.max(minimumWidth, innerChild.intrinsicWidthForRow(measure, heightBudget));
    }

    @Override
    public float intrinsicHeightForColumn(UIRenderer measure, float widthBudget) {
        return innerChild.intrinsicHeightForColumn(measure, widthBudget);
    }

    @Override
    public boolean fillsHorizontalInRow() {
        return innerChild.fillsHorizontalInRow();
    }

    @Override
    public boolean fillsVerticalInColumn() {
        return innerChild.fillsVerticalInColumn();
    }

    @Override
    public void layout(UIRenderer measure, float layoutX, float layoutY, float layoutWidth, float layoutHeight) {
        setBounds(layoutX, layoutY, layoutWidth, layoutHeight);
        innerChild.layout(measure, layoutX, layoutY, layoutWidth, layoutHeight);
    }
}
