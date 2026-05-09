package cc.fascinated.fascinatedutils.gui2.core;

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

    public void set(T nextValue) {
        if (Objects.equals(value, nextValue)) {
            return;
        }
        value = nextValue;
        invalidate.run();
    }
}
