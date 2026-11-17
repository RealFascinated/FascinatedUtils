package cc.fascinated.fascinatedutils.gui.node.base;

import cc.fascinated.fascinatedutils.gui.anim.AnimHandle;
import cc.fascinated.fascinatedutils.gui.core.UiNode;
import lombok.Getter;

/**
 * Base for on/off controls: owns the checked state, the change callback, the hover animation and the checked
 * animation, and flips the state when activated.
 *
 * @param <T> the concrete node type, returned from fluent setters
 */
public abstract class ToggleNode<T extends ToggleNode<T>> extends InteractiveNode<T> {

    /**
     * Receives the new state after the user toggles a control.
     */
    @FunctionalInterface
    public interface ChangeListener {
        /**
         * Called after the state flipped.
         *
         * @param checked the new state
         */
        void onChange(boolean checked);
    }

    private final AnimHandle hoverAnim = new AnimHandle(0f);
    private final AnimHandle checkAnim = new AnimHandle(0f);
    @Getter
    private boolean checked;
    private ChangeListener onChange = ignored -> {};

    /**
     * Sets the checked state without notifying the change listener. The checked animation jumps to the new state;
     * recomposition keeps in-flight animations through {@link #inheritAnimationState}.
     *
     * @param checked the new state
     * @return this node
     */
    public T setChecked(boolean checked) {
        this.checked = checked;
        checkAnim.snap(checked ? 1f : 0f);
        return self();
    }

    /**
     * Sets the listener notified when the user toggles this control.
     *
     * @param onChange the listener; null removes it
     * @return this node
     */
    public T setOnChange(ChangeListener onChange) {
        this.onChange = onChange == null ? ignored -> {} : onChange;
        return self();
    }

    @Override
    protected boolean activate() {
        checked = !checked;
        onChange.onChange(checked);
        return true;
    }

    @Override
    protected void inheritAnimationState(UiNode previous) {
        ToggleNode<?> previousToggle = (ToggleNode<?>) previous;
        hoverAnim.snap(previousToggle.hoverAnim.value());
        checkAnim.snap(previousToggle.checkAnim.value());
    }

    /**
     * Advances the hover animation toward the pointer state.
     *
     * @param deltaSeconds elapsed frame time
     * @return the hover amount from 0 to 1
     */
    protected float tickHover(float deltaSeconds) {
        hoverAnim.target(isHovered() && !isDisabled() ? 1f : 0f);
        hoverAnim.tick(deltaSeconds);
        return hoverAnim.value();
    }

    /**
     * Advances the checked animation toward the checked state.
     *
     * @param deltaSeconds elapsed frame time
     * @param speed        the animation rate
     * @return the checked amount from 0 to 1
     */
    protected float tickChecked(float deltaSeconds, float speed) {
        checkAnim.speed(speed);
        checkAnim.target(checked ? 1f : 0f);
        checkAnim.tick(deltaSeconds);
        return checkAnim.value();
    }
}
