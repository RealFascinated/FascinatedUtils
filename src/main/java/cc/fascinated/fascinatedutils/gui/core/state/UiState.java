package cc.fascinated.fascinatedutils.gui.core.state;

import java.util.Objects;

public class UiState<T> {
    private T value;
    private final Runnable invalidate;

    UiState(T initialValue, Runnable invalidate) {
        this.value = initialValue;
        this.invalidate = invalidate;
    }

    public T get() {
        return value;
    }

    /**
     * Stores a new value and requests a recompose when it differs. Writes made while the host is composing update the
     * value but do not request another compose.
     *
     * @param nextValue the new value
     */
    public void set(T nextValue) {
        if (Objects.equals(value, nextValue)) {
            return;
        }
        value = nextValue;
        invalidate.run();
    }
}
