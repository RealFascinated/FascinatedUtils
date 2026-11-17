package cc.fascinated.fascinatedutils.gui.node.input;

import lombok.Getter;
import lombok.experimental.Accessors;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayDeque;
import java.util.function.Consumer;

/**
 * Editing state and behaviour of a single-line text field: buffer, caret, selection, undo history and the keyboard
 * shortcuts shared by every text input. Positions are UTF-16 indices that always sit on code point boundaries.
 */
@Accessors(fluent = true)
public class TextInputHandler {

    private record TextSnapshot(String text, int caret) {}

    private static final int MAX_UNDO_DEPTH = 200;

    @Getter
    private final StringBuilder buffer = new StringBuilder();
    @Getter
    private int caretIndex;
    @Getter
    private int selectionAnchor = -1;
    @Getter
    private boolean focused;
    @Getter
    private boolean mouseSelecting;
    @Getter
    private int maxLength = 2000;
    private final ArrayDeque<TextSnapshot> undoStack = new ArrayDeque<>();
    private final ArrayDeque<TextSnapshot> redoStack = new ArrayDeque<>();
    private boolean inTypingRun;
    private Consumer<String> onSubmit = ignored -> {};
    private Consumer<String> onChange = ignored -> {};
    private Runnable onCancel;

    /**
     * Sets the most UTF-16 units the text may hold and shortens the current text to fit.
     *
     * @param maxLength the limit, at least zero
     * @return this handler
     */
    public TextInputHandler setMaxLength(int maxLength) {
        this.maxLength = Math.max(0, maxLength);
        if (buffer.length() > this.maxLength) {
            buffer.setLength(truncationIndex(buffer, this.maxLength));
            caretIndex = Math.min(caretIndex, buffer.length());
            selectionAnchor = Math.min(selectionAnchor, buffer.length());
        }
        return this;
    }

    public TextInputHandler setOnSubmit(Consumer<String> onSubmit) {
        this.onSubmit = onSubmit == null ? ignored -> {} : onSubmit;
        return this;
    }

    public TextInputHandler setOnChange(Consumer<String> onChange) {
        this.onChange = onChange == null ? ignored -> {} : onChange;
        return this;
    }

    public TextInputHandler setOnCancel(Runnable onCancel) {
        this.onCancel = onCancel;
        return this;
    }

    public void setFocused(boolean focused) {
        if (!focused) {
            inTypingRun = false;
        }
        this.focused = focused;
    }

    /**
     * Copies the caret, selection and drag state of the handler this one replaces after recomposition, clamped to the
     * current text.
     *
     * @param previous the handler of the matched node from the previous tree
     */
    public void inheritInteractionFrom(TextInputHandler previous) {
        caretIndex = Math.min(previous.caretIndex, buffer.length());
        selectionAnchor = previous.selectionAnchor < 0 ? -1 : Math.min(previous.selectionAnchor, buffer.length());
        mouseSelecting = previous.mouseSelecting;
        inTypingRun = previous.inTypingRun;
    }

    /**
     * Copies the text and undo history of the handler this one replaces after recomposition, so typing that the
     * composed value does not reflect (for example a number that does not parse yet) survives.
     *
     * @param previous the handler of the matched node from the previous tree
     */
    public void inheritTextFrom(TextInputHandler previous) {
        buffer.setLength(0);
        buffer.append(previous.buffer);
        undoStack.clear();
        undoStack.addAll(previous.undoStack);
        redoStack.clear();
        redoStack.addAll(previous.redoStack);
    }

    /**
     * Replaces the text, shortened to the maximum length, and forgets the undo history. Does not fire the change
     * callback.
     *
     * @param value the new text; null means empty
     */
    public void setValue(String value) {
        inTypingRun = false;
        undoStack.clear();
        redoStack.clear();
        buffer.setLength(0);
        if (value != null) {
            buffer.append(value, 0, truncationIndex(value, maxLength));
        }
        caretIndex = buffer.length();
        selectionAnchor = -1;
        mouseSelecting = false;
    }

