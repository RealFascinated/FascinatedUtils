package cc.fascinated.fascinatedutils.gui.node.input;

import cc.fascinated.fascinatedutils.gui.layout.PositionedNode;
import cc.fascinated.fascinatedutils.gui.core.UiNode;
import cc.fascinated.fascinatedutils.gui.render.AlumiteGuiRenderer;
import cc.fascinated.fascinatedutils.gui.render.ClipRegion;
import cc.fascinated.fascinatedutils.gui.theme.UiMetrics;
import net.minecraft.util.Util;
import org.lwjgl.glfw.GLFW;

import java.util.Objects;
import java.util.function.Consumer;

public class TextInputNode<V> extends PositionedNode<TextInputNode<V>> {

    private static final int CORNER_RADIUS = UiMetrics.RADIUS;
    private static final long CARET_BLINK_HALF_PERIOD_MS = 530L;
    private static final int DEFAULT_MAX_LENGTH = 256;

    private final TextParser<V> parser;
    private Consumer<V> typedSubmit = ignored -> {};
    private Consumer<V> typedChange = ignored -> {};
    private final TextInputHandler handler = new TextInputHandler();
    private String placeholder = "";
    private V composedValue;
    private int scrollOffsetX;
    // The caret stays solid right after any interaction and only then starts blinking.
    private long lastInteractionMillis;

    // Layout results reused by pointer hit testing and rendering.
    private AlumiteGuiRenderer lastRenderFrame;
    private int lastTextX;
    private int lastTextY;
    private int lastTextAreaWidth;
    private int lastLineHeight;
    private int arrangedCaretIndex = -1;
    private int arrangedTextLength = -1;

