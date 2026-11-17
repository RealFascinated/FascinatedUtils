package cc.fascinated.fascinatedutils.gui.node.base;

import cc.fascinated.fascinatedutils.gui.layout.PositionedNode;
import cc.fascinated.fascinatedutils.gui.core.input.UiKeys;
import cc.fascinated.fascinatedutils.gui.render.AlumiteGuiRenderer;

import java.util.function.BooleanSupplier;

/**
 * Base for focusable controls that can be disabled and activated with the pointer or the activation keys. Provides
 * the shared focus ring and its metrics so every control highlights keyboard focus the same way.
 *
 * @param <T> the concrete node type, returned from fluent setters
 */
public abstract class InteractiveNode<T extends InteractiveNode<T>> extends PositionedNode<T> {
    public static final int FOCUS_RING_GAP = 1;
    public static final int FOCUS_RING_THICKNESS = 2;
    public static final int FOCUS_RING_OFFSET = FOCUS_RING_GAP + FOCUS_RING_THICKNESS;

    private BooleanSupplier disabledSupplier = () -> false;

    /**
     * Sets when this control is disabled. A disabled control cannot be focused or activated.
     *
     * @param disabledSupplier supplies whether the control is disabled; null means never
     * @return this node
     */
    public T setDisabled(BooleanSupplier disabledSupplier) {
        this.disabledSupplier = disabledSupplier == null ? () -> false : disabledSupplier;
        return self();
    }

    /**
     * Whether this control is currently disabled.
     *
     * @return true when the control ignores activation and cannot take focus
     */
    public boolean isDisabled() {
        return disabledSupplier.getAsBoolean();
    }

    @Override
    public boolean hitTarget() {
        return true;
    }

    @Override
    public boolean focusable() {
        return !isDisabled();
    }

    /**
     * Performs this control's action in response to a primary click or an activation key.
     *
     * @return true when the activation was handled
     */
    protected boolean activate() {
        return false;
    }

    @Override
    public boolean onClick(float pointerX, float pointerY, int button) {
        return button == 0 && !isDisabled() && activate();
    }

    @Override
    public boolean onKeyPress(int keyCode, int modifiers) {
        return UiKeys.isActivationKey(keyCode) && !isDisabled() && activate();
    }

    /**
     * Draws the keyboard focus ring around a rectangle when this node shows focus.
     *
     * @param renderFrame  the active renderer
     * @param positionX    left edge of the focused rectangle
     * @param positionY    top edge of the focused rectangle
     * @param width        width of the focused rectangle
     * @param height       height of the focused rectangle
     * @param cornerRadius corner radius of the focused rectangle
     */
    protected void drawFocusRing(AlumiteGuiRenderer renderFrame, float positionX, float positionY, float width, float height, float cornerRadius) {
        if (!isFocusVisible() || isDisabled()) {
            return;
        }
        renderFrame.drawRoundedRectFrame(positionX - FOCUS_RING_OFFSET, positionY - FOCUS_RING_OFFSET, width + FOCUS_RING_OFFSET * 2, height + FOCUS_RING_OFFSET * 2,
                cornerRadius + FOCUS_RING_OFFSET, renderFrame.theme().focusRing(), 0, FOCUS_RING_THICKNESS);
    }
}
