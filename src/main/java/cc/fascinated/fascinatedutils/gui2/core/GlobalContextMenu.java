package cc.fascinated.fascinatedutils.gui2.core;

import cc.fascinated.fascinatedutils.gui2.node.ContextMenuNode;
import org.jspecify.annotations.Nullable;

import java.util.function.Supplier;

/**
 * Screen-agnostic singleton that tracks the one context menu allowed to be open at a time.
 *
 * <p>Callers open a menu by providing a factory that produces the configured {@link ContextMenuNode}.
 * The factory is invoked once per compose pass so the node is always fresh. Callers close the menu
 * by invoking {@link #close()}, typically from the node's {@code onClose} callback.
 *
 * <p>All access must occur on the render thread.
 */
public class GlobalContextMenu {

    @Nullable
    private static Supplier<ContextMenuNode> factory;

    private GlobalContextMenu() {}

    /**
     * Opens a context menu, replacing any previously open one.
     *
     * @param factory
     *         produces a fully configured {@link ContextMenuNode}; called once per compose pass
     */
    public static void open(Supplier<ContextMenuNode> factory) {
        GlobalContextMenu.factory = factory;
    }

    /**
     * Closes the active context menu. Safe to call when no menu is open.
     */
    public static void close() {
        factory = null;
    }

    /**
     * Returns {@code true} if a context menu is currently open.
     */
    public static boolean isOpen() {
        return factory != null;
    }

    /**
     * If a context menu is active, adds it to {@code root} as an overlay child.
     * Called by {@link cc.fascinated.fascinatedutils.gui2.screens.RootScreen} on every compose pass.
     */
    public static void mountIfActive(PositionedNode root) {
        if (factory != null) {
            root.addChild(factory.get());
        }
    }
}
