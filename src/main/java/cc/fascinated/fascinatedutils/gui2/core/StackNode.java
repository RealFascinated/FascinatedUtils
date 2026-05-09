package cc.fascinated.fascinatedutils.gui2.core;

import cc.fascinated.fascinatedutils.gui2.render.RenderFrame;

/**
 * Overlay container that gives each child the full parent bounds.
 */
public class StackNode extends UiNode {
    @Override
    public void layout(RenderFrame renderFrame, int positionX, int positionY, int width, int height) {
        bounds().set(positionX, positionY, width, height);
        for (UiNode childNode : childrenView()) {
            childNode.layout(renderFrame, positionX, positionY, width, height);
        }
    }
}
