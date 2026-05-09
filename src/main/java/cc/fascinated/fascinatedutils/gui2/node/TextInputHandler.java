package cc.fascinated.fascinatedutils.gui2.node;

import cc.fascinated.fascinatedutils.gui2.core.UiState;
import lombok.Getter;
import lombok.Setter;
import org.lwjgl.glfw.GLFW;

import java.util.function.Consumer;

public class TextInputHandler {

    private final StringBuilder buffer = new StringBuilder();
    private int caretIndex;
    private int selectionAnchor = -1;
    @Getter
    @Setter
    private boolean focused;
    @Getter
    private boolean mouseSelecting;
    private int maxLength = 2000;
    private Consumer<String> onSubmit = ignored -> {};
    private Consumer<String> onChange = ignored -> {};
    private Runnable onCancel = () -> {};
    private Runnable onBufferMutated = () -> {};

    // Persistent state — bound by caller so values survive recompose
    private UiState<Integer> caretState;
    private UiState<Integer> selectionState;
    private UiState<Boolean> dragState;

    public TextInputHandler setMaxLength(int maxLength) {
        this.maxLength = Math.max(0, maxLength);
        return this;
    }

    public int maxLength() {
        return maxLength;
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
        this.onCancel = onCancel == null ? () -> {} : onCancel;
        return this;
    }

    /**
     * Registers a callback invoked after every buffer mutation, before {@code onChange}.
     *
     * <p>Use this to invalidate per-node caches (e.g. wrapped-line layout) without wrapping
     * the public {@code onChange} callback.
     *
     * @param callback invoked after every buffer mutation
     * @return this
     */
    public TextInputHandler setOnBufferMutated(Runnable callback) {
        this.onBufferMutated = callback == null ? () -> {} : callback;
        return this;
    }

    public void setValue(String value) {
        buffer.setLength(0);
        if (value != null) {
            buffer.append(value);
        }
        caretIndex = buffer.length();
    }

    public String value() {
        return buffer.toString();
    }

    public void clear() {
        buffer.setLength(0);
        caretIndex = 0;
        selectionAnchor = -1;
        mouseSelecting = false;
        if (caretState != null) caretState.set(0);
        if (selectionState != null) selectionState.set(-1);
        if (dragState != null) dragState.set(false);
    }

    public void bindCaretState(UiState<Integer> state) {
        this.caretState = state;
        this.caretIndex = Math.max(0, Math.min(state.get(), buffer.length()));
    }

    public void bindSelectionState(UiState<Integer> state) {
        this.selectionState = state;
        this.selectionAnchor = state.get();
    }

    public void bindDragState(UiState<Boolean> state) {
        this.dragState = state;
        this.mouseSelecting = Boolean.TRUE.equals(state.get());
    }

    public StringBuilder buffer() {
        return buffer;
    }

    public int length() {
        return buffer.length();
    }

    public int caretIndex() {
        return caretIndex;
    }

    public int selectionAnchor() {
        return selectionAnchor;
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
        caretIndex = charIndex;
        selectionAnchor = charIndex;
        mouseSelecting = true;
        persistCaret();
        persistSelection();
        if (dragState != null) dragState.set(true);
    }

    /**
     * Updates the drag caret to the given character index.
     *
     * @param charIndex the character index under the pointer
     */
    public void updateDrag(int charIndex) {
        if (charIndex != caretIndex) {
            caretIndex = charIndex;
            persistCaret();
        }
    }

    /**
     * Ends the mouse-drag selection. Collapses the selection if the pointer did not move.
     */
    public void endDrag() {
        mouseSelecting = false;
        if (dragState != null) dragState.set(false);
        if (selectionAnchor == caretIndex) {
            selectionAnchor = -1;
            persistSelection();
        }
    }

