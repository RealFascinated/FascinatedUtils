package cc.fascinated.fascinatedutils.gui.core;

import lombok.Getter;
import lombok.experimental.Accessors;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * Matches the nodes of a freshly composed tree to the nodes of the previous tree so per-node state survives
 * recomposition. Nodes with a node id match by id; other nodes match by their parent's match, class and index among
 * same-class siblings without an id.
 */
@Accessors(fluent = true)
public class UiReconciler {
    /**
     * Identity map from previous nodes to the nodes that replaced them, from the latest reconcile.
     */
    @Getter
    private final Map<UiNode, UiNode> previousToNext = new IdentityHashMap<>();
    /**
     * Focus-trapping nodes of the latest new tree, topmost candidate first.
     */
    @Getter
    private final List<UiNode> focusTraps = new ArrayList<>();
    private final Map<String, UiNode> previousById = new HashMap<>();

    /**
     * Reconciles a new tree with the previous one: calls {@link UiNode#inheritState(UiNode)} on each matched node,
     * {@link UiNode#onComposed(UiComposeContext)} on every node and records the focus traps of the new tree.
     *
     * @param previousRoot the root of the previous tree, or null
     * @param nextRoot     the root of the new tree, or null
     * @param context      the context passed to {@link UiNode#onComposed(UiComposeContext)}
     */
    public void reconcile(UiNode previousRoot, UiNode nextRoot, UiComposeContext context) {
        // The collections are reused so recomposing every frame does not allocate them again.
        previousToNext.clear();
        focusTraps.clear();
        previousById.clear();
        if (nextRoot == null) {
            return;
        }
        collectIds(previousRoot, previousById);
        UiNode rootMatch = previousRoot != null && previousRoot.getClass() == nextRoot.getClass() ? previousRoot : null;
        visit(nextRoot, rootMatch, previousById, context);
        collectFocusTraps(nextRoot);
    }

    private void visit(UiNode next, UiNode previous, Map<String, UiNode> previousById, UiComposeContext context) {
        if (previous != null) {
            previousToNext.put(previous, next);
            next.inheritState(previous);
        }
        next.onComposed(context);
        List<UiNode> nextChildren = next.childrenView();
        if (nextChildren.isEmpty()) {
            return;
        }
        Map<Class<?>, List<UiNode>> previousByClass = groupIdLessByClass(previous);
        Map<Class<?>, Integer> ordinals = new HashMap<>();
        for (UiNode nextChild : nextChildren) {
            UiNode match = null;
            if (hasId(nextChild)) {
                UiNode candidate = previousById.remove(nextChild.nodeId());
                if (candidate != null && candidate.getClass() == nextChild.getClass()) {
                    match = candidate;
                }
            }
            else {
                int ordinal = ordinals.merge(nextChild.getClass(), 1, Integer::sum) - 1;
                List<UiNode> sameClass = previousByClass.get(nextChild.getClass());
                if (sameClass != null && ordinal < sameClass.size()) {
                    match = sameClass.get(ordinal);
                }
            }
            visit(nextChild, match, previousById, context);
        }
    }

    private static Map<Class<?>, List<UiNode>> groupIdLessByClass(UiNode previous) {
        if (previous == null || previous.childrenView().isEmpty()) {
            return Map.of();
        }
        Map<Class<?>, List<UiNode>> grouped = new HashMap<>();
        for (UiNode previousChild : previous.childrenView()) {
            if (!hasId(previousChild)) {
                grouped.computeIfAbsent(previousChild.getClass(), ignored -> new ArrayList<>()).add(previousChild);
            }
        }
        return grouped;
    }

    private static void collectIds(UiNode node, Map<String, UiNode> output) {
        if (node == null) {
            return;
        }
        if (hasId(node)) {
            output.putIfAbsent(node.nodeId(), node);
        }
        for (UiNode child : node.childrenView()) {
            collectIds(child, output);
        }
    }

    private void collectFocusTraps(UiNode node) {
        List<UiNode> childNodes = node.childrenView();
        for (int childIndex = childNodes.size() - 1; childIndex >= 0; childIndex--) {
            collectFocusTraps(childNodes.get(childIndex));
        }
        if (node.trapsFocus()) {
            focusTraps.add(node);
        }
    }

    private static boolean hasId(UiNode node) {
        String nodeId = node.nodeId();
        return nodeId != null && !nodeId.isBlank();
    }
}
