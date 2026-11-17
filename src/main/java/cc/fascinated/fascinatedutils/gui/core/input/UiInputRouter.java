package cc.fascinated.fascinatedutils.gui.core.input;

import cc.fascinated.fascinatedutils.gui.core.UiTooltips;

import cc.fascinated.fascinatedutils.gui.core.UiNode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;
import org.lwjgl.glfw.GLFW;

import java.util.List;
import java.util.Map;

/**
 * Routes {@link UiEvent}s to nodes: hover tracking, press/release/click, scrolling, keys and characters. When a
 * focus-trapping node (a modal) is visible, hit testing and bubbling are confined to its subtree.
 */
@RequiredArgsConstructor
public class UiInputRouter {
    private static final int NO_BUTTON = -1;
    private static final int MAX_HOVER_PASSES = 3;

    @Getter
    @Accessors(fluent = true)
    private final UiFocusManager focusManager = new UiFocusManager();
    private final UiTooltips tooltips;
    private List<UiNode> focusTraps = List.of();
    private UiNode hoverLeaf;
    private UiNode pressedLeaf;
    private UiNode pressConsumer;
    private int pressedButton = NO_BUTTON;
    private UiNode boundary;
    private boolean hasPointer;
    private float lastPointerX;
    private float lastPointerY;

    /**
     * Whether a pointer button is currently held on a node.
     *
     * @return true between a press and its release
     */
    public boolean hasPressed() {
        return pressedButton != NO_BUTTON;
    }

    /**
     * Re-points hover, press, focus and tooltip tracking at the nodes of a newly composed tree without firing any
     * enter, leave or focus callbacks, so recomposition is invisible to nodes.
     *
     * @param previousToNext mapping from previous nodes to their replacements
     * @param nextFocusTraps focus-trapping nodes of the new tree, topmost candidate first
     */
    public void retarget(Map<UiNode, UiNode> previousToNext, List<UiNode> nextFocusTraps) {
        focusTraps = nextFocusTraps;
        UiNode previousHover = hoverLeaf;
        hoverLeaf = null;
        // The deepest still-existing ancestor becomes the hover leaf; the next hover pass enters anything below it.
        for (UiNode cursor = previousHover; cursor != null && hoverLeaf == null; cursor = cursor.parent()) {
            hoverLeaf = previousToNext.get(cursor);
        }
        for (UiNode cursor = hoverLeaf; cursor != null; cursor = cursor.parent()) {
            cursor.markHovered(true);
        }
        pressedLeaf = pressedLeaf == null ? null : previousToNext.get(pressedLeaf);
        pressConsumer = pressConsumer == null ? null : previousToNext.get(pressConsumer);
        if (pressConsumer != null) {
            pressConsumer.markPressed(true);
        }
        focusManager.retarget(previousToNext);
    }

    /**
     * Re-evaluates what lies under the last known pointer position, for example after recomposition and layout, so
     * new nodes under the pointer become hovered without waiting for another pointer move.
     *
     * @param root the root of the live tree
     */
    public void refreshHover(UiNode root) {
        if (!hasPointer || root == null) {
            return;
        }
        UiNode trap = activeFocusTrap();
        boundary = trap;
        updateHover(trap != null ? trap : root, lastPointerX, lastPointerY);
    }

    /**
     * Whether a hit target lies under a point, honouring any active focus trap, without changing any tracking state.
     *
     * @param root     the root of the live tree
     * @param pointerX the pointer X
     * @param pointerY the pointer Y
     * @return true when the point is over a node that takes pointer input
     */
    public boolean isOverHitTarget(UiNode root, float pointerX, float pointerY) {
        UiNode trap = activeFocusTrap();
        return hitLeaf(trap != null ? trap : root, pointerX, pointerY) != null;
    }

    /**
     * Drops all hover, press and focus tracking.
     */
    public void reset() {
        for (UiNode cursor = hoverLeaf; cursor != null; cursor = cursor.parent()) {
            cursor.markHovered(false);
        }
        if (pressConsumer != null) {
            pressConsumer.markPressed(false);
        }
        hoverLeaf = null;
        pressedLeaf = null;
        pressConsumer = null;
        pressedButton = NO_BUTTON;
        boundary = null;
        hasPointer = false;
        focusTraps = List.of();
        focusManager.clearFocus();
    }

