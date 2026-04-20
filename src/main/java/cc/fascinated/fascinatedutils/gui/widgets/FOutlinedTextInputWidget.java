package cc.fascinated.fascinatedutils.gui.widgets;

import cc.fascinated.fascinatedutils.common.ColorUtils;
import cc.fascinated.fascinatedutils.gui.core.*;
import cc.fascinated.fascinatedutils.gui.renderer.GuiRenderer;
import cc.fascinated.fascinatedutils.gui.renderer.RectCornerRoundMask;
import cc.fascinated.fascinatedutils.gui.renderer.UIRenderer;
import cc.fascinated.fascinatedutils.gui.theme.UITheme;
import net.minecraft.util.Mth;
import org.lwjgl.glfw.GLFW;

import java.util.function.IntSupplier;
import java.util.function.Supplier;

public class FOutlinedTextInputWidget extends FWidget {
    private final int focusId;
    private final int maxLength;
    private final float intrinsicHeightDesign;

    private String value = "";
    private int caretIndex;
    private int selectionAnchor = -1;
    private float scrollOffsetX = 0f;

    private Callback<String> onChange = ignored -> {};
    private IntSupplier externalFocusIdSupplier = () -> UiFocusIds.NO_FOCUS_ID;
    private Supplier<String> placeholderSupplier = () -> "";

    public FOutlinedTextInputWidget(int focusId, int maxLength, float intrinsicHeightDesign, Supplier<String> placeholderSupplier) {
        this.focusId = focusId;
        this.maxLength = Math.max(0, maxLength);
        this.intrinsicHeightDesign = Math.max(0f, intrinsicHeightDesign);
        setPlaceholderSupplier(placeholderSupplier);
    }

    public void setValue(String value) {
        this.value = value == null ? "" : value;
        caretIndex = this.value.length();
        selectionAnchor = -1;
        scrollOffsetX = 0f;
    }

    public String value() {
        return value;
    }

    public void setOnChange(Callback<String> onChange) {
        this.onChange = onChange == null ? ignored -> {} : onChange;
    }

    public void setExternalFocusIdSupplier(IntSupplier externalFocusIdSupplier) {
        this.externalFocusIdSupplier = externalFocusIdSupplier == null ? () -> UiFocusIds.NO_FOCUS_ID : externalFocusIdSupplier;
    }

    public void setPlaceholderSupplier(Supplier<String> placeholderSupplier) {
        this.placeholderSupplier = placeholderSupplier == null ? () -> "" : placeholderSupplier;
    }

    @Override
    public int focusId() {
        return focusId;
    }

    @Override
    public boolean wantsPointer() {
        return true;
    }

    @Override
    public UiPointerCursor pointerCursor(float px, float py) {
        return UiPointerCursor.TEXT;
    }

    @Override
    public boolean fillsHorizontalInRow() {
        return true;
    }

    @Override
    public void layout(UIRenderer measure, float layoutX, float layoutY, float layoutWidth, float layoutHeight) {
        setBounds(layoutX, layoutY, layoutWidth, layoutHeight);
    }

    @Override
    public float intrinsicHeightForColumn(UIRenderer measure, float widthBudget) {
        return intrinsicHeightDesign;
    }

    @Override
    public boolean mouseDown(float pointerX, float pointerY, int button) {
        if (button == 0 && containsPoint(pointerX, pointerY)) {
            float innerX = pointerX - (x() + UITheme.INPUT_PAD_X) + scrollOffsetX;
            int newIndex = 0;
            float bestDist = Float.MAX_VALUE;
            for (int i = 0; i <= value.length(); i++) {
                float charX = value.length() == 0 ? 0f : i * (innerX / value.length());
                float dist = Math.abs(charX - innerX);
                if (dist < bestDist) {
                    bestDist = dist;
                    newIndex = i;
                }
            }
            caretIndex = Mth.clamp(newIndex, 0, value.length());
            clearSelection();
            return true;
        }
        return false;
    }

    @Override
    public boolean charTyped(char character) {
        if (character < 32 && character != '\t') {
            return false;
        }
        deleteSelection();
        String before = value.substring(0, caretIndex);
        String after = value.substring(caretIndex);
        String next = before + character + after;
        if (next.length() <= maxLength) {
            commitValue(next);
            caretIndex = before.length() + 1;
            clampCaret();
        }
        return true;
    }

