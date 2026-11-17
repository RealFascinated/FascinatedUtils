package cc.fascinated.fascinatedutils.gui.node.popup;

import cc.fascinated.fascinatedutils.gui.node.display.CardNode;

import cc.fascinated.fascinatedutils.gui.node.display.RectNode;

import cc.fascinated.fascinatedutils.gui.layout.PositionedNode;
import cc.fascinated.fascinatedutils.gui.core.UiNode;
import cc.fascinated.fascinatedutils.gui.render.AlumiteGuiRenderer;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.Accessors;
import org.lwjgl.glfw.GLFW;

/**
 * A modal panel centred over a dimmed backdrop. Children added with {@link #addChild} are laid out inside the panel,
 * relative to its top-left corner, and the panel is clamped to the available space. Pressing and releasing the
 * pointer outside the panel, or pressing Escape, dismisses the popup.
 *
 * @param <T> the concrete popup type, returned from fluent setters
 */
@Accessors(fluent = true)
public class PopupNode<T extends PopupNode<T>> extends PositionedNode<T> {

    private static final int DEFAULT_POPUP_WIDTH = 220;
    private static final int DEFAULT_POPUP_HEIGHT = 100;

    private Runnable onDismiss = () -> {};
    @Getter(AccessLevel.PROTECTED)
    private int popupWidth = DEFAULT_POPUP_WIDTH;
    @Getter(AccessLevel.PROTECTED)
    private int popupHeight = DEFAULT_POPUP_HEIGHT;
    @Getter(AccessLevel.PROTECTED)
    private int popupX;
    @Getter(AccessLevel.PROTECTED)
    private int popupY;
    private boolean pressStartedOutside;

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

    public T setPopupWidth(int popupWidth) {
        this.popupWidth = Math.max(1, popupWidth);
        return self();
    }

    public T setPopupHeight(int popupHeight) {
        this.popupHeight = Math.max(1, popupHeight);
        return self();
    }

    public T setOnDismiss(Runnable onDismiss) {
        this.onDismiss = onDismiss == null ? () -> {} : onDismiss;
        return self();
    }

    @Override
    public boolean hitTarget() {
        return true;
    }

    @Override
    public boolean trapsFocus() {
        return true;
    }

    @Override
    protected void inheritState(UiNode previous) {
        super.inheritState(previous);
        pressStartedOutside = ((PopupNode<?>) previous).pressStartedOutside;
    }

    @Override
    public boolean onPointerPress(float pointerX, float pointerY, int button) {
        if (button != 0) {
            return false;
        }
        pressStartedOutside = !isInPopup(pointerX, pointerY);
        return true;
    }

    @Override
    public boolean onClick(float pointerX, float pointerY, int button) {
        boolean dismissed = button == 0 && pressStartedOutside && !isInPopup(pointerX, pointerY);
        pressStartedOutside = false;
        if (dismissed) {
            onDismiss.run();
        }
        return dismissed;
    }

    @Override
    public boolean onKeyPress(int keyCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            onDismiss.run();
            return true;
        }
        return false;
    }

    /**
     * Sets the panel size through {@link #setPopupWidth} and {@link #setPopupHeight}. Called before every layout and
     * whenever layout inputs are checked, so it must be cheap, idempotent and must not change layout constraints of
     * other nodes.
     *
     * @param renderFrame     the active renderer, used to measure text
     * @param availableWidth  the width the popup may use
     * @param availableHeight the height the popup may use
     */
    protected void configurePopup(AlumiteGuiRenderer renderFrame, int availableWidth, int availableHeight) {
    }

    @Override
    protected boolean layoutInputsChanged(AlumiteGuiRenderer renderFrame) {
        int previousWidth = popupWidth;
        int previousHeight = popupHeight;
        sizePopup(renderFrame, bounds().width(), bounds().height());
        return super.layoutInputsChanged(renderFrame) || previousWidth != popupWidth || previousHeight != popupHeight;
    }

    private void sizePopup(AlumiteGuiRenderer renderFrame, int availableWidth, int availableHeight) {
        configurePopup(renderFrame, availableWidth, availableHeight);
        popupWidth = Math.max(1, Math.min(popupWidth, availableWidth));
        popupHeight = Math.max(1, Math.min(popupHeight, availableHeight));
    }

    @Override
    protected void arrangeChildren(AlumiteGuiRenderer renderFrame, int positionX, int positionY, int width, int height) {
        sizePopup(renderFrame, width, height);
        popupX = positionX + (width - popupWidth) / 2;
        popupY = positionY + (height - popupHeight) / 2;
        backdropNode.layout(renderFrame, positionX, positionY, width, height);
        arrangePopupChildren(renderFrame, popupX, popupY, popupWidth, popupHeight);
    }

    /**
     * Lays out the panel and the popup's content. The default gives the panel and every other child the whole panel
     * rectangle as its slot; override to place content explicitly.
     *
     * @param renderFrame the active renderer
     * @param panelX      the panel's left edge
     * @param panelY      the panel's top edge
     * @param panelWidth  the panel's width
     * @param panelHeight the panel's height
     */
    protected void arrangePopupChildren(AlumiteGuiRenderer renderFrame, int panelX, int panelY, int panelWidth, int panelHeight) {
        for (UiNode childNode : childrenView()) {
            if (childNode.visible() && childNode != backdropNode) {
                childNode.layout(renderFrame, panelX, panelY, panelWidth, panelHeight);
            }
        }
    }

    @Override
    protected void onTranslated(int deltaX, int deltaY) {
        popupX += deltaX;
        popupY += deltaY;
    }

    protected boolean isInPopup(float pointerX, float pointerY) {
        return pointerX >= popupX && pointerX <= popupX + popupWidth
                && pointerY >= popupY && pointerY <= popupY + popupHeight;
    }
}
