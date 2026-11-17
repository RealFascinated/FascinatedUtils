package cc.fascinated.fascinatedutils.gui.core.input;

import cc.fascinated.fascinatedutils.gui.core.UiNode;

import lombok.Getter;
import lombok.experimental.Accessors;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class UiFocusManager {
    @Getter
    @Accessors(fluent = true)
    private UiNode focusedNode;
    private boolean focusVisible;

    public void clearFocus() {
        focus(null);
    }

    /**
     * Moves focus to the node under a pointer press. The node does not show its focus indicator until a key is used.
     *
     * @param nextFocusedNode the node to focus
     */
    public void focusFromPointer(UiNode nextFocusedNode) {
        focus(nextFocusedNode, false);
    }

    /**
     * Makes the focus indicator of the focused node visible, as keyboard use does.
     */
    public void showFocusVisible() {
        if (focusedNode != null && !focusVisible) {
            focusVisible = true;
            focusedNode.markFocused(true, true);
        }
    }

    /**
     * Moves focus to the given node. The new node is recorded before any callback runs, so callbacks that move
     * focus again behave correctly.
     *
     * @param nextFocusedNode the node to focus, or null to clear focus
     */
    public void focus(UiNode nextFocusedNode) {
        focus(nextFocusedNode, true);
    }

    private void focus(UiNode nextFocusedNode, boolean nextFocusVisible) {
        UiNode previousFocusedNode = focusedNode;
        if (previousFocusedNode == nextFocusedNode) {
            return;
        }
        focusedNode = nextFocusedNode;
        focusVisible = nextFocusVisible;
        if (previousFocusedNode != null) {
            previousFocusedNode.markFocused(false, false);
        }
        if (nextFocusedNode != null) {
            nextFocusedNode.markFocused(true, nextFocusVisible);
        }
        if (previousFocusedNode != null) {
            previousFocusedNode.onFocusLost();
        }
        if (nextFocusedNode != null && focusedNode == nextFocusedNode) {
            nextFocusedNode.onFocusGained();
        }
    }

    /**
     * Moves focus to the node that replaced the focused node after recomposition, without firing focus callbacks.
     * When the focused node has no replacement, focus is cleared normally.
     *
     * @param previousToNext mapping from previous nodes to their replacements
     */
    void retarget(Map<UiNode, UiNode> previousToNext) {
        if (focusedNode == null) {
            return;
        }
        UiNode replacement = previousToNext.get(focusedNode);
        if (replacement == null) {
            clearFocus();
            return;
        }
        focusedNode.markFocused(false, false);
        replacement.markFocused(true, focusVisible);
        focusedNode = replacement;
    }

    /**
     * Clears focus when the focused node is no longer attached below the root or is hidden by itself or an ancestor.
     * Runs in time proportional to the depth of the focused node.
     *
     * @param rootNode the root of the live tree
     */
    public void ensureValid(UiNode rootNode) {
        if (focusedNode == null) {
            return;
        }
        for (UiNode cursor = focusedNode; cursor != null; cursor = cursor.parent()) {
            if (!cursor.visible()) {
                clearFocus();
                return;
            }
            if (cursor == rootNode) {
                return;
            }
        }
        clearFocus();
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
        if (node == null || !node.visible() || !node.acceptsInput()) {
            return;
        }
        if (node.focusable()) {
            output.add(node);
        }
        for (UiNode childNode : node.childrenView()) {
            collectFocusable(childNode, output);
        }
    }
}
