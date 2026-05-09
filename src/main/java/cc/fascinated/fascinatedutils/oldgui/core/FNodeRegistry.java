package cc.fascinated.fascinatedutils.oldgui.core;

import cc.fascinated.fascinatedutils.oldgui.widgets.FWidget;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/**
 * Preserves {@link FWidgetNode} state across frames by key.
 *
 * <p>Lives on the owning screen or component and must be disposed when the screen closes via
 * {@link #dispose()}.
 *
 * <p>Automatic GC: call {@link #gc()} once per layout pass (after all {@link #get} calls for
 * that pass). Any node whose key was not retrieved since the last {@code gc()} is considered
 * unmounted and its state is discarded.
 */
public class FNodeRegistry {
    private final Map<String, FWidgetNode> nodes = new HashMap<>();
    private final Set<String> touchedThisEpoch = new HashSet<>();
    private Runnable globalDirtyCallback;

    /**
     * Optional callback invoked whenever any {@link FState} in any owned node marks itself dirty.
     * Useful for triggering a re-layout from the host screen.
     */
    public void setGlobalDirtyCallback(Runnable callback) {
        this.globalDirtyCallback = callback;
    }

    /**
     * Retrieves an existing node for {@code key}, or creates one using {@code renderFn}.
     * The {@code renderFn} argument is ignored if the node already exists.
     * Marks the key as touched for the current GC epoch.
     */
    public FWidgetNode get(String key, Function<FWidgetNode.RenderContext, FWidget> renderFn) {
        touchedThisEpoch.add(key);
        return nodes.computeIfAbsent(key, k -> {
            FWidgetNode node = FWidgetNode.of(k, renderFn);
            node.attachDirtyCallback(globalDirtyCallback);
            return node;
        });
    }

    /**
     * Removes nodes that were not retrieved since the last call to {@code gc()}.
     * Call once per layout pass, after all {@link #get} calls for that pass.
     */
    public void gc() {
        nodes.keySet().retainAll(touchedThisEpoch);
        touchedThisEpoch.clear();
    }

    /**
     * Discards all nodes and their state. Call when the owning screen closes.
     */
    public void dispose() {
        nodes.clear();
        touchedThisEpoch.clear();
    }
}