    @Override
    public boolean keyDown(int keyCode, int modifiers) {
        boolean ctrl = (modifiers & GLFW.GLFW_MOD_CONTROL) != 0;
        boolean shift = (modifiers & GLFW.GLFW_MOD_SHIFT) != 0;

        if (ctrl) {
            switch (keyCode) {
                case GLFW.GLFW_KEY_A -> {
                    selectionAnchor = 0;
                    caretIndex = value.length();
                    return true;
                }
                case GLFW.GLFW_KEY_C -> {
                    if (hasSelection()) {
                        GLFW.glfwSetClipboardString(0L, value.substring(selectStart(), selectEnd()));
                    }
                    return true;
                }
                case GLFW.GLFW_KEY_X -> {
                    if (hasSelection()) {
                        GLFW.glfwSetClipboardString(0L, value.substring(selectStart(), selectEnd()));
                        deleteSelection();
                    }
                    return true;
                }
                case GLFW.GLFW_KEY_V -> {
                    String clip = GLFW.glfwGetClipboardString(0L);
                    if (clip != null && !clip.isEmpty()) {
                        deleteSelection();
                        StringBuilder sb = new StringBuilder();
                        for (char c : clip.toCharArray()) {
                            if (c >= 32 || c == '\t') {
                                sb.append(c);
                            }
                        }
                        String insert = sb.toString();
                        String before = value.substring(0, caretIndex);
                        String after = value.substring(caretIndex);
                        String next = before + insert + after;
                        if (next.length() > maxLength) {
                            int room = maxLength - value.length() + (selectEnd() - selectStart());
                            insert = insert.substring(0, Math.max(0, Math.min(room, insert.length())));
                            next = before + insert + after;
                        }
                        commitValue(next);
                        caretIndex = before.length() + insert.length();
                        clampCaret();
                    }
                    return true;
                }
                case GLFW.GLFW_KEY_LEFT -> {
                    moveCaret(wordLeft(shift && hasSelection() ? selectStart() : caretIndex), shift);
                    return true;
                }
                case GLFW.GLFW_KEY_RIGHT -> {
                    moveCaret(wordRight(shift && hasSelection() ? selectEnd() : caretIndex), shift);
                    return true;
                }
                case GLFW.GLFW_KEY_BACKSPACE -> {
                    if (hasSelection()) {
                        deleteSelection();
                        return true;
                    }
                    if (caretIndex > 0) {
                        int target = wordLeft(caretIndex);
                        commitValue(value.substring(0, target) + value.substring(caretIndex));
                        caretIndex = target;
                        clampCaret();
                        return true;
                    }
                    return false;
                }
                case GLFW.GLFW_KEY_DELETE -> {
                    if (hasSelection()) {
                        deleteSelection();
                        return true;
                    }
                    if (caretIndex < value.length()) {
                        int target = wordRight(caretIndex);
                        commitValue(value.substring(0, caretIndex) + value.substring(target));
                        clampCaret();
                        return true;
                    }
                    return false;
                }
            }
        }

        switch (keyCode) {
            case GLFW.GLFW_KEY_BACKSPACE -> {
                if (deleteSelection()) {
                    return true;
                }
                if (caretIndex > 0) {
                    int next = caretIndex - 1;
                    commitValue(value.substring(0, next) + value.substring(caretIndex));
                    caretIndex = next;
                    clampCaret();
                    return true;
                }
                return false;
            }
            case GLFW.GLFW_KEY_DELETE -> {
                if (deleteSelection()) {
                    return true;
                }
                if (caretIndex < value.length()) {
                    commitValue(value.substring(0, caretIndex) + value.substring(caretIndex + 1));
                    clampCaret();
                    return true;
                }
                return false;
            }
            case GLFW.GLFW_KEY_LEFT -> {
                if (!shift && hasSelection()) {
                    caretIndex = selectStart();
                    clearSelection();
                }
                else {
                    moveCaret(caretIndex - 1, shift);
                }
                return true;
            }
            case GLFW.GLFW_KEY_RIGHT -> {
                if (!shift && hasSelection()) {
                    caretIndex = selectEnd();
                    clearSelection();
                }
                else {
                    moveCaret(caretIndex + 1, shift);
                }
                return true;
            }
            case GLFW.GLFW_KEY_HOME -> {
                moveCaret(0, shift);
                return true;
            }
            case GLFW.GLFW_KEY_END -> {
                moveCaret(value.length(), shift);
                return true;
            }
        }

        return false;
    }

