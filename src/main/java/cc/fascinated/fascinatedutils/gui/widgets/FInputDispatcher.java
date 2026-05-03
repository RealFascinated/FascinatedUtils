package cc.fascinated.fascinatedutils.gui.widgets;

import cc.fascinated.fascinatedutils.gui.UiSounds;
import cc.fascinated.fascinatedutils.gui.core.InputEvent;
import cc.fascinated.fascinatedutils.gui.core.UiFocusIds;
import cc.fascinated.fascinatedutils.gui.core.UiPointerCursor;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;

public class FInputDispatcher {
    private FWidget hoverLeaf;
    private FWidget pressedOn;
    private boolean clickSoundPlayedOnPress;

    private static FWidget findScrollHost(FWidget leaf) {
        for (FWidget cursor = leaf; cursor != null; cursor = cursor.parent()) {
            if (cursor instanceof FScrollable) {
                return cursor;
            }
        }
        return null;
    }

    private static FWidget findFocusElement(FWidget node, int focusId) {
        if (node == null || focusId == UiFocusIds.NO_FOCUS_ID) {
            return null;
        }
        for (FWidget child : node.childrenView()) {
            FWidget found = findFocusElement(child, focusId);
            if (found != null) {
                return found;
            }
        }
        if (node.focusId() == focusId) {
            return node;
        }
        return null;
    }

    private static boolean walkMouseDown(FWidget leaf, float pointerX, float pointerY, int button) {
        List<FWidget> chain = pathToRoot(leaf);
        for (int chainIndex = chain.size() - 1; chainIndex >= 0; chainIndex--) {
            if (chain.get(chainIndex).mouseDownCapture(pointerX, pointerY, button)) {
                return true;
            }
        }
        for (FWidget element : chain) {
            if (element.mouseDown(pointerX, pointerY, button)) {
                return true;
            }
        }
        return false;
    }

    private static boolean walkMouseUp(FWidget leaf, float pointerX, float pointerY, int button) {
        List<FWidget> chain = pathToRoot(leaf);
        for (int chainIndex = chain.size() - 1; chainIndex >= 0; chainIndex--) {
            if (chain.get(chainIndex).mouseUpCapture(pointerX, pointerY, button)) {
                return true;
            }
        }
        boolean any = false;
        for (FWidget element : chain) {
            any |= element.mouseUp(pointerX, pointerY, button);
        }
        return any;
    }

    private static boolean walkClick(FWidget leaf, float pointerX, float pointerY, int button) {
        List<FWidget> chain = pathToRoot(leaf);
        for (int chainIndex = chain.size() - 1; chainIndex >= 0; chainIndex--) {
            if (chain.get(chainIndex).clickCapture(pointerX, pointerY, button)) {
                return true;
            }
        }
        boolean any = false;
        for (FWidget element : chain) {
            any |= element.click(pointerX, pointerY, button);
        }
        return any;
    }

    private static boolean walkKeyDown(FWidget leaf, int keyCode, int modifiers) {
        List<FWidget> chain = pathToRoot(leaf);
        for (int chainIndex = chain.size() - 1; chainIndex >= 0; chainIndex--) {
            if (chain.get(chainIndex).keyDownCapture(keyCode, modifiers)) {
                return true;
            }
        }
        for (FWidget element : chain) {
            if (element.keyDown(keyCode, modifiers)) {
                return true;
            }
        }
        return false;
    }

    private static boolean walkCharTyped(FWidget leaf, char character) {
        List<FWidget> chain = pathToRoot(leaf);
        for (int chainIndex = chain.size() - 1; chainIndex >= 0; chainIndex--) {
            if (chain.get(chainIndex).charTypedCapture(character)) {
                return true;
            }
        }
        for (FWidget element : chain) {
            if (element.charTyped(character)) {
                return true;
            }
        }
        return false;
    }

    private static boolean walkBubbleMouseMove(FWidget leaf, float pointerX, float pointerY) {
        List<FWidget> chain = pathToRoot(leaf);
        boolean any = false;
        for (FWidget element : chain) {
            any |= element.mouseMove(pointerX, pointerY);
        }
        return any;
    }

    private static void walkBubbleMouseLeave(FWidget leaf, float pointerX, float pointerY) {
        List<FWidget> chain = pathToRoot(leaf);
        for (FWidget element : chain) {
            element.mouseLeave(pointerX, pointerY);
        }
    }

    private static void walkBubbleMouseEnter(FWidget leaf, float pointerX, float pointerY) {
        List<FWidget> chain = pathToRoot(leaf);
        for (FWidget element : chain) {
            element.mouseEnter(pointerX, pointerY);
        }
    }

    private static List<FWidget> pathToRoot(FWidget leaf) {
        List<FWidget> out = new ArrayList<>();
        for (FWidget cursor = leaf; cursor != null; cursor = cursor.parent()) {
            out.add(cursor);
        }
        return out;
    }

    private static FWidget hitLeaf(FWidget node, float pointerX, float pointerY) {
        if (node == null || !node.visible()) {
            return null;
        }
        if (node.clipChildren() && !node.containsPoint(pointerX, pointerY)) {
            return null;
        }
        float childPointerY = pointerY + node.childPointerYOffset();
        List<FWidget> kids = node.childrenView();
        for (int childIndex = kids.size() - 1; childIndex >= 0; childIndex--) {
            FWidget hitChild = hitLeaf(kids.get(childIndex), pointerX, childPointerY);
            if (hitChild != null) {
                return hitChild;
            }
        }
        if (node.wantsPointer() && node.containsPoint(pointerX, pointerY)) {
            return node;
        }
        return null;
    }

