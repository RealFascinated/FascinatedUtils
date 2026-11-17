package cc.fascinated.fascinatedutils.gui.core.state;

import lombok.RequiredArgsConstructor;

import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;

@RequiredArgsConstructor
public class UiStateStore {
    private static final int FOCUS_REQUEST_COMPOSES = 3;

    private final Map<String, UiState<?>> states = new HashMap<>();
    private final Runnable invalidate;
    private String requestedFocusNodeId;
    private int focusRequestComposesLeft;

    /**
     * Requests keyboard focus for the node with the given id. The request survives a few recompositions until a node
     * with that id exists.
     *
     * @param nodeId the id of the node to focus
     */
    public void requestFocus(String nodeId) {
        this.requestedFocusNodeId = nodeId;
        this.focusRequestComposesLeft = FOCUS_REQUEST_COMPOSES;
        invalidate.run();
    }

    /**
     * Offers the pending focus request, if any, to a focuser. The request is cleared once the focuser accepts it, and
     * dropped when it has been declined for every compose it survives.
     *
     * @param focuser focuses the node with the given id, returning false when no such node exists
     */
    public void applyFocusRequest(Predicate<String> focuser) {
        if (requestedFocusNodeId == null) {
            return;
        }
        if (focuser.test(requestedFocusNodeId)) {
            requestedFocusNodeId = null;
        }
        else if (--focusRequestComposesLeft <= 0) {
            requestedFocusNodeId = null;
        }
    }

    /**
     * Returns the state stored under a key, creating it on first use. The initial value is only used when the state
     * is created; later calls with the same key ignore it.
     *
     * @param key          the state key, unique within the host
     * @param initialValue the value to start with when the key has no state yet
     * @param <T>          the value type
     * @return the state for the key
     * @throws IllegalStateException when the key already holds a value of an incompatible final type
     */
    @SuppressWarnings("unchecked")
    public <T> UiState<T> state(String key, T initialValue) {
        UiState<?> existingState = states.get(key);
        if (existingState != null) {
            assertCompatible(key, existingState.get(), initialValue);
            return (UiState<T>) existingState;
        }
        UiState<T> newState = new UiState<>(initialValue, invalidate);
        states.put(key, newState);
        return newState;
    }

    private static void assertCompatible(String key, Object existingValue, Object requestedInitial) {
        if (existingValue == null || requestedInitial == null) {
            return;
        }
        Class<?> existingType = existingValue.getClass();
        Class<?> requestedType = requestedInitial.getClass();
        if (existingType != requestedType && Modifier.isFinal(existingType.getModifiers()) && Modifier.isFinal(requestedType.getModifiers())) {
            throw new IllegalStateException("UI state '" + key + "' holds a " + existingType.getName()
                    + " but was requested with a " + requestedType.getName() + " initial value");
        }
    }

    public void clear() {
        states.clear();
        requestedFocusNodeId = null;
    }
}