    public boolean undo() {
        inTypingRun = false;
        if (undoStack.isEmpty()) {
            return false;
        }
        redoStack.push(new TextSnapshot(buffer.toString(), caretIndex));
        applySnapshot(undoStack.pop());
        return true;
    }

    public boolean redo() {
        inTypingRun = false;
        if (redoStack.isEmpty()) {
            return false;
        }
        undoStack.push(new TextSnapshot(buffer.toString(), caretIndex));
        applySnapshot(redoStack.pop());
        return true;
    }

    private void saveUndoSnapshot() {
        String current = buffer.toString();
        if (!undoStack.isEmpty() && undoStack.peek().text().equals(current)) {
            return;
        }
        if (undoStack.size() >= MAX_UNDO_DEPTH) {
            undoStack.removeLast();
        }
        undoStack.push(new TextSnapshot(current, caretIndex));
        redoStack.clear();
    }

    private void applySnapshot(TextSnapshot snapshot) {
        buffer.setLength(0);
        buffer.append(snapshot.text());
        caretIndex = Math.min(snapshot.caret(), buffer.length());
        clearSelection();
        fireChange();
    }

    public String value() {
        return buffer.toString();
    }

    /**
     * Empties the text and the undo history. Does not fire the change callback.
     */
    public void clear() {
        setValue("");
    }

    public int length() {
        return buffer.length();
    }

    public int selectStart() {
        return Math.min(caretIndex, selectionAnchor);
    }

    public int selectEnd() {
        return Math.max(caretIndex, selectionAnchor);
    }

    public boolean hasSelection() {
        return selectionAnchor != -1 && selectionAnchor != caretIndex;
    }

    /**
     * Begins a mouse-drag selection at the given character index.
     *
     * @param charIndex the character index under the pointer
     */
    public void beginDrag(int charIndex) {
        inTypingRun = false;
        caretIndex = charIndex;
        selectionAnchor = charIndex;
        mouseSelecting = true;
    }

    /**
     * Updates the drag caret to the given character index.
     *
     * @param charIndex the character index under the pointer
     */
    public void updateDrag(int charIndex) {
        caretIndex = charIndex;
    }

    /**
     * Ends the mouse-drag selection. Collapses the selection if the pointer did not move.
     */
    public void endDrag() {
        mouseSelecting = false;
        if (selectionAnchor == caretIndex) {
            selectionAnchor = -1;
        }
    }

    /**
     * Moves the caret to {@code newIndex}, clamped to buffer bounds, and ends the current typing run so the next typed
     * character starts a new undo step. When {@code extending} is {@code true} the selection anchor is fixed (starting
     * a new selection at the current caret if none exists); otherwise any existing selection is cleared.
     *
     * @param newIndex  destination caret index
     * @param extending {@code true} to extend the selection, {@code false} to collapse it
     */
    public void moveCaret(int newIndex, boolean extending) {
        inTypingRun = false;
        int clamped = Math.max(0, Math.min(newIndex, buffer.length()));
        if (extending) {
            if (selectionAnchor == -1) {
                selectionAnchor = caretIndex;
            }
            caretIndex = clamped;
        } else {
            caretIndex = clamped;
            clearSelection();
        }
    }

    /**
     * Returns the index of the start of the word to the left of {@code position}.
     *
     * @param position position to search from
     * @return word-start index
     */
    public int wordLeft(int position) {
        int index = position;
        while (index > 0 && !isWordCodePoint(buffer.codePointBefore(index))) {
            index = buffer.offsetByCodePoints(index, -1);
        }
        while (index > 0 && isWordCodePoint(buffer.codePointBefore(index))) {
            index = buffer.offsetByCodePoints(index, -1);
        }
        return index;
    }

    /**
     * Returns the index just past the end of the word to the right of {@code position}.
     *
     * @param position position to search from
     * @return word-end index
     */
    public int wordRight(int position) {
        int length = buffer.length();
        int index = position;
        while (index < length && !isWordCodePoint(buffer.codePointAt(index))) {
            index = buffer.offsetByCodePoints(index, 1);
        }
        while (index < length && isWordCodePoint(buffer.codePointAt(index))) {
            index = buffer.offsetByCodePoints(index, 1);
        }
        return index;
    }

