package cc.fascinated.fascinatedutils.oldgui.widgets;

import cc.fascinated.fascinatedutils.oldgui.renderer.UIRenderer;

import lombok.Getter;
import lombok.experimental.Accessors;

@Accessors(fluent = true)
public class FMinWidthHostWidget extends FWidget {
    @Getter
    private final float minimumWidth;
    @Getter
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
