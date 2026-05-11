package cc.fascinated.fascinatedutils.gui2.core;

import cc.fascinated.fascinatedutils.gui2.render.RenderFrame;

public class SpacerNode extends UiNode {
    @Override
    public void layout(RenderFrame renderFrame, int positionX, int positionY, int width, int height) {
        bounds().set(positionX, positionY, width, height);
        for (UiNode child : childrenView()) {
            child.layout(renderFrame, positionX, positionY, width, height);
        }
    }
}