    /**
     * Dispatches one event against the live tree.
     *
     * @param root  the root of the live tree
     * @param event the event to deliver
     * @return true when a node consumed the event
     */
    public boolean dispatch(UiNode root, UiEvent event) {
        focusManager.ensureValid(root);
        UiNode trap = activeFocusTrap();
        boundary = trap;
        boolean keyboardEvent = event instanceof UiEvent.KeyPress || event instanceof UiEvent.CharType;
        if (trap != null && keyboardEvent && !focusManager.isWithin(trap)) {
            focusManager.focusFirst(trap);
            boolean typingEvent = event instanceof UiEvent.CharType
                    || event instanceof UiEvent.KeyPress keyPress && keyPress.keyCode() != GLFW.GLFW_KEY_ESCAPE;
            if (typingEvent) {
                return true;
            }
        }
        UiNode hitRoot = trap != null ? trap : root;
        return switch (event) {
            case UiEvent.PointerMove pointerMove -> handlePointerMove(hitRoot, pointerMove);
            case UiEvent.PointerPress pointerPress -> handlePointerPress(hitRoot, pointerPress);
            case UiEvent.PointerRelease pointerRelease -> handlePointerRelease(pointerRelease);
            case UiEvent.PointerScroll pointerScroll -> handlePointerScroll(hitRoot, pointerScroll);
            case UiEvent.KeyPress keyPress -> handleKeyPress(hitRoot, keyPress);
            case UiEvent.CharType charType -> handleCharType(charType);
        };
    }

    private boolean handlePointerMove(UiNode hitRoot, UiEvent.PointerMove pointerMove) {
        rememberPointer(pointerMove.pointerX(), pointerMove.pointerY());
        // Hover is always updated, even with a button held, so nodes rebuilt by recomposition keep receiving enter/leave.
        updateHover(hitRoot, pointerMove.pointerX(), pointerMove.pointerY());
        UiNode start = hasPressed() ? (pressConsumer != null ? pressConsumer : pressedLeaf) : hoverLeaf;
        boolean consumed = false;
        for (UiNode node = start; node != null; node = parentWithinBoundary(node)) {
            consumed |= node.onPointerMove(pointerMove.pointerX(), pointerMove.pointerY());
        }
        return consumed;
    }

    private boolean handlePointerPress(UiNode hitRoot, UiEvent.PointerPress pointerPress) {
        rememberPointer(pointerPress.pointerX(), pointerPress.pointerY());
        if (hasPressed()) {
            return false;
        }
        UiNode hitLeaf = hitLeaf(hitRoot, pointerPress.pointerX(), pointerPress.pointerY());
        tooltips.reset();
        if (hitLeaf != null && hitLeaf.focusable()) {
            focusManager.focusFromPointer(hitLeaf);
        }
        else if (hitLeaf != null || boundary == null) {
            focusManager.clearFocus();
        }
        if (hitLeaf == null) {
            return false;
        }
        pressedLeaf = hitLeaf;
        pressedButton = pointerPress.button();
        for (UiNode node = hitLeaf; node != null; node = parentWithinBoundary(node)) {
            if (node.onPointerPress(pointerPress.pointerX(), pointerPress.pointerY(), pointerPress.button())) {
                pressConsumer = node;
                node.markPressed(true);
                return true;
            }
        }
        return false;
    }

    private boolean handlePointerRelease(UiEvent.PointerRelease pointerRelease) {
        rememberPointer(pointerRelease.pointerX(), pointerRelease.pointerY());
        if (!hasPressed() || pointerRelease.button() != pressedButton) {
            return false;
        }
        UiNode releasedLeaf = pressedLeaf;
        UiNode releasedConsumer = pressConsumer;
        float pointerX = pointerRelease.pointerX();
        float pointerY = pointerRelease.pointerY();
        boolean consumed = false;
        for (UiNode node = releasedConsumer != null ? releasedConsumer : releasedLeaf; node != null; node = parentWithinBoundary(node)) {
            if (node.onPointerRelease(pointerX, pointerY, pointerRelease.button())) {
                consumed = true;
                break;
            }
        }
        // The click fires only when the pointer is still over the pressed node; recomposition re-targets it to the live node.
        UiNode clickLeaf = releasedLeaf != null && releasedLeaf.contains(pointerX, pointerY) ? releasedLeaf : null;
        for (UiNode node = clickLeaf; node != null; node = parentWithinBoundary(node)) {
            if (node.onClick(pointerX, pointerY, pointerRelease.button())) {
                consumed = true;
                break;
            }
        }
        // Callbacks may have recomposed or swapped screens; clear whatever tracking points at now.
        if (pressConsumer != null) {
            pressConsumer.markPressed(false);
        }
        pressedLeaf = null;
        pressConsumer = null;
        pressedButton = NO_BUTTON;
        return consumed;
    }

    private boolean handlePointerScroll(UiNode hitRoot, UiEvent.PointerScroll pointerScroll) {
        rememberPointer(pointerScroll.pointerX(), pointerScroll.pointerY());
        UiNode scrollLeaf = hitLeaf(hitRoot, pointerScroll.pointerX(), pointerScroll.pointerY());
        for (UiNode node = scrollLeaf; node != null; node = parentWithinBoundary(node)) {
            if (node.onPointerScroll(pointerScroll.pointerX(), pointerScroll.pointerY(), pointerScroll.delta())) {
                return true;
            }
        }
        return false;
    }

