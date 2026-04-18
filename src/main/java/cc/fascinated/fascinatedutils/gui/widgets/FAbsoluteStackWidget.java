package cc.fascinated.fascinatedutils.gui.widgets;

import cc.fascinated.fascinatedutils.gui.renderer.UIRenderer;

public class FAbsoluteStackWidget extends FWidget {
    @Override
    public void layout(UIRenderer measure, float layoutX, float layoutY, float layoutWidth, float layoutHeight) {
        setBounds(layoutX, layoutY, layoutWidth, layoutHeight);
        for (FWidget child : childrenView()) {
            child.layout(measure, layoutX, layoutY, layoutWidth, layoutHeight);
        }
    }
}
