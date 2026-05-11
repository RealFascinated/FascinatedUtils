package cc.fascinated.fascinatedutils.gui2.node.input;

import cc.fascinated.fascinatedutils.gui2.core.PositionedNode;
import cc.fascinated.fascinatedutils.gui2.core.UiState;
import cc.fascinated.fascinatedutils.gui2.render.ClipRegion;
import cc.fascinated.fascinatedutils.gui2.render.RenderFrame;
import cc.fascinated.fascinatedutils.gui2.render.UiText;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class TextboxInputNode extends PositionedNode<TextboxInputNode> {

    private static final int PADDING_V = 10;
    private static final int PADDING_H = 10;

    private int leftInset = 0;
    private static final int CORNER_RADIUS = 4;
    private static final int LINE_GAP = 2;
    private static final int MAX_VISIBLE_LINES = 10;
    private static final long CARET_BLINK_HALF_PERIOD_MS = 530L;
    private static final int CARET_VERTICAL_INSET = 0;
    private static final int SELECTION_COLOR = 0x55aaccff;
    private static final int NEWLINE_SELECTION_EXTRA = 4;

    private final TextInputHandler handler = new TextInputHandler();
    private String placeholder = "";

    // Layout / render cache
    private List<int[]> lastVisualLines;
    private String lastComputedText;
    private int lastComputedWidth = -1;
    private RenderFrame lastRenderFrame;
    private int lastTextAreaWidth;
    private int lastTextAreaX;
    private int lastTextAreaY;
    private int lastLineHeight;
    private int scrollOffsetY;

    public TextboxInputNode() {
        fullWidth();
        handler.setMaxLength(20000);
        handler.setOnBufferMutated(this::invalidateLineCache);
    }

    public TextboxInputNode setLeftInset(int leftInset) {
        this.leftInset = leftInset;
        return this;
    }

    public int minHeight(RenderFrame frame) {
        return PADDING_V * 2 + frame.fontHeight();
    }

    public TextboxInputNode setPlaceholder(String placeholder) {
        this.placeholder = placeholder == null ? "" : placeholder;
        return this;
    }

    public TextboxInputNode setMaxLength(int maxLength) {
        handler.setMaxLength(maxLength);
        return this;
    }

    public TextboxInputNode setOnSubmit(Consumer<String> onSubmit) {
        handler.setOnSubmit(onSubmit);
        return this;
    }

    public TextboxInputNode setOnChange(Consumer<String> onChange) {
        handler.setOnChange(onChange);
        return this;
    }

    public TextboxInputNode setOnCancel(Runnable onCancel) {
        handler.setOnCancel(onCancel);
        return this;
    }

    public void submit() {
        handler.submit();
    }

    public void cancel() {
        handler.cancel();
    }

    public TextboxInputNode setValue(String value) {
        handler.setValue(value);
        invalidateLineCache();
        return this;
    }

    /**
     * Binds an external {@link UiState} to persist the caret index across recompose cycles.
     * Call this after {@link #setValue(String)}.
     *
     * @param state persistent state for the caret index
     * @return this
     */
    public TextboxInputNode bindCaretState(UiState<Integer> state) {
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
    public TextboxInputNode bindSelectionState(UiState<Integer> state) {
        handler.bindSelectionState(state);
        return this;
    }

    /**
     * Binds an external {@link UiState} to persist whether a mouse-drag selection is active.
     *
     * @param state persistent drag state
     * @return this
     */
    public TextboxInputNode bindDragState(UiState<Boolean> state) {
        handler.bindDragState(state);
        return this;
    }

    public String value() {
        return handler.value();
    }

    public void clear() {
        handler.clear();
        scrollOffsetY = 0;
        invalidateLineCache();
    }

    /**
     * Returns the preferred height in pixels for the given available width based on the current content.
     *
     * @param frame          render frame used for font measurement
     * @param availableWidth total available width including horizontal padding
     * @return preferred height in pixels
     */
    public int preferredHeight(RenderFrame frame, int availableWidth) {
        int textAreaWidth = Math.max(1, availableWidth - PADDING_H - leftInset - PADDING_H);
        int lineCount = Math.max(1, Math.min(visualLines(frame, textAreaWidth).size(), MAX_VISIBLE_LINES));
        int lineH = frame.fontHeight();
        return PADDING_V * 2 + lineCount * lineH + Math.max(0, lineCount - 1) * LINE_GAP;
    }

    @Override
    public void layout(RenderFrame frame, int parentX, int parentY, int parentWidth, int parentHeight) {
        lastRenderFrame = frame;
        int textAreaWidth = Math.max(1, parentWidth - PADDING_H - leftInset - PADDING_H);
        List<int[]> lines = visualLines(frame, textAreaWidth);
        int lineH = frame.fontHeight();
        int lineCount = Math.max(1, Math.min(lines.size(), MAX_VISIBLE_LINES));
        int computedHeight = PADDING_V * 2 + lineCount * lineH + Math.max(0, lineCount - 1) * LINE_GAP;
        bounds().set(parentX, parentY, parentWidth, computedHeight);

        // Keep caret in view
        int visibleH = computedHeight - PADDING_V * 2;
        int caretLineIdx = findCaretVisualLine(lines);
        int caretTop = caretLineIdx * (lineH + LINE_GAP);
        int caretBottom = caretTop + lineH;
        if (caretTop < scrollOffsetY) {
            scrollOffsetY = caretTop;
        } else if (caretBottom > scrollOffsetY + visibleH) {
            scrollOffsetY = caretBottom - visibleH;
        }
        scrollOffsetY = Math.max(0, scrollOffsetY);

        lastVisualLines = lines;
        lastLineHeight = lineH;
        lastTextAreaWidth = textAreaWidth;
        lastTextAreaX = parentX + PADDING_H + leftInset;
        lastTextAreaY = parentY + PADDING_V;
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
        handler.beginDrag(charIndexAtPosition(pointerX, pointerY));
        return true;
    }

    @Override
    public boolean onPointerMove(float pointerX, float pointerY) {
        if (!handler.isMouseSelecting()) {
            return false;
        }
        handler.updateDrag(charIndexAtPosition(pointerX, pointerY));
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
    public boolean onPointerScroll(float pointerX, float pointerY, float delta) {
        if (!contains(pointerX, pointerY) || lastVisualLines == null) {
            return false;
        }
        int lineH = lastLineHeight > 0 ? lastLineHeight : 9;
        scrollOffsetY = Math.max(0, scrollOffsetY - Math.round(delta * (lineH + LINE_GAP) * 3));
        return true;
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
            // Home/End: go to document start/end (Ctrl+Home, Ctrl+End)
            if (keyCode == GLFW.GLFW_KEY_HOME) {
                handler.moveCaret(0, shift);
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_END) {
                handler.moveCaret(handler.length(), shift);
                return true;
            }
            return handler.handleCtrlKeyPress(keyCode, modifiers, true);
        }

        switch (keyCode) {
            case GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER -> {
                if (shift) {
                    handler.handleCharType('\n');
                } else {
                    handler.submit();
                }
                return true;
            }
            case GLFW.GLFW_KEY_UP -> {
                moveCaretVertical(-1, shift);
                return true;
            }
            case GLFW.GLFW_KEY_DOWN -> {
                moveCaretVertical(1, shift);
                return true;
            }
            case GLFW.GLFW_KEY_HOME -> {
                handler.moveCaret(currentVisualLineStart(), shift);
                return true;
            }
            case GLFW.GLFW_KEY_END -> {
                handler.moveCaret(currentVisualLineEnd(), shift);
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
    protected void renderSelf(RenderFrame frame, float deltaSeconds) {
        lastRenderFrame = frame;
        int bx = bounds().positionX();
        int by = bounds().positionY();
        int bw = bounds().width();
        int bh = bounds().height();
        int lineH = frame.fontHeight();

        frame.drawRoundedRect(bx, by, bw, bh, CORNER_RADIUS, frame.theme().inputFill());
        frame.drawRoundedRectFrame(bx, by, bw, bh, CORNER_RADIUS, frame.theme().inputBorder(), 0, 1);

        if (handler.buffer().isEmpty()) {
            UiText.of(placeholder).color(frame.theme().textMuted()).draw(frame, lastTextAreaX, lastTextAreaY);
            if (handler.isFocused() && (System.currentTimeMillis() / CARET_BLINK_HALF_PERIOD_MS & 1L) == 0L) {
                drawCaret(frame, lastTextAreaX, lastTextAreaY, lineH);
            }
            return;
        }

        List<int[]> lines = visualLines(frame, lastTextAreaWidth);
        int caretLineIdx = findCaretVisualLine(lines);

        frame.pushClip(new ClipRegion(bx + PADDING_H + leftInset, by + PADDING_V, lastTextAreaWidth, bh - PADDING_V * 2));

        // Selection highlight
        if (handler.isFocused() && handler.hasSelection()) {
            int selStart = handler.selectStart();
            int selEnd = handler.selectEnd();
            for (int lineIdx = 0; lineIdx < lines.size(); lineIdx++) {
                int[] line = lines.get(lineIdx);
                if (selEnd <= line[0] || selStart > line[1]) {
                    continue;
                }
                int lineY = lastTextAreaY + lineIdx * (lineH + LINE_GAP) - scrollOffsetY;
                if (lineY + lineH < by + PADDING_V || lineY > by + bh - PADDING_V) {
                    continue;
                }
                int clampedStart = Math.max(selStart, line[0]);
                int startPx = frame.measureTextWidth(handler.buffer().substring(line[0], clampedStart), false);
                int endPx;
                if (selEnd > line[1]) {
                    endPx = frame.measureTextWidth(handler.buffer().substring(line[0], line[1]), false);
                    if (line[2] == 1) {
                        endPx += NEWLINE_SELECTION_EXTRA;
                    }
                } else {
                    endPx = frame.measureTextWidth(handler.buffer().substring(line[0], selEnd), false);
                }
                if (endPx > startPx) {
                    frame.drawRect(lastTextAreaX + startPx, lineY, endPx - startPx, lineH, SELECTION_COLOR);
                }
            }
        }

        // Text lines
        for (int lineIdx = 0; lineIdx < lines.size(); lineIdx++) {
            int[] line = lines.get(lineIdx);
            int lineY = lastTextAreaY + lineIdx * (lineH + LINE_GAP) - scrollOffsetY;
            if (lineY + lineH < by + PADDING_V || lineY > by + bh - PADDING_V) {
                continue;
            }
            if (line[0] < line[1]) {
                UiText.of(handler.buffer().substring(line[0], line[1])).color(frame.theme().textPrimary()).draw(frame, lastTextAreaX, lineY);
            }
        }

        frame.popClip();
        frame.flushText();

        // Caret
        if (handler.isFocused() && !handler.hasSelection() && (System.currentTimeMillis() / CARET_BLINK_HALF_PERIOD_MS & 1L) == 0L) {
            int[] caretLine = lines.get(caretLineIdx);
            int caretX = lastTextAreaX + frame.measureTextWidth(handler.buffer().substring(caretLine[0], handler.caretIndex()), false);
            int caretY = lastTextAreaY + caretLineIdx * (lineH + LINE_GAP) - scrollOffsetY;
            drawCaret(frame, caretX, caretY, lineH);
        }
    }

    private void drawCaret(RenderFrame frame, int caretX, int caretY, int lineH) {
        int clipTop = bounds().positionY() + PADDING_V;
        int clipBottom = bounds().positionY() + bounds().height() - PADDING_V;
        int drawTop = Math.max(clipTop, caretY + CARET_VERTICAL_INSET);
        int drawBottom = Math.min(clipBottom, caretY + lineH - CARET_VERTICAL_INSET);
        if (drawBottom > drawTop) {
            frame.drawRect(caretX, drawTop, 1, drawBottom - drawTop, frame.theme().caret());
        }
    }

    /**
     * Returns the visual line list, computing and caching if the text or width changed.
     *
     * <p>Each entry is {@code {bufStart, bufEnd, hardBreak}} where {@code hardBreak} is {@code 1}
     * if the line ends with an explicit {@code \n}, {@code 0} for word-wrapped or last lines.
     */
    private List<int[]> visualLines(RenderFrame frame, int availableWidth) {
        String text = handler.buffer().toString();
        if (lastVisualLines != null && text.equals(lastComputedText) && availableWidth == lastComputedWidth) {
            return lastVisualLines;
        }
        lastComputedText = text;
        lastComputedWidth = availableWidth;
        lastVisualLines = computeVisualLines(frame, text, availableWidth);
        return lastVisualLines;
    }

    private static List<int[]> computeVisualLines(RenderFrame frame, String text, int availableWidth) {
        if (text.isEmpty()) {
            List<int[]> single = new ArrayList<>(1);
            single.add(new int[]{0, 0, 0});
            return single;
        }
        List<int[]> result = new ArrayList<>();
        int len = text.length();
        int pos = 0;
        while (pos <= len) {
            int nlAt = text.indexOf('\n', pos);
            boolean hasNl = nlAt >= 0;
            int paraEnd = hasNl ? nlAt : len;
            wrapParagraph(frame, text, pos, paraEnd, hasNl ? 1 : 0, availableWidth, result);
            if (hasNl) {
                pos = paraEnd + 1;
            } else {
                break;
            }
        }
        return result.isEmpty() ? new ArrayList<>(List.of(new int[]{0, 0, 0})) : result;
    }

    private static void wrapParagraph(RenderFrame frame, String text, int paraStart, int paraEnd, int hardBreak, int availableWidth, List<int[]> result) {
        if (paraStart == paraEnd) {
            result.add(new int[]{paraStart, paraEnd, hardBreak});
            return;
        }
        int lineStart = paraStart;
        while (lineStart < paraEnd) {
            int lineEnd = lineStart;
            int lastSpaceAt = -1;
            boolean wrapped = false;
            while (lineEnd < paraEnd) {
                int lineWidth = frame.measureTextWidth(text.substring(lineStart, lineEnd + 1), false);
                if (lineWidth > availableWidth) {
                    if (lineEnd > lineStart && lastSpaceAt >= lineStart) {
                        result.add(new int[]{lineStart, lastSpaceAt, 0});
                        lineStart = lastSpaceAt + 1;
                    } else {
                        int breakAt = Math.max(lineEnd, lineStart + 1);
                        result.add(new int[]{lineStart, breakAt, 0});
                        lineStart = breakAt;
                    }
                    wrapped = true;
                    break;
                }
                if (text.charAt(lineEnd) == ' ') {
                    lastSpaceAt = lineEnd;
                }
                lineEnd++;
            }
            if (!wrapped) {
                result.add(new int[]{lineStart, paraEnd, hardBreak});
                break;
            }
        }
    }

    private int findCaretVisualLine(List<int[]> lines) {
        int caretIndex = handler.caretIndex();
        for (int lineIdx = 0; lineIdx < lines.size(); lineIdx++) {
            int[] line = lines.get(lineIdx);
            if (caretIndex >= line[0] && caretIndex <= line[1]) {
                return lineIdx;
            }
        }
        return lines.size() - 1;
    }

    private void moveCaretVertical(int direction, boolean shift) {
        if (lastVisualLines == null || lastRenderFrame == null) {
            return;
        }
        int lineIdx = findCaretVisualLine(lastVisualLines);
        int targetLineIdx = lineIdx + direction;
        if (targetLineIdx < 0) {
            handler.moveCaret(0, shift);
            return;
        }
        if (targetLineIdx >= lastVisualLines.size()) {
            handler.moveCaret(handler.length(), shift);
            return;
        }
        int[] curLine = lastVisualLines.get(lineIdx);
        int caretX = lastRenderFrame.measureTextWidth(handler.buffer().substring(curLine[0], handler.caretIndex()), false);
        handler.moveCaret(closestIndexOnLine(lastVisualLines.get(targetLineIdx), caretX), shift);
    }

    private int closestIndexOnLine(int[] line, int targetX) {
        if (lastRenderFrame == null) {
            return handler.caretIndex();
        }
        String lineText = handler.buffer().substring(line[0], line[1]);
        int best = line[0];
        int bestDist = Integer.MAX_VALUE;
        for (int charPos = 0; charPos <= lineText.length(); charPos++) {
            int dist = Math.abs(lastRenderFrame.measureTextWidth(lineText.substring(0, charPos), false) - targetX);
            if (dist < bestDist) {
                bestDist = dist;
                best = line[0] + charPos;
            }
        }
        return best;
    }

    private int currentVisualLineStart() {
        if (lastVisualLines == null) {
            return 0;
        }
        return lastVisualLines.get(findCaretVisualLine(lastVisualLines))[0];
    }

    private int currentVisualLineEnd() {
        if (lastVisualLines == null) {
            return handler.length();
        }
        return lastVisualLines.get(findCaretVisualLine(lastVisualLines))[1];
    }

    private int charIndexAtPosition(float pointerX, float pointerY) {
        if (lastVisualLines == null || lastRenderFrame == null) {
            return handler.caretIndex();
        }
        int lineH = lastLineHeight > 0 ? lastLineHeight : 9;
        float localY = pointerY - lastTextAreaY + scrollOffsetY;
        int lineIdx = Math.max(0, Math.min((int) (localY / (lineH + LINE_GAP)), lastVisualLines.size() - 1));
        return closestIndexOnLine(lastVisualLines.get(lineIdx), Math.round(pointerX - lastTextAreaX));
    }

    private void invalidateLineCache() {
        lastComputedText = null;
    }
}