    /**
     * Moves the caret to {@code newIndex}, clamped to buffer bounds.
     * When {@code extending} is {@code true} the selection anchor is fixed (starting a new
     * selection at the current caret if none exists); otherwise any existing selection is cleared.
     *
     * @param newIndex  destination caret index
     * @param extending {@code true} to extend the selection, {@code false} to collapse it
     */
    public void moveCaret(int newIndex, boolean extending) {
        newIndex = Math.max(0, Math.min(newIndex, buffer.length()));
        if (extending) {
            if (selectionAnchor == -1) {
                selectionAnchor = caretIndex;
                persistSelection();
            }
            caretIndex = newIndex;
            persistCaret();
        } else {
            caretIndex = newIndex;
            clearSelection();
            persistCaret();
        }
    }

    /**
     * Returns the index of the start of the word to the left of {@code pos}.
     *
     * @param pos position to search from
     * @return word-start index
     */
    public int wordLeft(int pos) {
        if (pos <= 0) {
            return 0;
        }
        int index = pos - 1;
        while (index > 0 && !isWordChar(buffer.charAt(index))) {
            index--;
        }
        while (index > 0 && isWordChar(buffer.charAt(index - 1))) {
            index--;
        }
        return index;
    }

    /**
     * Returns the index just past the end of the word to the right of {@code pos}.
     *
     * @param pos position to search from
     * @return word-end index
     */
    public int wordRight(int pos) {
        int len = buffer.length();
        if (pos >= len) {
            return len;
        }
        int index = pos;
        while (index < len && !isWordChar(buffer.charAt(index))) {
            index++;
        }
        while (index < len && isWordChar(buffer.charAt(index))) {
            index++;
        }
        return index;
    }

    /**
     * Deletes the current selection and repositions the caret.
     *
     * @return {@code true} if a selection was deleted
     */
    public boolean deleteSelection() {
        if (!hasSelection()) {
            return false;
        }
        int start = selectStart();
        int end = selectEnd();
        buffer.delete(start, end);
        caretIndex = start;
        clearSelection();
        persistCaret();
        fireChange();
        return true;
    }

    /**
     * Clears the selection anchor, collapsing any active selection.
     */
    public void clearSelection() {
        selectionAnchor = -1;
        persistSelection();
    }

    /**
     * Handles Ctrl+key shortcuts common to all text inputs: A, C, X, V, Left, Right,
     * Backspace, Delete.
     *
     * <p>Home and End are intentionally excluded — their behaviour differs between
     * single-line (buffer bounds) and multi-line (visual-line bounds) inputs.
     *
     * @param keyCode              GLFW key code
     * @param modifiers            GLFW modifier mask (must include CTRL)
     * @param allowNewlinesInPaste whether pasted text may contain newlines
     * @return {@code true} if the key was consumed
     */
    public boolean handleCtrlKeyPress(int keyCode, int modifiers, boolean allowNewlinesInPaste) {
        boolean shift = (modifiers & GLFW.GLFW_MOD_SHIFT) != 0;
        switch (keyCode) {
            case GLFW.GLFW_KEY_A -> {
                selectionAnchor = 0;
                caretIndex = buffer.length();
                persistCaret();
                persistSelection();
                return true;
            }
            case GLFW.GLFW_KEY_C -> {
                if (hasSelection()) {
                    GLFW.glfwSetClipboardString(0L, buffer.substring(selectStart(), selectEnd()));
                }
                return true;
            }
            case GLFW.GLFW_KEY_X -> {
                if (hasSelection()) {
                    GLFW.glfwSetClipboardString(0L, buffer.substring(selectStart(), selectEnd()));
                    deleteSelection();
                }
                return true;
            }
            case GLFW.GLFW_KEY_V -> {
                String clip = GLFW.glfwGetClipboardString(0L);
                if (clip != null && !clip.isEmpty()) {
                    deleteSelection();
                    StringBuilder filtered = new StringBuilder();
                    for (char character : clip.toCharArray()) {
                        if (character >= 32 || (allowNewlinesInPaste && character == '\n')) {
                            filtered.append(character);
                        }
                    }
                    String insert = filtered.toString();
                    int available = maxLength - buffer.length();
                    if (available > 0) {
                        if (insert.length() > available) {
                            insert = insert.substring(0, available);
                        }
                        buffer.insert(caretIndex, insert);
                        caretIndex += insert.length();
                        persistCaret();
                        fireChange();
                    }
                }
                return true;
            }
            case GLFW.GLFW_KEY_LEFT -> {
                int target = wordLeft(shift && hasSelection() ? selectStart() : caretIndex);
                moveCaret(target, shift);
                return true;
            }
            case GLFW.GLFW_KEY_RIGHT -> {
                int target = wordRight(shift && hasSelection() ? selectEnd() : caretIndex);
                moveCaret(target, shift);
                return true;
            }
            case GLFW.GLFW_KEY_BACKSPACE -> {
                if (deleteSelection()) {
                    return true;
                }
                if (caretIndex > 0) {
                    int target = wordLeft(caretIndex);
                    buffer.delete(target, caretIndex);
                    caretIndex = target;
                    persistCaret();
                    fireChange();
                }
                return true;
            }
            case GLFW.GLFW_KEY_DELETE -> {
                if (deleteSelection()) {
                    return true;
                }
                if (caretIndex < buffer.length()) {
                    int target = wordRight(caretIndex);
                    buffer.delete(caretIndex, target);
                    persistCaret();
                    fireChange();
                }
                return true;
            }
        }
        return false;
    }

