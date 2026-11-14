package cc.fascinated.fascinatedutils.gui.core;

import cc.fascinated.fascinatedutils.gui.render.GuiRenderer;

public class StackNode extends UiNode {
    @Override
    public void layout(GuiRenderer renderFrame, int positionX, int positionY, int width, int height) {
        bounds().set(positionX, positionY, width, height);
        for (UiNode childNode : childrenView()) {
            childNode.layout(renderFrame, positionX, positionY, width, height);
        }
    }
}
