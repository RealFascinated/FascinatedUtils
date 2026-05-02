package cc.fascinated.fascinatedutils.gui.widgets;

import cc.fascinated.fascinatedutils.gui.renderer.UIRenderer;

/**
 * Layout root that expands a single reconciled child to the host bounds.
 */
public class FReconcileRoot extends FWidget {

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
        if (childrenView().size() == 1) {
            childrenView().get(0).layout(measure, layoutX, layoutY, layoutWidth, layoutHeight);
        }
    }
}
