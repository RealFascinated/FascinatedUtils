package cc.fascinated.fascinatedutils.gui2.node.input;

import cc.fascinated.fascinatedutils.gui2.core.PositionedNode;
import cc.fascinated.fascinatedutils.gui2.core.UiState;
import cc.fascinated.fascinatedutils.gui2.render.ClipRegion;
import cc.fascinated.fascinatedutils.gui2.render.RenderFrame;
import cc.fascinated.fascinatedutils.gui2.render.UiText;
import org.lwjgl.glfw.GLFW;

import java.util.function.Consumer;

public class TextInputNode<V> extends PositionedNode<TextInputNode<V>> {

    private static final int PADDING_V = 5;
    private static final int PADDING_H = 8;
    private static final int CORNER_RADIUS = 4;
    private static final long CARET_BLINK_HALF_PERIOD_MS = 530L;
    private static final int SELECTION_COLOR = 0x55aaccff;
    private static final int BORDER_INVALID = 0xFFE05252;

    private final TextParser<V> parser;
    private Consumer<V> typedSubmit = ignored -> {};
    private Consumer<V> typedChange = ignored -> {};
    private final TextInputHandler handler = new TextInputHandler();
    private String placeholder = "";
    private int scrollOffsetX;

    // Layout / render cache
    private RenderFrame lastRenderFrame;
    private int lastTextX;
    private int lastTextY;
    private int lastTextAreaWidth;
    private int lastLineHeight;

    public TextInputNode(TextParser<V> parser) {
        this.parser = parser;
        fullWidth();
        handler.setMaxLength(256);
        handler.setOnSubmit(text -> {
            V parsed = parser.parse(text);
            if (parsed != null) typedSubmit.accept(parsed);
        });
        handler.setOnChange(text -> {
            V parsed = parser.parse(text);
            if (parsed != null) typedChange.accept(parsed);
        });
    }

    public TextInputNode<V> setPlaceholder(String placeholder) {
        this.placeholder = placeholder == null ? "" : placeholder;
        return this;
    }

    public TextInputNode<V> setMaxLength(int maxLength) {
        handler.setMaxLength(maxLength);
        return this;
    }

    public TextInputNode<V> setOnSubmit(Consumer<V> onSubmit) {
        this.typedSubmit = onSubmit == null ? ignored -> {} : onSubmit;
        return this;
    }

    public TextInputNode<V> setOnChange(Consumer<V> onChange) {
        this.typedChange = onChange == null ? ignored -> {} : onChange;
        return this;
    }

    public TextInputNode<V> setOnCancel(Runnable onCancel) {
        handler.setOnCancel(onCancel);
        return this;
    }

    public TextInputNode<V> setValue(V value) {
        handler.setValue(value == null ? "" : parser.format(value));
        return this;
    }

    public V value() {
        return parser.parse(handler.value());
    }

    public String rawValue() {
        return handler.value();
    }

    public void clear() {
        handler.clear();
        scrollOffsetX = 0;
    }

    public TextInputNode<V> bindCaretState(UiState<Integer> state) {
        handler.bindCaretState(state);
        return this;
    }

    public TextInputNode<V> bindSelectionState(UiState<Integer> state) {
        handler.bindSelectionState(state);
        return this;
    }

    public TextInputNode<V> bindDragState(UiState<Boolean> state) {
        handler.bindDragState(state);
        return this;
    }

    public void submit() {
        handler.submit();
    }

    public void cancel() {
        handler.cancel();
    }

    @Override
    public void layout(RenderFrame frame, int parentX, int parentY, int parentWidth, int parentHeight) {
        lastRenderFrame = frame;
        int lineH = frame.fontHeight();
        bounds().set(parentX, parentY, parentWidth, PADDING_V * 2 + lineH);

        int textAreaWidth = Math.max(1, parentWidth - PADDING_H * 2);
        int caretPx = frame.measureTextWidth(handler.buffer().substring(0, handler.caretIndex()), false);
        if (caretPx < scrollOffsetX) {
            scrollOffsetX = caretPx;
        } else if (caretPx > scrollOffsetX + textAreaWidth) {
            scrollOffsetX = caretPx - textAreaWidth;
        }
        scrollOffsetX = Math.max(0, scrollOffsetX);

        lastLineHeight = lineH;
        lastTextAreaWidth = textAreaWidth;
        lastTextX = parentX + PADDING_H;
        lastTextY = parentY + PADDING_V;
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
        handler.beginDrag(charIndexAtPosition(pointerX));
        return true;
    }

