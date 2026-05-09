package cc.fascinated.fascinatedutils.oldgui.core;

import cc.fascinated.fascinatedutils.oldgui.widgets.FWidget;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * A keyed, stateful widget node. State is preserved across re-renders as long as the same key
 * appears at the same position in the registry.
 *
 * <p>Usage:
 * <pre>
 *   FWidgetNode.of("presence-picker", ctx -> {
 *       FState&lt;Boolean&gt; open = ctx.useState(false);
 *       return new MyPickerWidget(open);
 *   });
 * </pre>
 */
public class FWidgetNode {
    private final String key;
    private final Function<RenderContext, FWidget> renderFn;

    final Map<Integer, Object> stateSlots = new LinkedHashMap<>();
    int stateSlotCursor = 0;
    boolean dirty = true;
    private FWidget lastResult;
    private Runnable onDirty;

    private FWidgetNode(String key, Function<RenderContext, FWidget> renderFn) {
        this.key = key;
        this.renderFn = renderFn;
    }

    public static FWidgetNode of(String key, Function<RenderContext, FWidget> renderFn) {
        return new FWidgetNode(key, renderFn);
    }

    void attachDirtyCallback(Runnable onDirty) {
        this.onDirty = onDirty;
    }

    /**
     * Returns the current widget, re-invoking the render function if the node is dirty.
     * Resets the slot cursor before each render so hook ordering is stable.
     */
    FWidget resolveWidget() {
        if (!dirty && lastResult != null) {
            return lastResult;
        }
        stateSlotCursor = 0;
        lastResult = renderFn.apply(new RenderContext(this));
        dirty = false;
        return lastResult;
    }

    void markDirty() {
        dirty = true;
        if (onDirty != null) {
            onDirty.run();
        }
    }

    public String key() {
        return key;
    }

    /**
     * Passed to the render function once per resolve. Provides hook primitives.
     */
    public static final class RenderContext {
        private final FWidgetNode node;

        RenderContext(FWidgetNode node) {
            this.node = node;
        }

        /**
         * Returns persisted state for the current slot index.
         * On first render initialises the slot with {@code defaultValue}.
         * On subsequent renders returns the previously stored {@link FState}.
         */
        @SuppressWarnings("unchecked")
        public <T> FState<T> useState(T defaultValue) {
            int slot = node.stateSlotCursor++;
            if (!node.stateSlots.containsKey(slot)) {
                node.stateSlots.put(slot, new FState<>(defaultValue, node::markDirty));
            }
            return (FState<T>) node.stateSlots.get(slot);
        }

        /**
         * Derived value recomputed on every render. No memoisation.
         */
        public <T> T useDerived(Supplier<T> compute) {
            return compute.get();
        }
    }
}
