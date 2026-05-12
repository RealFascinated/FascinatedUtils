package cc.fascinated.fascinatedutils.gui2.core;

import java.util.Map;

/**
 * Thread-local context that carries the host's persisted scroll-offset map into node constructors
 * during the compose phase. {@link cc.fascinated.fascinatedutils.gui2.node.ScrollColumnNode} reads
 * this context when {@code persistScroll(key)} is called so it can restore and auto-save its scroll
 * position across recomposes without any stateStore boilerplate in the caller.
 */
public class NodeScrollContext {
    private static final ThreadLocal<Map<String, Integer>> CURRENT = new ThreadLocal<>();

    private NodeScrollContext() {}

    static void push(Map<String, Integer> offsets) {
        CURRENT.set(offsets);
    }

    static void pop() {
        CURRENT.remove();
    }

    /**
     * Returns the active scroll-offset map, or {@code null} when called outside the compose phase.
     */
    public static Map<String, Integer> get() {
        return CURRENT.get();
    }
}