    @Override
    protected void renderSelf(GuiRenderer graphics, float mouseX, float mouseY, float deltaSeconds) {
        boolean focused = fieldFocused();
        int borderArgb = focused ? graphics.theme().accentBright() : graphics.theme().border();
        int fillArgb = focused ? graphics.theme().surfaceElevated() : graphics.theme().surface();
        float borderThickness = UITheme.BORDER_THICKNESS_PX;
        float cornerRadius = Mth.clamp(3f, 0.5f, Math.min(w(), h()) * 0.5f - borderThickness * 0.5f - 0.01f);
        graphics.fillRoundedRectFrame(x(), y(), w(), h(), cornerRadius, borderArgb, fillArgb, borderThickness, borderThickness, RectCornerRoundMask.ALL);

        float pad = padX(graphics);
        float visibleW = w() - pad * 2f;
        float lineH = TextLayoutMetrics.layoutLineHeightPx(graphics);
        float textY = y() + Math.max(0f, h() - lineH) * 0.5f;

        if (focused) {
            scrollToCaret(graphics);
        }

        String placeholder = placeholderSupplier.get();
        if (placeholder == null) {
            placeholder = "";
        }
        String displayText;
        int displayColor;
        if (value.isEmpty()) {
            if (!focused && !placeholder.isEmpty()) {
                displayText = placeholder;
                displayColor = graphics.theme().textLabel();
            }
            else {
                displayText = " ";
                displayColor = graphics.theme().textPrimary();
            }
        }
        else {
            displayText = value;
            displayColor = graphics.theme().textPrimary();
        }

        float textDrawX = x() + pad - scrollOffsetX;

        if (focused && hasSelection()) {
            float selStartX = textDrawX + graphics.measureTextWidth(value.substring(0, selectStart()), false);
            float selEndX = textDrawX + graphics.measureTextWidth(value.substring(0, selectEnd()), false);
            float clipLeft = x() + pad;
            float clipRight = x() + w() - pad;
            float drawS = Math.max(selStartX, clipLeft);
            float drawE = Math.min(selEndX, clipRight);
            if (drawE > drawS) {
                int selColor = (graphics.theme().accentBright() & 0x00FFFFFF) | 0x55000000;
                graphics.drawRect(drawS, textY, drawE - drawS, Math.min(lineH, graphics.getFontHeight()), selColor);
            }
        }

        graphics.pushClip(x() + pad, y(), visibleW, h());
        graphics.drawMiniMessageText("<color:" + ColorUtils.rgbHex(displayColor) + ">" + displayText + "</color>", textDrawX, textY, false);
        graphics.popClip();

        if (focused && !hasSelection()) {
            long blink = System.currentTimeMillis() / 530L;
            if ((blink & 1L) == 0L) {
                float caretX = textDrawX + graphics.measureTextWidth(value.substring(0, Mth.clamp(caretIndex, 0, value.length())), false);
                float caretH = Math.min(lineH, (float) graphics.getFontHeight());
                graphics.drawRect(caretX, textY, 1f, caretH, displayColor);
            }
        }
    }

    private boolean fieldFocused() {
        if (externalFocusIdSupplier != null && externalFocusIdSupplier.getAsInt() != UiFocusIds.NO_FOCUS_ID) {
            return externalFocusIdSupplier.getAsInt() == focusId;
        }
        return GuiFocusState.getFocusedId() == focusId;
    }

    private void clampCaret() {
        caretIndex = Mth.clamp(caretIndex, 0, value.length());
    }

    private int selectStart() {
        return selectionAnchor < 0 ? caretIndex : Math.min(caretIndex, selectionAnchor);
    }

    private int selectEnd() {
        return selectionAnchor < 0 ? caretIndex : Math.max(caretIndex, selectionAnchor);
    }

    private boolean hasSelection() {
        return selectionAnchor >= 0 && selectionAnchor != caretIndex;
    }

    private boolean deleteSelection() {
        if (!hasSelection()) {
            return false;
        }
        int s = selectStart(), e = selectEnd();
        commitValue(value.substring(0, s) + value.substring(e));
        caretIndex = s;
        selectionAnchor = -1;
        clampCaret();
        return true;
    }

    private void clearSelection() {
        selectionAnchor = -1;
    }

    private void moveCaret(int newIndex, boolean shift) {
        if (shift) {
            if (selectionAnchor < 0) {
                selectionAnchor = caretIndex;
            }
        }
        else {
            selectionAnchor = -1;
        }
        caretIndex = Mth.clamp(newIndex, 0, value.length());
    }

    private int wordLeft(int pos) {
        if (pos == 0) {
            return 0;
        }
        int i = pos - 1;
        while (i > 0 && !Character.isLetterOrDigit(value.charAt(i - 1))) {
            i--;
        }
        while (i > 0 && Character.isLetterOrDigit(value.charAt(i - 1))) {
            i--;
        }
        return i;
    }

    private int wordRight(int pos) {
        int len = value.length();
        if (pos >= len) {
            return len;
        }
        int i = pos;
        while (i < len && !Character.isLetterOrDigit(value.charAt(i))) {
            i++;
        }
        while (i < len && Character.isLetterOrDigit(value.charAt(i))) {
            i++;
        }
        return i;
    }

    private void commitValue(String next) {
        onChange.invoke(next);
        value = next;
        clampCaret();
    }

    private float padX(GuiRenderer g) {
        return UITheme.INPUT_PAD_X;
    }

    private void scrollToCaret(GuiRenderer g) {
        float pad = padX(g);
        float visibleW = w() - pad * 2f;
        if (visibleW <= 0f) {
            return;
        }
        float caretX = g.measureTextWidth(value.substring(0, Mth.clamp(caretIndex, 0, value.length())), false);
        if (caretX - scrollOffsetX > visibleW) {
            scrollOffsetX = caretX - visibleW;
        }
        else if (caretX - scrollOffsetX < 0f) {
            scrollOffsetX = caretX;
        }
        float maxScroll = Math.max(0f, g.measureTextWidth(value, false) - visibleW);
        scrollOffsetX = Mth.clamp(scrollOffsetX, 0f, maxScroll);
    }
}