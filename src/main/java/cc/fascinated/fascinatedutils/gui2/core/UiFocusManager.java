package cc.fascinated.fascinatedutils.gui2.core;

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
        if (!contains(rootNode, focusedNode) || !focusedNode.enabled() || !focusedNode.visible()) {
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
        focus(focusableNodes.get(nextIndex));
        return true;
    }

    private static void collectFocusable(UiNode node, List<UiNode> output) {
        if (node == null || !node.visible() || !node.enabled()) {
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
