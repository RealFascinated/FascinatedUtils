package cc.fascinated.fascinatedutils.gui2.core;

import java.util.HashMap;
import java.util.Map;

/**
 * Keyed local state registry used by a composed gui2 tree.
 */
public class UiStateStore {
    private final Map<String, UiState<?>> states = new HashMap<>();
    private final Runnable invalidate;
    private String requestedFocusNodeId;

    public UiStateStore(Runnable invalidate) {
        this.invalidate = invalidate;
    }

    public void requestFocus(String nodeId) {
        this.requestedFocusNodeId = nodeId;
    }

    public String pollRequestedFocusNodeId() {
        String requested = requestedFocusNodeId;
        requestedFocusNodeId = null;
        return requested;
    }

    @SuppressWarnings("unchecked")
    public <T> UiState<T> state(String key, T initialValue) {
        UiState<?> existingState = states.get(key);
        if (existingState != null) {
            return (UiState<T>) existingState;
        }
        UiState<T> newState = new UiState<>(initialValue, invalidate);
        states.put(key, newState);
        return newState;
    }

    public void clear() {
        states.clear();
    }
}
