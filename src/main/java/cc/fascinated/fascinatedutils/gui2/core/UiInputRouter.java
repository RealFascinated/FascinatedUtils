package cc.fascinated.fascinatedutils.gui2.core;

import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class UiInputRouter {
    private UiNode hoverLeaf;
    private UiNode pressedLeaf;
    private final UiFocusManager focusManager = new UiFocusManager();

    public UiFocusManager focusManager() {
        return focusManager;
    }

    public boolean dispatch(UiNode root, UiEvent event) {
        focusManager.ensureValid(root);
        if (event instanceof UiEvent.PointerMove pointerMove) {
            return handlePointerMove(root, pointerMove);
        }
        if (event instanceof UiEvent.PointerPress pointerPress) {
            return handlePointerPress(root, pointerPress);
        }
        if (event instanceof UiEvent.PointerRelease pointerRelease) {
            return handlePointerRelease(root, pointerRelease);
        }
        if (event instanceof UiEvent.PointerScroll pointerScroll) {
            return handlePointerScroll(root, pointerScroll);
        }
        if (event instanceof UiEvent.KeyPress keyPress) {
            return handleKeyPress(root, keyPress);
        }
        if (event instanceof UiEvent.CharType charType) {
            return handleCharType(root, charType);
        }
        return false;
    }

    private boolean handlePointerMove(UiNode root, UiEvent.PointerMove pointerMove) {
        // Always update hover state so that nodes rebuilt each frame (recompose) still receive
        // onPointerEnter/Leave even while a mouse button is held down.
        UiNode nextHoverLeaf = hitLeaf(root, pointerMove.pointerX(), pointerMove.pointerY());
        if (nextHoverLeaf != hoverLeaf) {
            transitionHover(hoverLeaf, nextHoverLeaf, pointerMove.pointerX(), pointerMove.pointerY());
            hoverLeaf = nextHoverLeaf;
        }
        if (pressedLeaf != null) {
            return bubblePointerMove(pressedLeaf, pointerMove.pointerX(), pointerMove.pointerY());
        }
        if (nextHoverLeaf == null) {
            return false;
        }
        return bubblePointerMove(nextHoverLeaf, pointerMove.pointerX(), pointerMove.pointerY());
    }

    private boolean handlePointerPress(UiNode root, UiEvent.PointerPress pointerPress) {
        UiNode hitLeaf = hitLeaf(root, pointerPress.pointerX(), pointerPress.pointerY());
        pressedLeaf = hitLeaf;
        if (hitLeaf != null && hitLeaf.focusable()) {
            focusManager.focus(hitLeaf);
        }
        else {
            focusManager.clearFocus();
        }
        if (hitLeaf == null) {
            return false;
        }
        return walkPointerPress(hitLeaf, pointerPress.pointerX(), pointerPress.pointerY(), pointerPress.button());
    }

    private boolean handlePointerRelease(UiNode root, UiEvent.PointerRelease pointerRelease) {
        UiNode releaseTarget = pressedLeaf != null ? pressedLeaf : hitLeaf(root, pointerRelease.pointerX(), pointerRelease.pointerY());
        boolean consumed = false;
        if (releaseTarget != null) {
            consumed = walkPointerRelease(releaseTarget, pointerRelease.pointerX(), pointerRelease.pointerY(), pointerRelease.button());
            consumed = walkClick(releaseTarget, pointerRelease.pointerX(), pointerRelease.pointerY(), pointerRelease.button()) || consumed;
        }
        pressedLeaf = null;
        return consumed;
    }

    private boolean handlePointerScroll(UiNode root, UiEvent.PointerScroll pointerScroll) {
        UiNode scrollLeaf = hitLeaf(root, pointerScroll.pointerX(), pointerScroll.pointerY());
        if (scrollLeaf == null) {
            return false;
        }
        for (UiNode cursor = scrollLeaf; cursor != null; cursor = cursor.parent()) {
            if (cursor.onPointerScroll(pointerScroll.pointerX(), pointerScroll.pointerY(), pointerScroll.delta())) {
                return true;
            }
        }
        return false;
    }

    private boolean handleKeyPress(UiNode root, UiEvent.KeyPress keyPress) {
        UiNode focusedNode = focusManager.focusedNode();
        if (keyPress.keyCode() == GLFW.GLFW_KEY_TAB) {
            boolean backwards = (keyPress.modifiers() & GLFW.GLFW_MOD_SHIFT) != 0;
            if (backwards) {
                return focusManager.focusPrevious(root);
            }
            return focusManager.focusNext(root);
        }
        if (focusedNode == null) {
            return false;
        }
        List<UiNode> chain = pathToRoot(focusedNode);
        for (int index = chain.size() - 1; index >= 0; index--) {
            if (chain.get(index).capturesKeyPress(keyPress.keyCode(), keyPress.modifiers())) {
                return true;
            }
        }
        for (UiNode node : chain) {
            if (node.onKeyPress(keyPress.keyCode(), keyPress.modifiers())) {
                return true;
            }
        }
        return false;
    }

    private boolean handleCharType(UiNode root, UiEvent.CharType charType) {
        focusManager.ensureValid(root);
        UiNode focusedNode = focusManager.focusedNode();
        if (focusedNode == null) {
            return false;
        }
        List<UiNode> chain = pathToRoot(focusedNode);
        for (int index = chain.size() - 1; index >= 0; index--) {
            if (chain.get(index).capturesCharType(charType.character())) {
                return true;
            }
        }
        for (UiNode node : chain) {
            if (node.onCharType(charType.character())) {
                return true;
            }
        }
        return false;
    }

    private static UiNode hitLeaf(UiNode root, float pointerX, float pointerY) {
        if (root == null || !root.visible() || !root.enabled()) {
            return null;
        }
        if (root.clipChildren() && !root.contains(pointerX, pointerY)) {
            return null;
        }
        List<UiNode> childNodes = root.childrenView();
        for (int childIndex = childNodes.size() - 1; childIndex >= 0; childIndex--) {
            UiNode childLeaf = hitLeaf(childNodes.get(childIndex), pointerX, pointerY);
            if (childLeaf != null) {
                return childLeaf;
            }
        }
        if (root.contains(pointerX, pointerY) && root.blocksHitWhenEmpty() && root.enabled()) {
            return root;
        }
        return null;
    }


    private static List<UiNode> pathToRoot(UiNode leaf) {
        if (leaf == null) {
            return List.of();
        }
        List<UiNode> chain = new ArrayList<>();
        for (UiNode cursor = leaf; cursor != null; cursor = cursor.parent()) {
            chain.add(cursor);
        }
        return chain;
    }

    private static List<UiNode> pathRootToLeaf(UiNode leaf) {
        List<UiNode> bottomUpChain = pathToRoot(leaf);
        List<UiNode> topDownChain = new ArrayList<>(bottomUpChain);
        Collections.reverse(topDownChain);
        return topDownChain;
    }

    private static void transitionHover(UiNode previousLeaf, UiNode nextLeaf, float pointerX, float pointerY) {
        List<UiNode> oldPath = pathRootToLeaf(previousLeaf);
        List<UiNode> newPath = pathRootToLeaf(nextLeaf);
        int commonPrefixLength = 0;
        int commonPrefixMax = Math.min(oldPath.size(), newPath.size());
        while (commonPrefixLength < commonPrefixMax && oldPath.get(commonPrefixLength) == newPath.get(commonPrefixLength)) {
            commonPrefixLength++;
        }
        for (int oldIndex = oldPath.size() - 1; oldIndex >= commonPrefixLength; oldIndex--) {
            oldPath.get(oldIndex).onPointerLeave(pointerX, pointerY);
        }
        for (int newIndex = commonPrefixLength; newIndex < newPath.size(); newIndex++) {
            newPath.get(newIndex).onPointerEnter(pointerX, pointerY);
        }
    }

    private static boolean walkPointerPress(UiNode leaf, float pointerX, float pointerY, int button) {
        List<UiNode> chain = pathToRoot(leaf);
        for (int index = chain.size() - 1; index >= 0; index--) {
            if (chain.get(index).capturesPointerPress(pointerX, pointerY, button)) {
                return true;
            }
        }
        for (UiNode node : chain) {
            if (node.onPointerPress(pointerX, pointerY, button)) {
                return true;
            }
        }
        return false;
    }

    private static boolean walkPointerRelease(UiNode leaf, float pointerX, float pointerY, int button) {
        List<UiNode> chain = pathToRoot(leaf);
        for (int index = chain.size() - 1; index >= 0; index--) {
            if (chain.get(index).capturesPointerRelease(pointerX, pointerY, button)) {
                return true;
            }
        }
        boolean consumed = false;
        for (UiNode node : chain) {
            consumed |= node.onPointerRelease(pointerX, pointerY, button);
        }
        return consumed;
    }

    private static boolean walkClick(UiNode leaf, float pointerX, float pointerY, int button) {
        List<UiNode> chain = pathToRoot(leaf);
        for (int index = chain.size() - 1; index >= 0; index--) {
            if (chain.get(index).capturesClick(pointerX, pointerY, button)) {
                return true;
            }
        }
        boolean consumed = false;
        for (UiNode node : chain) {
            consumed |= node.onClick(pointerX, pointerY, button);
        }
        return consumed;
    }

    private static boolean bubblePointerMove(UiNode leaf, float pointerX, float pointerY) {
        List<UiNode> chain = pathToRoot(leaf);
        boolean consumed = false;
        for (UiNode node : chain) {
            consumed |= node.onPointerMove(pointerX, pointerY);
        }
        return consumed;
    }
}