    public boolean dispatch(FWidget root, InputEvent raw, int focusedId, IntConsumer setFocusedId, IntSupplier getFocusedId) {
        if (root == null) {
            return false;
        }
        return switch (raw) {
            case InputEvent.MouseMove mouseMove -> handleMove(root, mouseMove.positionX(), mouseMove.positionY());
            case InputEvent.MousePress mousePress ->
                    handlePress(root, mousePress.positionX(), mousePress.positionY(), mousePress.button(), setFocusedId);
            case InputEvent.MouseRelease mouseRelease ->
                    handleRelease(root, mouseRelease.positionX(), mouseRelease.positionY(), mouseRelease.button());
            case InputEvent.MouseScroll mouseScroll ->
                    handleScroll(root, mouseScroll.positionX(), mouseScroll.positionY(), mouseScroll.delta());
            case InputEvent.KeyPress keyPress -> handleKey(root, keyPress, focusedId);
            case InputEvent.CharType charType -> handleChar(root, charType, focusedId);
        };
    }

    public UiPointerCursor pointerCursorAt(FWidget root, float pointerX, float pointerY) {
        if (root == null) {
            return UiPointerCursor.DEFAULT;
        }
        FWidget leaf = hitLeaf(root, pointerX, pointerY);
        if (leaf == null) {
            return UiPointerCursor.DEFAULT;
        }
        return leaf.pointerCursor(pointerX, pointerY);
    }

    private boolean handleMove(FWidget root, float pointerX, float pointerY) {
        if (pressedOn != null) {
            return walkBubbleMouseMove(pressedOn, pointerX, pointerY);
        }
        FWidget next = hitLeaf(root, pointerX, pointerY);
        if (next != hoverLeaf) {
            if (hoverLeaf != null) {
                walkBubbleMouseLeave(hoverLeaf, pointerX, pointerY);
            }
            if (next != null) {
                walkBubbleMouseEnter(next, pointerX, pointerY);
            }
            hoverLeaf = next;
        }
        if (next == null) {
            return false;
        }
        return walkBubbleMouseMove(next, pointerX, pointerY);
    }

    private boolean handlePress(FWidget root, float pointerX, float pointerY, int button, IntConsumer setFocusedId) {
        clickSoundPlayedOnPress = false;
        FWidget leaf = hitLeaf(root, pointerX, pointerY);
        pressedOn = leaf;
        if (leaf == null) {
            setFocusedId.accept(UiFocusIds.NO_FOCUS_ID);
            return false;
        }
        int focusId = leaf.focusId();
        setFocusedId.accept(focusId);
        boolean mouseDownConsumed = walkMouseDown(leaf, pointerX, pointerY, button);
        if (button == 0 && mouseDownConsumed) {
            UiSounds.playButtonClick();
            clickSoundPlayedOnPress = true;
        }
        return mouseDownConsumed;
    }

    private boolean handleRelease(FWidget root, float pointerX, float pointerY, int button) {
        boolean consumed = false;
        FWidget upTarget = pressedOn;
        if (upTarget == null) {
            upTarget = hitLeaf(root, pointerX, pointerY);
        }
        if (upTarget != null) {
            consumed = walkMouseUp(upTarget, pointerX, pointerY, button);
        }
        FWidget clickTarget = pressedOn;
        if (button == 0 && clickTarget != null) {
            boolean clickConsumed = walkClick(clickTarget, pointerX, pointerY, button);
            consumed |= clickConsumed;
            if (clickConsumed && !clickSoundPlayedOnPress) {
                UiSounds.playButtonClick();
            }
        }
        pressedOn = null;
        clickSoundPlayedOnPress = false;
        return consumed;
    }

    private boolean handleScroll(FWidget root, float pointerX, float pointerY, float delta) {
        FWidget leaf = hitLeaf(root, pointerX, pointerY);
        if (leaf == null) {
            return false;
        }
        FWidget scrollHost = findScrollHost(leaf);
        return scrollHost instanceof FScrollable scrollable && scrollable.applyScroll(delta);
    }

    private boolean handleKey(FWidget root, InputEvent.KeyPress keyPress, int focusedId) {
        FWidget target = findFocusElement(root, focusedId);
        if (target == null) {
            return handleKeyUnfocused(root, keyPress);
        }
        return walkKeyDown(target, keyPress.keyCode(), keyPress.modifiers());
    }

    private boolean handleKeyUnfocused(FWidget node, InputEvent.KeyPress keyPress) {
        if (node == null || !node.visible()) {
            return false;
        }
        List<FWidget> childList = node.childrenView();
        for (int childIndex = childList.size() - 1; childIndex >= 0; childIndex--) {
            if (handleKeyUnfocused(childList.get(childIndex), keyPress)) {
                return true;
            }
        }
        return node.keyDownUnfocused(keyPress.keyCode(), keyPress.modifiers());
    }

    private boolean handleChar(FWidget root, InputEvent.CharType charType, int focusedId) {
        FWidget target = findFocusElement(root, focusedId);
        if (target == null) {
            return false;
        }
        return walkCharTyped(target, charType.character());
    }
}
