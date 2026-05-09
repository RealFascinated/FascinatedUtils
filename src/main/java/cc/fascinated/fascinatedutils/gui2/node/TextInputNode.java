package cc.fascinated.fascinatedutils.gui2.node;

import cc.fascinated.fascinatedutils.gui2.core.PositionedNode;
import cc.fascinated.fascinatedutils.gui2.core.UiState;
import cc.fascinated.fascinatedutils.gui2.render.ClipRegion;
import cc.fascinated.fascinatedutils.gui2.render.RenderFrame;
import org.lwjgl.glfw.GLFW;

import java.util.function.Consumer;

/**
 * Single-line text input node with full HTML-like editing behaviour.
 *
 * <p>Supports caret navigation, selection (keyboard and mouse drag), clipboard operations,
 * word-boundary movement, and scroll-into-view. Because the owning composer rebuilds this
 * node every frame, callers must bind persistent {@link UiState} handles via
 * {@link #bindCaretState}, {@link #bindSelectionState}, and {@link #bindDragState} so
 * caret and selection survive across recompose cycles.
 */
public class TextInputNode extends PositionedNode {
    private static final int PADDING_V = 6;
    private static final int PADDING_H = 10;
    private static final int CORNER_RADIUS = 4;
    private static final long CARET_BLINK_HALF_PERIOD_MS = 530L;
    private static final float CARET_THICKNESS = 0.75f;
    private static final int CARET_VERTICAL_INSET = 1;
    private static final int SELECTION_COLOR = 0x55aaccff;

    private final StringBuilder buffer = new StringBuilder();
    private int caretIndex;
    private int selectionAnchor = -1;
    private boolean focused;
    private boolean mouseSelecting;
    private String placeholder = "";
    private int maxLength = 2000;
    private Consumer<String> onSubmit = ignored -> {};
    private Consumer<String> onChange = ignored -> {};
    private Runnable onCancel = () -> {};

    // Persistent state — must be bound by the caller so values survive recompose
    private UiState<Integer> caretState;
    private UiState<Integer> selectionState;
    private UiState<Boolean> dragState;

    // Cached during renderSelf so pointer-event handlers can hit-test without a RenderFrame
    private RenderFrame lastRenderFrame;
    private int lastTextAreaX;
    private int lastScrollOffset;

    public TextInputNode() {
        height(36).fullWidth();
    }

    public TextInputNode setPlaceholder(String placeholder) {
        this.placeholder = placeholder == null ? "" : placeholder;
        return this;
    }

    public TextInputNode setMaxLength(int maxLength) {
        this.maxLength = Math.max(0, maxLength);
        return this;
    }

    public TextInputNode setOnSubmit(Consumer<String> onSubmit) {
        this.onSubmit = onSubmit == null ? ignored -> {} : onSubmit;
        return this;
    }

    public TextInputNode setOnChange(Consumer<String> onChange) {
        this.onChange = onChange == null ? ignored -> {} : onChange;
        return this;
    }

    public TextInputNode setOnCancel(Runnable onCancel) {
        this.onCancel = onCancel == null ? () -> {} : onCancel;
        return this;
    }

    public TextInputNode setValue(String value) {
        buffer.setLength(0);
        if (value != null) {
            buffer.append(value);
        }
        caretIndex = buffer.length();
        return this;
    }

    /**
     * Binds an external {@link UiState} to persist the caret index across recompose cycles.
     * Call this after {@link #setValue(String)}.
     *
     * @param state persistent state for the caret index
     * @return this
     */
    public TextInputNode bindCaretState(UiState<Integer> state) {
        this.caretState = state;
        this.caretIndex = Math.max(0, Math.min(state.get(), buffer.length()));
        return this;
    }

    /**
     * Binds an external {@link UiState} to persist the selection anchor across recompose cycles.
     * Call this after {@link #setValue(String)}.
     *
     * @param state persistent state for the selection anchor ({@code -1} means no selection)
     * @return this
     */
    public TextInputNode bindSelectionState(UiState<Integer> state) {
        this.selectionState = state;
        this.selectionAnchor = state.get();
        return this;
    }

