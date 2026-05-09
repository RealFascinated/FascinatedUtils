package cc.fascinated.fascinatedutils.gui2.node;

import cc.fascinated.fascinatedutils.gui2.core.PositionedNode;
import cc.fascinated.fascinatedutils.gui2.core.UiState;
import cc.fascinated.fascinatedutils.gui2.render.ClipRegion;
import cc.fascinated.fascinatedutils.gui2.render.RenderFrame;
import cc.fascinated.fascinatedutils.gui2.render.UiText;
import org.lwjgl.glfw.GLFW;

import java.util.function.Consumer;

public class TextInputNode extends PositionedNode {
    private static final int PADDING_V = 6;
    private static final int PADDING_H = 10;
    private static final int CORNER_RADIUS = 4;
    private static final long CARET_BLINK_HALF_PERIOD_MS = 530L;
    private static final int CARET_VERTICAL_INSET = 0;
    private static final int SELECTION_COLOR = 0x55aaccff;

    private final TextInputHandler handler = new TextInputHandler();
    private String placeholder = "";

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
        handler.setMaxLength(maxLength);
        return this;
    }

    public TextInputNode setOnSubmit(Consumer<String> onSubmit) {
        handler.setOnSubmit(onSubmit);
        return this;
    }

    public TextInputNode setOnChange(Consumer<String> onChange) {
        handler.setOnChange(onChange);
        return this;
    }

    public TextInputNode setOnCancel(Runnable onCancel) {
        handler.setOnCancel(onCancel);
        return this;
    }

    public TextInputNode setValue(String value) {
        handler.setValue(value);
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
        handler.bindCaretState(state);
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
        handler.bindSelectionState(state);
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
        handler.bindDragState(state);
        return this;
    }

    public String value() {
        return handler.value();
    }

    public void clear() {
        handler.clear();
    }

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
        handler.setFocused(true);
    }

    @Override
    public void onFocusLost() {
        handler.setFocused(false);
    }

    @Override
    public boolean onPointerPress(float pointerX, float pointerY, int button) {
        if (button != 0 || !contains(pointerX, pointerY)) {
            return false;
        }
        handler.beginDrag(charIndexAtX(pointerX));
        return true;
    }

    @Override
    public boolean onPointerMove(float pointerX, float pointerY) {
        if (!handler.isMouseSelecting()) {
            return false;
        }
        handler.updateDrag(charIndexAtX(pointerX));
        return true;
    }

    @Override
    public boolean onPointerRelease(float pointerX, float pointerY, int button) {
        if (button != 0 || !handler.isMouseSelecting()) {
            return false;
        }
        handler.endDrag();
        return false;
    }

    @Override
    public boolean capturesKeyPress(int keyCode, int modifiers) {
        return false;
    }

    @Override
    public boolean onKeyPress(int keyCode, int modifiers) {
        if (!handler.isFocused()) {
            return false;
        }
        boolean ctrl = (modifiers & GLFW.GLFW_MOD_CONTROL) != 0;
        boolean shift = (modifiers & GLFW.GLFW_MOD_SHIFT) != 0;

        if (ctrl) {
            return handler.handleCtrlKeyPress(keyCode, modifiers, false);
        }

        switch (keyCode) {
            case GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER -> {
                if (!shift) {
                    handler.submit();
                    return true;
                }
            }
            case GLFW.GLFW_KEY_HOME -> {
                handler.moveCaret(0, shift);
                return true;
            }
            case GLFW.GLFW_KEY_END -> {
                handler.moveCaret(handler.length(), shift);
                return true;
            }
        }
        return handler.handleBasicKeyPress(keyCode, modifiers);
    }

    @Override
    public boolean capturesCharType(char character) {
        return false;
    }

    @Override
    public boolean onCharType(char character) {
        if (!handler.isFocused()) {
            return false;
        }
        return handler.handleCharType(character);
    }

    @Override
    protected void renderSelf(RenderFrame renderFrame, float deltaSeconds) {
        int bx = bounds().positionX();
        int by = bounds().positionY();
        int bw = bounds().width();
        int bh = bounds().height();

        int borderColor = handler.isFocused() ? renderFrame.theme().accent() : renderFrame.theme().inputBorder();
        renderFrame.drawRoundedRect(bx, by, bw, bh, CORNER_RADIUS, renderFrame.theme().inputFill());
        renderFrame.drawRoundedRectFrame(bx, by, bw, bh, CORNER_RADIUS, borderColor, 0, 1);

        int textAreaX = bx + PADDING_H;
        int textY = by + (bh - renderFrame.fontHeight()) / 2;
        int textAreaWidth = bw - PADDING_H * 2;

        String text = handler.buffer().toString();
        if (text.isBlank() && !handler.isFocused()) {
            UiText.of(placeholder).color(renderFrame.theme().textMuted()).draw(renderFrame, textAreaX, textY);
            return;
        }
        if (text.isBlank()) {
            UiText.of(placeholder).color(renderFrame.theme().inputPlaceholderFocused()).draw(renderFrame, textAreaX, textY);
        }

        int caretPositionX = renderFrame.measureTextWidth(text.substring(0, handler.caretIndex()), false);
        int scrollOffset = Math.max(0, caretPositionX - textAreaWidth);

        // Cache for pointer hit-testing
        lastRenderFrame = renderFrame;
        lastTextAreaX = textAreaX;
        lastScrollOffset = scrollOffset;

        renderFrame.pushClip(new ClipRegion(textAreaX, by + PADDING_V, textAreaWidth, bh - PADDING_V * 2));

        if (handler.isFocused() && handler.hasSelection()) {
            int selStartPx = renderFrame.measureTextWidth(text.substring(0, handler.selectStart()), false);
            int selEndPx = renderFrame.measureTextWidth(text.substring(0, handler.selectEnd()), false);
            int drawStart = Math.max(textAreaX + selStartPx - scrollOffset, textAreaX);
            int drawEnd = Math.min(textAreaX + selEndPx - scrollOffset, textAreaX + textAreaWidth);
            if (drawEnd > drawStart) {
                renderFrame.drawRect(drawStart, textY - CARET_VERTICAL_INSET, drawEnd - drawStart, renderFrame.fontHeight() + CARET_VERTICAL_INSET * 2, SELECTION_COLOR);
            }
        }

        UiText.of(text).color(renderFrame.theme().textPrimary()).draw(renderFrame, textAreaX - scrollOffset, textY);
        renderFrame.flushText();
        renderFrame.popClip();

        if (handler.isFocused() && !handler.hasSelection() && (System.currentTimeMillis() / CARET_BLINK_HALF_PERIOD_MS & 1L) == 0L) {
            int cursorX = textAreaX + caretPositionX - scrollOffset;
            int caretTop = by + PADDING_V + CARET_VERTICAL_INSET;
            int caretBottom = by + bh - PADDING_V - CARET_VERTICAL_INSET;
            renderFrame.drawRect(cursorX, caretTop, 1, caretBottom - caretTop, renderFrame.theme().caret());
        }
    }

    /**
     * Returns the character index whose left edge is closest to the given pointer X position,
     * using render measurements cached from the last frame.
     */
    private int charIndexAtX(float pointerX) {
        if (lastRenderFrame == null) {
            return handler.caretIndex();
        }
        String text = handler.buffer().toString();
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
}