package cc.fascinated.fascinatedutils.oldgui.widgets;

import cc.fascinated.fascinatedutils.oldgui.renderer.UIRenderer;

public class FAbsoluteStackWidget extends FWidget {
    @Override
    public void layout(UIRenderer measure, float layoutX, float layoutY, float layoutWidth, float layoutHeight) {
        setBounds(layoutX, layoutY, layoutWidth, layoutHeight);
        for (FWidget child : childrenView()) {
            child.layout(measure, layoutX, layoutY, layoutWidth, layoutHeight);
        }
    }
}
