package cc.fascinated.fascinatedutils.gui.core;

import java.util.ArrayList;
import java.util.List;

public class UiFocusManager {
    private UiNode focusedNode;

    public UiNode focusedNode() {
        return focusedNode;
    }

    public void clearFocus() {
        focus(null);
    }

    public void focus(UiNode nextFocusedNode) {
        if (focusedNode == nextFocusedNode) {
            return;
        }
        if (focusedNode != null) {
            focusedNode.onFocusLost();
        }
        focusedNode = nextFocusedNode;
        if (focusedNode != null) {
            focusedNode.onFocusGained();
        }
    }

    public void ensureValid(UiNode rootNode) {
        if (focusedNode == null) {
            return;
        }
        if (!contains(rootNode, focusedNode) || !focusedNode.visible()) {
            clearFocus();
        }
    }

    public boolean focusNext(UiNode rootNode) {
        return moveFocus(rootNode, true);
    }

    public boolean focusPrevious(UiNode rootNode) {
        return moveFocus(rootNode, false);
    }

    private boolean moveFocus(UiNode rootNode, boolean forward) {
        List<UiNode> focusableNodes = new ArrayList<>();
        collectFocusable(rootNode, focusableNodes);
        if (focusableNodes.isEmpty()) {
            clearFocus();
            return false;
        }

        int currentIndex = focusableNodes.indexOf(focusedNode);
        int nextIndex;
        if (currentIndex < 0) {
            nextIndex = forward ? 0 : focusableNodes.size() - 1;
        }
        else if (forward) {
            nextIndex = (currentIndex + 1) % focusableNodes.size();
        }
        else {
            nextIndex = (currentIndex - 1 + focusableNodes.size()) % focusableNodes.size();
        }
        UiNode nextNode = focusableNodes.get(nextIndex);
        focus(nextNode);
        for (UiNode ancestor = nextNode.parent(); ancestor != null; ancestor = ancestor.parent()) {
            ancestor.revealDescendant(nextNode);
        }
        return true;
    }

    /**
     * Checks whether the focused node lies inside the given subtree.
     *
     * @param scopeNode the subtree root
     * @return true when a node is focused and it is the scope node or one of its descendants
     */
    public boolean isWithin(UiNode scopeNode) {
        for (UiNode cursor = focusedNode; cursor != null; cursor = cursor.parent()) {
            if (cursor == scopeNode) {
                return true;
            }
        }
        return false;
    }

    /**
     * Focuses the first focusable node in the given subtree.
     *
     * @param scopeNode the subtree root
     */
    public void focusFirst(UiNode scopeNode) {
        List<UiNode> focusableNodes = new ArrayList<>();
        collectFocusable(scopeNode, focusableNodes);
        if (focusableNodes.isEmpty()) {
            clearFocus();
            return;
        }
        focus(focusableNodes.get(0));
    }

    private static void collectFocusable(UiNode node, List<UiNode> output) {
        if (node == null || !node.visible()) {
            return;
        }
        if (node.focusable()) {
            output.add(node);
        }
        for (UiNode childNode : node.childrenView()) {
            collectFocusable(childNode, output);
        }
    }

    private static boolean contains(UiNode rootNode, UiNode targetNode) {
        if (rootNode == null || targetNode == null) {
            return false;
        }
        if (rootNode == targetNode) {
            return true;
        }
        for (UiNode childNode : rootNode.childrenView()) {
            if (contains(childNode, targetNode)) {
                return true;
            }
        }
        return false;
    }
}
