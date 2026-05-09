package cc.fascinated.fascinatedutils.gui2.node;

import cc.fascinated.fascinatedutils.gui2.core.PositionedNode;
import cc.fascinated.fascinatedutils.gui2.core.UiNode;
import cc.fascinated.fascinatedutils.gui2.render.RenderFrame;

import java.util.ArrayList;
import java.util.List;

public class PopupNode extends PositionedNode {

    private static final int DEFAULT_POPUP_WIDTH = 220;
    private static final int DEFAULT_POPUP_HEIGHT = 100;


    private final List<UiNode> popupChildren = new ArrayList<>();
    private Runnable onDismiss = () -> {};
    private int popupWidth = DEFAULT_POPUP_WIDTH;
    private int popupHeight = DEFAULT_POPUP_HEIGHT;
    private int popupX;
    private int popupY;

    private final RectNode backdropNode = new RectNode();
    private final CardNode panelNode = new CardNode();

    public PopupNode() {
        full();
        backdropNode.full();
        backdropNode.setFillResolver(theme -> theme.popupBackdropFill());
        addChild(backdropNode);
        panelNode.full();
        addChild(panelNode);
    }

    public PopupNode setPopupWidth(int popupWidth) {
        this.popupWidth = Math.max(1, popupWidth);
        return this;
    }

    public PopupNode setPopupHeight(int popupHeight) {
        this.popupHeight = Math.max(1, popupHeight);
        return this;
    }

    public PopupNode setOnDismiss(Runnable onDismiss) {
        this.onDismiss = onDismiss == null ? () -> {} : onDismiss;
        return this;
    }

    /**
     * Adds a child node that will be laid out relative to the popup panel's top-left corner.
     * Use the normal fluent positioning API on the child before calling this.
     */
    public PopupNode addPopupChild(UiNode child) {
        popupChildren.add(child);
        addChild(child);
        return this;
    }

    @Override
    public boolean blocksHitWhenEmpty() {
        return true;
    }

    @Override
    public boolean onClick(float pointerX, float pointerY, int button) {
        if (button == 0 && !isInPopup(pointerX, pointerY)) {
            onDismiss.run();
            return true;
        }
        return false;
    }

    @Override
    public void layout(RenderFrame renderFrame, int parentX, int parentY, int parentWidth, int parentHeight) {
        bounds().set(parentX, parentY, parentWidth, parentHeight);
        popupX = parentX + (parentWidth - popupWidth) / 2;
        popupY = parentY + (parentHeight - popupHeight) / 2;

        backdropNode.layout(renderFrame, parentX, parentY, parentWidth, parentHeight);
        panelNode.layout(renderFrame, popupX, popupY, popupWidth, popupHeight);

        for (UiNode child : popupChildren) {
            child.layout(renderFrame, popupX, popupY, popupWidth, popupHeight);
        }
    }

    protected int popupX() {
        return popupX;
    }

    protected int popupY() {
        return popupY;
    }

    protected int popupWidth() {
        return popupWidth;
    }

    protected int popupHeight() {
        return popupHeight;
    }

    protected boolean isInPopup(float pointerX, float pointerY) {
        return pointerX >= popupX && pointerX <= popupX + popupWidth
                && pointerY >= popupY && pointerY <= popupY + popupHeight;
    }
}