    private boolean handleKeyPress(UiNode scopeRoot, UiEvent.KeyPress keyPress) {
        UiNode focusedNode = focusManager.focusedNode();
        focusManager.showFocusVisible();
        if (keyPress.keyCode() == GLFW.GLFW_KEY_TAB) {
            // The focused node may use Tab itself (for example to insert indentation) before navigation takes over.
            if (focusedNode != null && focusedNode.onKeyPress(keyPress.keyCode(), keyPress.modifiers())) {
                return true;
            }
            boolean backwards = (keyPress.modifiers() & GLFW.GLFW_MOD_SHIFT) != 0;
            return backwards ? focusManager.focusPrevious(scopeRoot) : focusManager.focusNext(scopeRoot);
        }
        for (UiNode node = focusedNode; node != null; node = parentWithinBoundary(node)) {
            if (node.onKeyPress(keyPress.keyCode(), keyPress.modifiers())) {
                return true;
            }
        }
        return false;
    }

    private boolean handleCharType(UiEvent.CharType charType) {
        for (UiNode node = focusManager.focusedNode(); node != null; node = parentWithinBoundary(node)) {
            if (node.onCharType(charType.codePoint())) {
                return true;
            }
        }
        return false;
    }

    private void rememberPointer(float pointerX, float pointerY) {
        hasPointer = true;
        lastPointerX = pointerX;
        lastPointerY = pointerY;
    }

    private UiNode parentWithinBoundary(UiNode node) {
        return node == boundary ? null : node.parent();
    }

    /**
     * Finds the topmost focus trap that is visible together with all of its ancestors.
     */
    private UiNode activeFocusTrap() {
        for (UiNode candidate : focusTraps) {
            if (isEffectivelyVisible(candidate)) {
                return candidate;
            }
        }
        return null;
    }

    private static boolean isEffectivelyVisible(UiNode node) {
        for (UiNode cursor = node; cursor != null; cursor = cursor.parent()) {
            if (!cursor.visible()) {
                return false;
            }
        }
        return true;
    }

    private void updateHover(UiNode hitRoot, float pointerX, float pointerY) {
        // A hover callback can reveal nodes under the pointer (for example hover-only buttons), so settle over a few passes.
        for (int pass = 0; pass < MAX_HOVER_PASSES; pass++) {
            UiNode nextHoverLeaf = hitLeaf(hitRoot, pointerX, pointerY);
            if (nextHoverLeaf == hoverLeaf) {
                break;
            }
            UiNode previousHoverLeaf = hoverLeaf;
            hoverLeaf = nextHoverLeaf;
            transitionHover(previousHoverLeaf, nextHoverLeaf, pointerX, pointerY);
        }
        tooltips.pointerMoved(hoverLeaf, pointerX, pointerY);
    }

    private static UiNode hitLeaf(UiNode root, float pointerX, float pointerY) {
        if (root == null || !root.visible() || !root.acceptsInput()) {
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
        if (root.hitTarget() && root.contains(pointerX, pointerY)) {
            return root;
        }
        return null;
    }

    private static void transitionHover(UiNode previousLeaf, UiNode nextLeaf, float pointerX, float pointerY) {
        UiNode commonAncestor = commonAncestor(previousLeaf, nextLeaf);
        for (UiNode node = previousLeaf; node != null && node != commonAncestor; node = node.parent()) {
            node.markHovered(false);
            node.onPointerLeave(pointerX, pointerY);
        }
        enterPath(nextLeaf, commonAncestor, pointerX, pointerY);
    }

    private static void enterPath(UiNode node, UiNode stopAt, float pointerX, float pointerY) {
        if (node == null || node == stopAt) {
            return;
        }
        enterPath(node.parent(), stopAt, pointerX, pointerY);
        node.markHovered(true);
        node.onPointerEnter(pointerX, pointerY);
    }

    private static UiNode commonAncestor(UiNode first, UiNode second) {
        if (first == null || second == null) {
            return null;
        }
        int firstDepth = depth(first);
        int secondDepth = depth(second);
        UiNode firstCursor = first;
        UiNode secondCursor = second;
        while (firstDepth > secondDepth) {
            firstCursor = firstCursor.parent();
            firstDepth--;
        }
        while (secondDepth > firstDepth) {
            secondCursor = secondCursor.parent();
            secondDepth--;
        }
        while (firstCursor != secondCursor) {
            firstCursor = firstCursor.parent();
            secondCursor = secondCursor.parent();
        }
        return firstCursor;
    }

    private static int depth(UiNode node) {
        int depth = 0;
        for (UiNode cursor = node.parent(); cursor != null; cursor = cursor.parent()) {
            depth++;
        }
        return depth;
    }
}
