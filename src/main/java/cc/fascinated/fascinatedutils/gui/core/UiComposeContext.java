package cc.fascinated.fascinatedutils.gui.core;

import cc.fascinated.fascinatedutils.gui.core.state.UiScrollState;

import java.util.HashMap;
import java.util.Map;

/**
 * Per-host data that nodes can reach once the compose phase has produced a tree. Delivered through
 * {@link UiNode#onComposed(UiComposeContext)}.
 */
public class UiComposeContext {
    private final Map<String, UiScrollState> scrollStates = new HashMap<>();

    /**
     * Returns the remembered scroll state for a key, creating an empty one on first use.
     *
     * @param key a key unique within the host
     * @return the mutable scroll state for the key
     */
    public UiScrollState scrollState(String key) {
        return scrollStates.computeIfAbsent(key, ignored -> new UiScrollState());
    }

    void clear() {
        scrollStates.clear();
    }
}
