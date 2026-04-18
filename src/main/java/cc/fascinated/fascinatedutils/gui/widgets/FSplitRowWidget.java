package cc.fascinated.fascinatedutils.gui.widgets;

import cc.fascinated.fascinatedutils.gui.renderer.UIRenderer;

public class FSplitRowWidget extends FWidget {
    private final float leftWidth;

    public FSplitRowWidget(float leftWidth) {
        this.leftWidth = leftWidth;
    }

    @Override
    public float intrinsicHeightForColumn(UIRenderer measure, float widthBudget) {
        if (childrenView().size() != 2) {
            return 0f;
        }
        FWidget left = childrenView().get(0);
        FWidget right = childrenView().get(1);
        float leftColumnWidth = Math.min(leftWidth, widthBudget);
        float rightColumnWidth = Math.max(0f, widthBudget - leftColumnWidth);
        float leftHeight = left.intrinsicHeightForColumn(measure, leftColumnWidth);
        float rightHeight = right.intrinsicHeightForColumn(measure, rightColumnWidth);
        return Math.max(leftHeight, rightHeight);
    }

    @Override
    public float intrinsicWidthForRow(UIRenderer measure, float heightBudget) {
        if (childrenView().size() != 2) {
            return 0f;
        }
        FWidget right = childrenView().get(1);
        float rightIntrinsic = right.intrinsicWidthForRow(measure, heightBudget);
        return leftWidth + Math.max(0f, rightIntrinsic);
    }

    @Override
    public boolean fillsVerticalInColumn() {
        return childrenView().size() == 2;
    }

    @Override
    public void layout(UIRenderer measure, float layoutX, float layoutY, float layoutWidth, float layoutHeight) {
        setBounds(layoutX, layoutY, layoutWidth, layoutHeight);
        if (childrenView().size() != 2) {
            return;
        }
        FWidget left = childrenView().get(0);
        FWidget right = childrenView().get(1);
        float leftW = Math.min(leftWidth, layoutWidth);
        float rightW = Math.max(0f, layoutWidth - leftW);
        left.layout(measure, layoutX, layoutY, leftW, layoutHeight);
        right.layout(measure, layoutX + leftW, layoutY, rightW, layoutHeight);
    }
}