    /**
     * Deletes the current selection and repositions the caret.
     *
     * @return {@code true} if a selection was deleted
     */
    public boolean deleteSelection() {
        if (!removeSelection()) {
            return false;
        }
        fireChange();
        return true;
    }

    private boolean removeSelection() {
        if (!hasSelection()) {
            return false;
        }
        int start = selectStart();
        buffer.delete(start, selectEnd());
        caretIndex = start;
        clearSelection();
        return true;
    }

    /**
     * Clears the selection anchor, collapsing any active selection.
     */
    public void clearSelection() {
        selectionAnchor = -1;
    }

    /**
     * Handles shortcut-modifier key combinations common to all text inputs: undo, redo, select all, copy, cut, paste,
     * word movement and word deletion.
     *
     * <p>Home and End are intentionally excluded; their behaviour differs between single-line (buffer bounds) and
     * multi-line (visual-line bounds) inputs.
     *
     * @param keyCode   GLFW key code
     * @param modifiers GLFW modifier mask
     * @return {@code true} if the key was consumed
     */
    public boolean handleShortcutKeyPress(int keyCode, int modifiers) {
        boolean shift = (modifiers & GLFW.GLFW_MOD_SHIFT) != 0;
        switch (keyCode) {
            case GLFW.GLFW_KEY_Z -> {
                return shift ? redo() : undo();
            }
            case GLFW.GLFW_KEY_Y -> {
                return redo();
            }
            case GLFW.GLFW_KEY_A -> {
                inTypingRun = false;
                selectionAnchor = 0;
                caretIndex = buffer.length();
                return true;
            }
            case GLFW.GLFW_KEY_C -> {
                if (hasSelection()) {
                    Minecraft.getInstance().keyboardHandler.setClipboard(buffer.substring(selectStart(), selectEnd()));
                }
                return true;
            }
            case GLFW.GLFW_KEY_X -> {
                if (hasSelection()) {
                    inTypingRun = false;
                    saveUndoSnapshot();
                    Minecraft.getInstance().keyboardHandler.setClipboard(buffer.substring(selectStart(), selectEnd()));
                    deleteSelection();
                }
                return true;
            }
            case GLFW.GLFW_KEY_V -> {
                paste(Minecraft.getInstance().keyboardHandler.getClipboard());
                return true;
            }
            case GLFW.GLFW_KEY_LEFT -> {
                moveCaret(wordLeft(caretIndex), shift);
                return true;
            }
            case GLFW.GLFW_KEY_RIGHT -> {
                moveCaret(wordRight(caretIndex), shift);
                return true;
            }
            case GLFW.GLFW_KEY_BACKSPACE -> {
                return deleteBackward(true);
            }
            case GLFW.GLFW_KEY_DELETE -> {
                return deleteForward(true);
            }
        }
        return false;
    }

    /**
     * Handles non-shortcut key presses shared by all text inputs: Escape, Backspace, Delete, Left arrow, Right arrow.
     *
     * <p>Nodes must handle Enter, Home and End themselves.
     *
     * @param keyCode   GLFW key code
     * @param modifiers GLFW modifier mask
     * @return {@code true} if the key was consumed
     */
    public boolean handleBasicKeyPress(int keyCode, int modifiers) {
        boolean shift = (modifiers & GLFW.GLFW_MOD_SHIFT) != 0;
        switch (keyCode) {
            case GLFW.GLFW_KEY_ESCAPE -> {
                if (onCancel == null) {
                    return false;
                }
                onCancel.run();
                return true;
            }
            case GLFW.GLFW_KEY_BACKSPACE -> {
                return deleteBackward(false);
            }
            case GLFW.GLFW_KEY_DELETE -> {
                return deleteForward(false);
            }
            case GLFW.GLFW_KEY_LEFT -> {
                if (!shift && hasSelection()) {
                    moveCaret(selectStart(), false);
                } else {
                    moveCaret(caretIndex > 0 ? buffer.offsetByCodePoints(caretIndex, -1) : 0, shift);
                }
                return true;
            }
            case GLFW.GLFW_KEY_RIGHT -> {
                if (!shift && hasSelection()) {
                    moveCaret(selectEnd(), false);
                } else {
                    moveCaret(caretIndex < buffer.length() ? buffer.offsetByCodePoints(caretIndex, 1) : buffer.length(), shift);
                }
                return true;
            }
        }
        return false;
    }

