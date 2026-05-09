package cc.fascinated.fascinatedutils.gui2.node;

import cc.fascinated.fascinatedutils.gui2.core.UiNode;
import cc.fascinated.fascinatedutils.gui2.render.RenderFrame;

public class ClickableNode extends UiNode {

    private Runnable onPrimaryClick = () -> {};

    public ClickableNode setOnPrimaryClick(Runnable onPrimaryClick) {
        this.onPrimaryClick = onPrimaryClick == null ? () -> {} : onPrimaryClick;
        return this;
    }

    @Override
    public void layout(RenderFrame renderFrame, int positionX, int positionY, int width, int height) {
        bounds().set(positionX, positionY, width, height);
        for (UiNode child : childrenView()) {
            child.layout(renderFrame, positionX, positionY, width, height);
        }
    }

    @Override
    public boolean blocksHitWhenEmpty() {
        return true;
    }

    @Override
    public boolean onClick(float pointerX, float pointerY, int button) {
        if (button == 0) {
            onPrimaryClick.run();
            return true;
        }
        return false;
    }
}
