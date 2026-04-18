package cc.fascinated.fascinatedutils.gui.widgets;

import cc.fascinated.fascinatedutils.gui.renderer.UIRenderer;

import java.util.List;

public class FSplitRowWithDividerWidget extends FWidget {
    private final float leftColumnWidthLogical;
    private final float dividerWidthLogical;

    public FSplitRowWithDividerWidget(float leftColumnWidthLogical, float dividerWidthLogical) {
        this.leftColumnWidthLogical = leftColumnWidthLogical;
        this.dividerWidthLogical = dividerWidthLogical;
    }

    @Override
    public boolean fillsVerticalInColumn() {
        return childrenView().size() == 3;
    }

    @Override
    public float intrinsicHeightForColumn(UIRenderer measure, float widthBudget) {
        List<FWidget> kids = childrenView();
        if (kids.size() != 3) {
            return 0f;
        }
        float leftW = Math.min(leftColumnWidthLogical, widthBudget);
        float dividerW = Math.min(dividerWidthLogical, Math.max(0f, widthBudget - leftW));
        float rightW = Math.max(0f, widthBudget - leftW - dividerW);
        float leftHeight = kids.get(0).intrinsicHeightForColumn(measure, leftW);
        float dividerHeight = kids.get(1).intrinsicHeightForColumn(measure, dividerW);
        float rightHeight = kids.get(2).intrinsicHeightForColumn(measure, rightW);
        return Math.max(leftHeight, Math.max(dividerHeight, rightHeight));
    }

    @Override
    public float intrinsicWidthForRow(UIRenderer measure, float heightBudget) {
        List<FWidget> kids = childrenView();
        if (kids.size() != 3) {
            return 0f;
        }
        FWidget right = kids.get(2);
        float rightIntrinsic = right.intrinsicWidthForRow(measure, heightBudget);
        return leftColumnWidthLogical + dividerWidthLogical + Math.max(0f, rightIntrinsic);
    }

    @Override
    public void layout(UIRenderer measure, float layoutX, float layoutY, float layoutWidth, float layoutHeight) {
        setBounds(layoutX, layoutY, layoutWidth, layoutHeight);
        List<FWidget> kids = childrenView();
        if (kids.size() != 3) {
            return;
        }
        FWidget left = kids.get(0);
        FWidget divider = kids.get(1);
        FWidget right = kids.get(2);
        float leftW = Math.min(leftColumnWidthLogical, layoutWidth);
        float dividerW = Math.min(dividerWidthLogical, Math.max(0f, layoutWidth - leftW));
        float rightW = Math.max(0f, layoutWidth - leftW - dividerW);
        left.layout(measure, layoutX, layoutY, leftW, layoutHeight);
        divider.layout(measure, layoutX + leftW, layoutY, dividerW, layoutHeight);
        right.layout(measure, layoutX + leftW + dividerW, layoutY, rightW, layoutHeight);
    }
}