    /**
     * Handles non-Ctrl key presses shared by all text inputs: Escape, Backspace, Delete,
     * Left arrow, Right arrow.
     *
     * <p>Nodes must handle Enter, Home, End, and (for multi-line inputs) Up and Down themselves.
     *
     * @param keyCode   GLFW key code
     * @param modifiers GLFW modifier mask
     * @return {@code true} if the key was consumed
     */
    public boolean handleBasicKeyPress(int keyCode, int modifiers) {
        boolean shift = (modifiers & GLFW.GLFW_MOD_SHIFT) != 0;
        switch (keyCode) {
            case GLFW.GLFW_KEY_ESCAPE -> {
                onCancel.run();
                return true;
            }
            case GLFW.GLFW_KEY_BACKSPACE -> {
                if (deleteSelection()) {
                    return true;
                }
                if (caretIndex > 0) {
                    buffer.deleteCharAt(caretIndex - 1);
                    caretIndex--;
                    persistCaret();
                    fireChange();
                }
                return true;
            }
            case GLFW.GLFW_KEY_DELETE -> {
                if (deleteSelection()) {
                    return true;
                }
                if (caretIndex < buffer.length()) {
                    buffer.deleteCharAt(caretIndex);
                    persistCaret();
                    fireChange();
                }
                return true;
            }
            case GLFW.GLFW_KEY_LEFT -> {
                if (!shift && hasSelection()) {
                    caretIndex = selectStart();
                    clearSelection();
                    persistCaret();
                } else {
                    moveCaret(caretIndex - 1, shift);
                }
                return true;
            }
            case GLFW.GLFW_KEY_RIGHT -> {
                if (!shift && hasSelection()) {
                    caretIndex = selectEnd();
                    clearSelection();
                    persistCaret();
                } else {
                    moveCaret(caretIndex + 1, shift);
                }
                return true;
            }
        }
        return false;
    }

    /**
     * Handles a typed character: deletes any selection, then inserts the character if within
     * the {@link #maxLength}.
     *
     * @param character the typed character
     * @return {@code true} always (the character is consumed regardless of insertion)
     */
    public boolean handleCharType(char character) {
        deleteSelection();
        if (buffer.length() >= maxLength) {
            return true;
        }
        buffer.insert(caretIndex, character);
        caretIndex++;
        persistCaret();
        fireChange();
        return true;
    }

    /**
     * Fires the submit callback with the current buffer contents.
     */
    public void submit() {
        onSubmit.accept(buffer.toString());
    }

    private void fireChange() {
        onBufferMutated.run();
        onChange.accept(buffer.toString());
    }

    private static boolean isWordChar(char character) {
        return Character.isLetterOrDigit(character) || character == '_';
    }

    private void persistCaret() {
        if (caretState != null) caretState.set(caretIndex);
    }

    private void persistSelection() {
        if (selectionState != null) selectionState.set(selectionAnchor);
    }
}
