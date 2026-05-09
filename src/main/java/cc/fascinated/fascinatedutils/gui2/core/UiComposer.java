package cc.fascinated.fascinatedutils.gui2.core;

@FunctionalInterface
public interface UiComposer {
    UiNode compose(UiStateStore stateStore);
}
