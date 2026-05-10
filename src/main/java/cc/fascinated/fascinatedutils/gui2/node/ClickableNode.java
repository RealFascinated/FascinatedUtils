package cc.fascinated.fascinatedutils.gui2.node;

import cc.fascinated.fascinatedutils.gui2.core.UiNode;

public class ClickableNode extends UiNode {

    private Runnable onPrimaryClick = () -> {};

    public ClickableNode setOnPrimaryClick(Runnable onPrimaryClick) {
        this.onPrimaryClick = onPrimaryClick == null ? () -> {} : onPrimaryClick;
        return this;
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
