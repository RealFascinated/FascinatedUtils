package cc.fascinated.fascinatedutils.gui.core;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Ref<T> {
    private T value;

    public Ref(T value) {
        this.value = value;
    }

    public static <T> Ref<T> of(T value) {
        return new Ref<>(value);
    }
}