    public TextInputNode(TextParser<V> parser) {
        this.parser = parser;
        fullWidth();
        handler.setMaxLength(DEFAULT_MAX_LENGTH);
        handler.setOnSubmit(text -> {
            V parsed = parser.parse(text);
            if (parsed != null) {
                typedSubmit.accept(parsed);
            }
        });
        handler.setOnChange(text -> {
            V parsed = parser.parse(text);
            if (parsed != null) {
                typedChange.accept(parsed);
            }
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
        composedValue = value;
        handler.setValue(value == null ? "" : parser.format(value));
        return this;
    }

    public V value() {
        return parser.parse(handler.value());
    }

    public void clear() {
        handler.clear();
        scrollOffsetX = 0;
    }

    public void submit() {
        handler.submit();
    }

    public void cancel() {
        handler.cancel();
    }

    @Override
    protected int intrinsicWidth(AlumiteGuiRenderer renderFrame, int parentWidth) {
        return parentWidth;
    }

    @Override
    protected int intrinsicHeight(AlumiteGuiRenderer renderFrame, int parentHeight, int resolvedWidth) {
        return Math.max(UiMetrics.INPUT_PAD_Y * 2 + renderFrame.fontHeight(), Math.min(parentHeight, UiMetrics.CONTROL_HEIGHT));
    }

    @Override
    protected boolean layoutInputsChanged(AlumiteGuiRenderer renderFrame) {
        return super.layoutInputsChanged(renderFrame) || arrangedCaretIndex != handler.caretIndex() || arrangedTextLength != handler.length();
    }

    @Override
    protected void arrangeChildren(AlumiteGuiRenderer renderFrame, int positionX, int positionY, int width, int height) {
        lastRenderFrame = renderFrame;
        int lineHeight = renderFrame.fontHeight();
        int textAreaWidth = Math.max(1, width - UiMetrics.INPUT_PAD_X * 2);
        arrangedCaretIndex = handler.caretIndex();
        arrangedTextLength = handler.length();
        String text = handler.value();
        int caretPixels = renderFrame.measureTextWidth(text.substring(0, arrangedCaretIndex), false);
        if (caretPixels < scrollOffsetX) {
            scrollOffsetX = caretPixels;
        } else if (caretPixels > scrollOffsetX + textAreaWidth) {
            scrollOffsetX = caretPixels - textAreaWidth;
        }
        // Shorter text must not leave the view scrolled past its end.
        int maxScroll = Math.max(0, renderFrame.measureTextWidth(text, false) - textAreaWidth);
        scrollOffsetX = Math.max(0, Math.min(scrollOffsetX, maxScroll));

        lastLineHeight = lineHeight;
        lastTextAreaWidth = textAreaWidth;
        lastTextX = positionX + UiMetrics.INPUT_PAD_X;
        lastTextY = positionY + (height - lineHeight) / 2;
    }

    @Override
    protected void onTranslated(int deltaX, int deltaY) {
        lastTextX += deltaX;
        lastTextY += deltaY;
    }

    @Override
    protected void inheritState(UiNode previous) {
        super.inheritState(previous);
        TextInputNode<?> previousInput = (TextInputNode<?>) previous;
        // Keep what the user typed unless the composed value was deliberately changed to something it does not parse to.
        if (Objects.equals(composedValue, previousInput.composedValue) || Objects.equals(composedValue, parser.parse(previousInput.handler.value()))) {
            handler.inheritTextFrom(previousInput.handler);
        }
        handler.setFocused(previousInput.handler.focused());
        handler.inheritInteractionFrom(previousInput.handler);
        scrollOffsetX = previousInput.scrollOffsetX;
        lastInteractionMillis = previousInput.lastInteractionMillis;
    }

    @Override
    public boolean focusable() {
        return true;
    }

    @Override
    public boolean hitTarget() {
        return true;
    }

    @Override
    public void onFocusGained() {
        handler.setFocused(true);
        touch();
    }

    @Override
    public void onFocusLost() {
        handler.setFocused(false);
    }

    private void touch() {
        lastInteractionMillis = System.currentTimeMillis();
    }

    @Override
    public boolean onPointerPress(float pointerX, float pointerY, int button) {
        if (button != 0 || !contains(pointerX, pointerY)) {
            return false;
        }
        touch();
        handler.beginDrag(charIndexAtPosition(pointerX));
        return true;
    }

    @Override
    public boolean onPointerMove(float pointerX, float pointerY) {
        if (!handler.mouseSelecting()) {
            return false;
        }
        touch();
        handler.updateDrag(charIndexAtPosition(pointerX));
        return true;
    }

    @Override
    public boolean onPointerRelease(float pointerX, float pointerY, int button) {
        if (button != 0 || !handler.mouseSelecting()) {
            return false;
        }
        handler.endDrag();
        return false;
    }

    /**
     * Whether the platform's shortcut modifier is held: Control, or Command on macOS.
     */
    private static boolean isShortcutDown(int modifiers) {
        return (modifiers & GLFW.GLFW_MOD_CONTROL) != 0 || Util.getPlatform() == Util.OS.OSX && (modifiers & GLFW.GLFW_MOD_SUPER) != 0;
    }

    @Override
    public boolean onKeyPress(int keyCode, int modifiers) {
        if (!handler.focused()) {
            return false;
        }
        touch();
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
        if (isShortcutDown(modifiers)) {
            return handler.handleShortcutKeyPress(keyCode, modifiers);
        }
        return handler.handleBasicKeyPress(keyCode, modifiers);
    }

    @Override
    public boolean onCharType(int codePoint) {
        if (!handler.focused() || Character.isISOControl(codePoint)) {
            return false;
        }
        touch();
        return handler.handleCharType(codePoint);
    }

    private boolean caretPhaseVisible() {
        return ((System.currentTimeMillis() - lastInteractionMillis) / CARET_BLINK_HALF_PERIOD_MS & 1L) == 0L;
    }

    @Override
    protected void renderSelf(AlumiteGuiRenderer frame, float deltaSeconds) {
        lastRenderFrame = frame;
        int posX = bounds().positionX();
        int posY = bounds().positionY();
        int width = bounds().width();
        int height = bounds().height();
        int lineHeight = lastLineHeight > 0 ? lastLineHeight : frame.fontHeight();
        String text = handler.value();
        boolean focused = handler.focused();

        frame.drawRoundedRect(posX, posY, width, height, CORNER_RADIUS, frame.theme().inputFill());
        int borderColor = !text.isEmpty() && !parser.isValid(text)
                ? frame.theme().inputInvalidBorder() : focused ? frame.theme().focusRing() : frame.theme().inputBorder();
        frame.drawRoundedRectFrame(posX, posY, width, height, CORNER_RADIUS, borderColor, 0, 1);

        if (text.isEmpty()) {
            if (!focused) {
                frame.drawText(placeholder, lastTextX, lastTextY, frame.theme().textMuted(), false, false);
            } else if (caretPhaseVisible()) {
                drawCaret(frame, lastTextX, lastTextY, lineHeight);
            }
            return;
        }

        frame.pushClip(new ClipRegion(posX + UiMetrics.INPUT_PAD_X, posY + 1, lastTextAreaWidth, height - 2));

        if (focused && handler.hasSelection()) {
            int selectionStart = Math.max(0, Math.min(handler.selectStart(), text.length()));
            int selectionEnd = Math.max(0, Math.min(handler.selectEnd(), text.length()));
            int startPixels = frame.measureTextWidth(text.substring(0, selectionStart), false);
            int endPixels = frame.measureTextWidth(text.substring(0, selectionEnd), false);
            if (endPixels > startPixels) {
                frame.drawRect(lastTextX + startPixels - scrollOffsetX, lastTextY, endPixels - startPixels, lineHeight, frame.theme().selection());
            }
        }

        frame.drawText(text, lastTextX - scrollOffsetX, lastTextY, frame.theme().textPrimary(), false, false);

        frame.popClip();

        if (focused && !handler.hasSelection() && caretPhaseVisible()) {
            int caretX = lastTextX + frame.measureTextWidth(text.substring(0, handler.caretIndex()), false) - scrollOffsetX;
            drawCaret(frame, caretX, lastTextY, lineHeight);
        }
    }

    private void drawCaret(AlumiteGuiRenderer frame, int caretX, int caretY, int lineHeight) {
        int clipTop = bounds().positionY() + UiMetrics.INPUT_PAD_Y;
        int clipBottom = bounds().positionY() + bounds().height() - UiMetrics.INPUT_PAD_Y;
        int drawTop = Math.max(clipTop, caretY);
        int drawBottom = Math.min(clipBottom, caretY + lineHeight);
        if (drawBottom > drawTop) {
            frame.drawRect(caretX, drawTop, 1, drawBottom - drawTop, frame.theme().caret());
        }
    }

    /**
     * Finds the caret index closest to a pointer position. Prefix widths only grow with the index, so a binary search
     * over prefixes finds the boundary in a logarithmic number of measurements.
     */
    private int charIndexAtPosition(float pointerX) {
        if (lastRenderFrame == null) {
            return handler.caretIndex();
        }
        String text = handler.value();
        float localX = pointerX - lastTextX + scrollOffsetX;
        int low = 0;
        int high = text.length();
        while (low < high) {
            int middle = (low + high) >>> 1;
            if (lastRenderFrame.measureTextWidth(text.substring(0, middle), false) < localX) {
                low = middle + 1;
            } else {
                high = middle;
            }
        }
        int after = low;
        if (after > 0 && after < text.length() && Character.isLowSurrogate(text.charAt(after))) {
            after--;
        }
        if (after == 0) {
            return 0;
        }
        int before = text.offsetByCodePoints(after, -1);
        float afterGap = lastRenderFrame.measureTextWidth(text.substring(0, after), false) - localX;
        float beforeGap = localX - lastRenderFrame.measureTextWidth(text.substring(0, before), false);
        return afterGap < beforeGap ? after : before;
    }
}