    /**
     * Binds an external {@link UiState} to persist whether a mouse-drag selection is active.
     * Required for drag-selection to survive between recompose cycles.
     *
     * @param state persistent drag state
     * @return this
     */
    public TextInputNode bindDragState(UiState<Boolean> state) {
        this.dragState = state;
        this.mouseSelecting = Boolean.TRUE.equals(state.get());
        return this;
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

    // -------------------------------------------------------------------------
    // Focus
    // -------------------------------------------------------------------------

    @Override
    public boolean focusable() {
        return true;
    }

    @Override
    public boolean blocksHitWhenEmpty() {
        return true;
    }

    @Override
    public void onFocusGained() {
        focused = true;
    }

    @Override
    public void onFocusLost() {
        focused = false;
    }

    // -------------------------------------------------------------------------
    // Pointer input — click-to-position and drag selection
    // -------------------------------------------------------------------------

    @Override
    public boolean onPointerPress(float pointerX, float pointerY, int button) {
        if (button != 0 || !contains(pointerX, pointerY)) {
            return false;
        }
        int index = charIndexAtX(pointerX);
        caretIndex = index;
        selectionAnchor = index;
        mouseSelecting = true;
        persistCaret();
        persistSelection();
        if (dragState != null) dragState.set(true);
        return true;
    }

    @Override
    public boolean onPointerMove(float pointerX, float pointerY) {
        if (!mouseSelecting) {
            return false;
        }
        int index = charIndexAtX(pointerX);
        if (index != caretIndex) {
            caretIndex = index;
            persistCaret();
        }
        return true;
    }

    @Override
    public boolean onPointerRelease(float pointerX, float pointerY, int button) {
        if (button != 0 || !mouseSelecting) {
            return false;
        }
        mouseSelecting = false;
        if (dragState != null) dragState.set(false);
        // Single click without drag — collapse to caret, no selection
        if (selectionAnchor == caretIndex) {
            selectionAnchor = -1;
            persistSelection();
        }
        return false;
    }

    // -------------------------------------------------------------------------
    // Keyboard input
    // -------------------------------------------------------------------------

    @Override
    public boolean capturesKeyPress(int keyCode, int modifiers) {
        return false;
    }

    @Override
    public boolean onKeyPress(int keyCode, int modifiers) {
        if (!focused) {
            return false;
        }
        boolean ctrl = (modifiers & GLFW.GLFW_MOD_CONTROL) != 0;
        boolean shift = (modifiers & GLFW.GLFW_MOD_SHIFT) != 0;

        if (ctrl) {
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
                            if (character >= 32) {
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
                            onChange.accept(buffer.toString());
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
                        onChange.accept(buffer.toString());
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
                        onChange.accept(buffer.toString());
                    }
                    return true;
                }
            }
            return false;
        }

