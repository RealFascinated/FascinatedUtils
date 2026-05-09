package cc.fascinated.fascinatedutils.oldgui.core;

@FunctionalInterface
public interface Callback<T> {
    /**
     * Invokes the listener with a typed argument.
     */
    void invoke(T value);
}