    private boolean deleteBackward(boolean wholeWord) {
        inTypingRun = false;
        if (hasSelection() || caretIndex > 0) {
            saveUndoSnapshot();
        }
        if (deleteSelection()) {
            return true;
        }
        if (caretIndex > 0) {
            int target = wholeWord ? wordLeft(caretIndex) : buffer.offsetByCodePoints(caretIndex, -1);
            buffer.delete(target, caretIndex);
            caretIndex = target;
            fireChange();
        }
        return true;
    }

    private boolean deleteForward(boolean wholeWord) {
        inTypingRun = false;
        if (hasSelection() || caretIndex < buffer.length()) {
            saveUndoSnapshot();
        }
        if (deleteSelection()) {
            return true;
        }
        if (caretIndex < buffer.length()) {
            int target = wholeWord ? wordRight(caretIndex) : buffer.offsetByCodePoints(caretIndex, 1);
            buffer.delete(caretIndex, target);
            fireChange();
        }
        return true;
    }

    /**
     * Replaces the selection with clipboard text: control characters are dropped, line breaks become spaces and the
     * text is cut to the remaining room. Fires a single change.
     */
    private void paste(String clipboard) {
        if (clipboard == null || clipboard.isEmpty()) {
            return;
        }
        inTypingRun = false;
        saveUndoSnapshot();
        removeSelection();
        StringBuilder filtered = new StringBuilder(clipboard.length());
        clipboard.codePoints().forEach(codePoint -> {
            if (codePoint == '\n' || codePoint == '\r') {
                filtered.append(' ');
            } else if (!Character.isISOControl(codePoint)) {
                filtered.appendCodePoint(codePoint);
            }
        });
        int available = maxLength - buffer.length();
        if (available > 0) {
            String insert = filtered.substring(0, truncationIndex(filtered, available));
            buffer.insert(caretIndex, insert);
            caretIndex += insert.length();
        }
        fireChange();
    }

    /**
     * Handles a typed character: deletes any selection, then inserts the character if within the
     * {@link #maxLength()}. Control characters are ignored.
     *
     * @param codePoint the typed Unicode code point
     * @return {@code true} when the character was consumed
     */
    public boolean handleCharType(int codePoint) {
        if (Character.isISOControl(codePoint)) {
            return false;
        }
        if (!inTypingRun) {
            saveUndoSnapshot();
            inTypingRun = true;
        }
        removeSelection();
        char[] typedChars = Character.toChars(codePoint);
        if (buffer.length() + typedChars.length > maxLength) {
            return true;
        }
        buffer.insert(caretIndex, typedChars);
        caretIndex += typedChars.length;
        fireChange();
        return true;
    }

    /**
     * Fires the submit callback with the current buffer contents.
     */
    public void submit() {
        onSubmit.accept(buffer.toString());
    }

    /**
     * Fires the cancel callback.
     */
    public void cancel() {
        if (onCancel != null) {
            onCancel.run();
        }
    }

    private void fireChange() {
        onChange.accept(buffer.toString());
    }

    /**
     * Finds how many leading UTF-16 units of the text fit in a limit without cutting a surrogate pair in half.
     */
    private static int truncationIndex(CharSequence text, int limit) {
        if (text.length() <= limit) {
            return text.length();
        }
        return limit > 0 && Character.isHighSurrogate(text.charAt(limit - 1)) ? limit - 1 : limit;
    }

    private static boolean isWordCodePoint(int codePoint) {
        return Character.isLetterOrDigit(codePoint) || codePoint == '_';
    }
}
