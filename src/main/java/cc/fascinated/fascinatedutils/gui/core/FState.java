package cc.fascinated.fascinatedutils.gui.core;

import java.util.function.UnaryOperator;

/**
 * Mutable state cell owned by a widget node.
 *
 * <p>Reading {@link #get()} during render is fine; calling {@link #set(Object)} schedules
 * a re-render of the owning {@link FWidgetNode} via the dirty callback.
 * Use {@link #setQuiet(Object)} to update the stored value without scheduling a re-render —
 * useful for high-frequency events such as scroll position changes.
 */
public class FState<T> {
    T value;
    private final Runnable markDirty;

    FState(T initial, Runnable markDirty) {
        this.value = initial;
        this.markDirty = markDirty;
    }

    public T get() {
        return value;
    }

    public void set(T newValue) {
        this.value = newValue;
        markDirty.run();
    }

    public void update(UnaryOperator<T> fn) {
        set(fn.apply(value));
    }

    /**
     * Updates the stored value without invoking the dirty callback.
     * Use for high-frequency side-channel writes (e.g. scroll offset sync)
     * where a re-render should not be triggered on every event.
     */
    public void setQuiet(T newValue) {
        this.value = newValue;
    }

    /**
     * Creates a standalone state cell with no dirty callback.
     * Suitable for mutable holders outside a {@link FWidgetNode} context
     * (e.g. scroll position fields on a plain FWidget subclass).
     * Calling {@link #set(Object)} on a standalone instance is a value-only update
     * (equivalent to {@link #setQuiet(Object)}).
     */
    public static <T> FState<T> standalone(T initial) {
        return new FState<>(initial, () -> {});
    }
}
