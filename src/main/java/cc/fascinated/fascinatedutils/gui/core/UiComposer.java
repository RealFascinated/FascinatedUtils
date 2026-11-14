package cc.fascinated.fascinatedutils.gui.core;

@FunctionalInterface
public interface UiComposer {
    UiNode compose(UiStateStore stateStore);
}
