package cc.fascinated.fascinatedutils.gui2.core;

/**
 * Rebuild callback for stateful gui2 trees.
 */
@FunctionalInterface
public interface UiComposer {
    UiNode compose(UiStateStore stateStore);
}
