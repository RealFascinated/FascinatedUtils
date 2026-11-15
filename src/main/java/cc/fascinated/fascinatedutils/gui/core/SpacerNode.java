package cc.fascinated.fascinatedutils.gui.core;

import cc.fascinated.fascinatedutils.gui.render.AlumiteGuiRenderer;

public class SpacerNode extends UiNode {
    @Override
    public void layout(AlumiteGuiRenderer renderFrame, int positionX, int positionY, int width, int height) {
        bounds().set(positionX, positionY, width, height);
        for (UiNode child : childrenView()) {
            child.layout(renderFrame, positionX, positionY, width, height);
        }
    }
}
