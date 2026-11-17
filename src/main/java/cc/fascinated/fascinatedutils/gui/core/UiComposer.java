package cc.fascinated.fascinatedutils.gui.core;

import cc.fascinated.fascinatedutils.gui.core.state.UiStateStore;

@FunctionalInterface
public interface UiComposer {
    UiNode compose(UiStateStore stateStore);
}