        switch (keyCode) {
            case GLFW.GLFW_KEY_ESCAPE -> {
                onCancel.run();
                return true;
            }
            case GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER -> {
                if (!shift) {
                    onSubmit.accept(buffer.toString());
                    return true;
                }
            }
            case GLFW.GLFW_KEY_BACKSPACE -> {
                if (deleteSelection()) {
                    return true;
                }
                if (caretIndex > 0) {
                    buffer.deleteCharAt(caretIndex - 1);
                    caretIndex--;
                    persistCaret();
                    onChange.accept(buffer.toString());
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
                    onChange.accept(buffer.toString());
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
            case GLFW.GLFW_KEY_HOME -> {
                moveCaret(0, shift);
                return true;
            }
            case GLFW.GLFW_KEY_END -> {
                moveCaret(buffer.length(), shift);
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean capturesCharType(char character) {
        return false;
    }

    @Override
    public boolean onCharType(char character) {
        if (!focused) {
            return false;
        }
        deleteSelection();
        if (buffer.length() >= maxLength) {
            return true;
        }
        buffer.insert(caretIndex, character);
        caretIndex++;
        persistCaret();
        onChange.accept(buffer.toString());
        return true;
    }

    // -------------------------------------------------------------------------
    // Rendering
    // -------------------------------------------------------------------------

    @Override
    protected void renderSelf(RenderFrame renderFrame, float deltaSeconds) {
        int bx = bounds().positionX();
        int by = bounds().positionY();
        int bw = bounds().width();
        int bh = bounds().height();

        int borderColor = focused ? renderFrame.theme().accent() : renderFrame.theme().inputBorder();
        renderFrame.drawRoundedRect(bx, by, bw, bh, CORNER_RADIUS, renderFrame.theme().inputFill());
        renderFrame.drawRoundedRectFrame(bx, by, bw, bh, CORNER_RADIUS, borderColor, 0, 1);

        int textAreaX = bx + PADDING_H;
        int textY = by + (bh - renderFrame.fontHeight()) / 2;
        int textAreaWidth = bw - PADDING_H * 2;

        String text = buffer.toString();
        if (text.isBlank() && !focused) {
            renderFrame.drawText(placeholder, textAreaX, textY, renderFrame.theme().textMuted(), false, false);
            return;
        }
        if (text.isBlank()) {
            renderFrame.drawText(placeholder, textAreaX, textY, renderFrame.theme().inputPlaceholderFocused(), false, false);
        }

        int caretPositionX = renderFrame.measureTextWidth(text.substring(0, caretIndex), false);
        int scrollOffset = Math.max(0, caretPositionX - textAreaWidth);

        // Cache for pointer hit-testing
        lastRenderFrame = renderFrame;
        lastTextAreaX = textAreaX;
        lastScrollOffset = scrollOffset;

        renderFrame.pushClip(new ClipRegion(textAreaX, by + PADDING_V, textAreaWidth, bh - PADDING_V * 2));

        if (focused && hasSelection()) {
            int selStartPx = renderFrame.measureTextWidth(text.substring(0, selectStart()), false);
            int selEndPx = renderFrame.measureTextWidth(text.substring(0, selectEnd()), false);
            int drawStart = Math.max(textAreaX + selStartPx - scrollOffset, textAreaX);
            int drawEnd = Math.min(textAreaX + selEndPx - scrollOffset, textAreaX + textAreaWidth);
            if (drawEnd > drawStart) {
                renderFrame.drawRect(drawStart, textY - CARET_VERTICAL_INSET, drawEnd - drawStart, renderFrame.fontHeight() + CARET_VERTICAL_INSET * 2, SELECTION_COLOR);
            }
        }

        renderFrame.drawText(text, textAreaX - scrollOffset, textY, renderFrame.theme().textPrimary(), false, false);
        renderFrame.popClip();

        if (focused && !hasSelection() && (System.currentTimeMillis() / CARET_BLINK_HALF_PERIOD_MS & 1L) == 0L) {
            int cursorX = textAreaX + caretPositionX - scrollOffset;
            renderFrame.drawLine(
                    cursorX,
                    by + PADDING_V + CARET_VERTICAL_INSET,
                    cursorX,
                    by + bh - PADDING_V - CARET_VERTICAL_INSET,
                    CARET_THICKNESS,
                    renderFrame.theme().caret());
        }
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private int selectStart() {
        return Math.min(caretIndex, selectionAnchor);
    }

    private int selectEnd() {
        return Math.max(caretIndex, selectionAnchor);
    }

    private boolean hasSelection() {
        return selectionAnchor != -1 && selectionAnchor != caretIndex;
    }

    private boolean deleteSelection() {
        if (!hasSelection()) {
            return false;
        }
        int start = selectStart();
        int end = selectEnd();
        buffer.delete(start, end);
        caretIndex = start;
        clearSelection();
        persistCaret();
        onChange.accept(buffer.toString());
        return true;
    }

    private void clearSelection() {
        selectionAnchor = -1;
        persistSelection();
    }

    /**
     * Moves the caret to {@code newIndex}, clamped to buffer bounds.
     * When {@code extending} is {@code true} the selection anchor is fixed (starting a new
     * selection at the current caret if none exists); otherwise any existing selection is cleared.
     */
    private void moveCaret(int newIndex, boolean extending) {
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

    /** Returns the index of the start of the word to the left of {@code pos}. */
    private int wordLeft(int pos) {
        if (pos <= 0) {
            return 0;
        }
        int index = pos - 1;
        // Skip non-word chars first
        while (index > 0 && !isWordChar(buffer.charAt(index))) {
            index--;
        }
        // Then skip word chars
        while (index > 0 && isWordChar(buffer.charAt(index - 1))) {
            index--;
        }
        return index;
    }

    /** Returns the index just past the end of the word to the right of {@code pos}. */
    private int wordRight(int pos) {
        int len = buffer.length();
        if (pos >= len) {
            return len;
        }
        int index = pos;
        // Skip non-word chars first
        while (index < len && !isWordChar(buffer.charAt(index))) {
            index++;
        }
        // Then skip word chars
        while (index < len && isWordChar(buffer.charAt(index))) {
            index++;
        }
        return index;
    }

    private static boolean isWordChar(char character) {
        return Character.isLetterOrDigit(character) || character == '_';
    }

    /**
     * Returns the character index whose left edge is closest to the given pointer X position,
     * using render measurements cached from the last frame.
     */
    private int charIndexAtX(float pointerX) {
        if (lastRenderFrame == null) {
            return caretIndex;
        }
        String text = buffer.toString();
        float localX = pointerX - lastTextAreaX + lastScrollOffset;
        int best = 0;
        float bestDist = Float.MAX_VALUE;
        for (int index = 0; index <= text.length(); index++) {
            float charX = lastRenderFrame.measureTextWidth(text.substring(0, index), false);
            float dist = Math.abs(charX - localX);
            if (dist < bestDist) {
                bestDist = dist;
                best = index;
            }
        }
        return best;
    }

    private void persistCaret() {
        if (caretState != null) caretState.set(caretIndex);
    }

    private void persistSelection() {
        if (selectionState != null) selectionState.set(selectionAnchor);
    }
}