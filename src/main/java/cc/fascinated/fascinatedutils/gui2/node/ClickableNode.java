package cc.fascinated.fascinatedutils.gui2.node;

import cc.fascinated.fascinatedutils.gui2.core.UiNode;
import cc.fascinated.fascinatedutils.gui2.render.RenderFrame;

import java.util.function.BiConsumer;

public class ClickableNode extends UiNode {

    private Runnable onPrimaryClick = () -> {};
    private BiConsumer<Float, Float> onSecondaryClick = null;

    public ClickableNode setOnPrimaryClick(Runnable onPrimaryClick) {
        this.onPrimaryClick = onPrimaryClick == null ? () -> {} : onPrimaryClick;
        return this;
    }

    public ClickableNode setOnSecondaryClick(BiConsumer<Float, Float> onSecondaryClick) {
        this.onSecondaryClick = onSecondaryClick;
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
        if (button == 1 && onSecondaryClick != null) {
            onSecondaryClick.accept(pointerX, pointerY);
            return true;
        }
        return false;
    }
}