    @Override
    public boolean onPointerMove(float pointerX, float pointerY) {
        if (!handler.isMouseSelecting()) {
            return false;
        }
        handler.updateDrag(charIndexAtPosition(pointerX));
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

        if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
            handler.submit();
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_HOME) {
            handler.moveCaret(0, shift);
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_END) {
            handler.moveCaret(handler.length(), shift);
            return true;
        }
        if (ctrl) {
            return handler.handleCtrlKeyPress(keyCode, modifiers, false);
        }
        return handler.handleBasicKeyPress(keyCode, modifiers);
    }

    @Override
    public boolean capturesCharType(char character) {
        return false;
    }

    @Override
    public boolean onCharType(char character) {
        if (!handler.isFocused() || character == '\n' || character == '\r') {
            return false;
        }
        return handler.handleCharType(character);
    }

    @Override
    protected void renderSelf(RenderFrame frame, float deltaSeconds) {
        lastRenderFrame = frame;
        int posX = bounds().positionX();
        int posY = bounds().positionY();
        int width = bounds().width();
        int height = bounds().height();
        int lineH = lastLineHeight > 0 ? lastLineHeight : frame.fontHeight();
        String text = handler.buffer().toString();

        frame.drawRoundedRect(posX, posY, width, height, CORNER_RADIUS, frame.theme().inputFill());
        int borderColor = !handler.value().isEmpty() && !parser.isValid(handler.value())
                ? BORDER_INVALID : frame.theme().inputBorder();
        frame.drawRoundedRectFrame(posX, posY, width, height, CORNER_RADIUS, borderColor, 0, 1);

        if (text.isEmpty()) {
            UiText.of(placeholder).color(frame.theme().textMuted()).draw(frame, lastTextX, lastTextY);
            if (handler.isFocused() && (System.currentTimeMillis() / CARET_BLINK_HALF_PERIOD_MS & 1L) == 0L) {
                drawCaret(frame, lastTextX, lastTextY, lineH);
            }
            return;
        }

        frame.pushClip(new ClipRegion(posX + PADDING_H, posY + PADDING_V, lastTextAreaWidth, height - PADDING_V * 2));

        if (handler.isFocused() && handler.hasSelection()) {
            int selStart = handler.selectStart();
            int selEnd = handler.selectEnd();
            int startPx = frame.measureTextWidth(text.substring(0, selStart), false);
            int endPx = frame.measureTextWidth(text.substring(0, selEnd), false);
            if (endPx > startPx) {
                frame.drawRect(lastTextX + startPx - scrollOffsetX, lastTextY, endPx - startPx, lineH, SELECTION_COLOR);
            }
        }

        UiText.of(text).color(frame.theme().textPrimary()).draw(frame, lastTextX - scrollOffsetX, lastTextY);

        frame.popClip();
        frame.flushText();

        if (handler.isFocused() && !handler.hasSelection() && (System.currentTimeMillis() / CARET_BLINK_HALF_PERIOD_MS & 1L) == 0L) {
            int caretX = lastTextX + frame.measureTextWidth(text.substring(0, handler.caretIndex()), false) - scrollOffsetX;
            drawCaret(frame, caretX, lastTextY, lineH);
        }
    }

    private void drawCaret(RenderFrame frame, int caretX, int caretY, int lineH) {
        int clipTop = bounds().positionY() + PADDING_V;
        int clipBottom = bounds().positionY() + bounds().height() - PADDING_V;
        int drawTop = Math.max(clipTop, caretY);
        int drawBottom = Math.min(clipBottom, caretY + lineH);
        if (drawBottom > drawTop) {
            frame.drawRect(caretX, drawTop, 1, drawBottom - drawTop, frame.theme().caret());
        }
    }

    private int charIndexAtPosition(float pointerX) {
        if (lastRenderFrame == null) {
            return handler.caretIndex();
        }
        String text = handler.buffer().toString();
        float localX = pointerX - lastTextX + scrollOffsetX;
        int best = 0;
        int bestDist = Integer.MAX_VALUE;
        for (int charPos = 0; charPos <= text.length(); charPos++) {
            int dist = Math.abs(lastRenderFrame.measureTextWidth(text.substring(0, charPos), false) - (int) localX);
            if (dist < bestDist) {
                bestDist = dist;
                best = charPos;
            }
        }
        return best;
    }
}
