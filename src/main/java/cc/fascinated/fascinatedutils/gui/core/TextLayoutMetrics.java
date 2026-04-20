package cc.fascinated.fascinatedutils.gui.core;

import cc.fascinated.fascinatedutils.gui.renderer.UIRenderer;

import java.util.List;

public class TextLayoutMetrics {

    /**
     * Horizontal wrap budget in GUI pixels for a text host laid out at {@code layoutWidthPx}, matching {@link
     * GuiDesignSpace#guiTextWrapBudgetPx(float)}.
     */
    public static float wrapBudgetForLayoutWidthPx(float layoutWidthPx) {
        return layoutWidthPx;
    }

    /**
     * Line height used for layout and wrapped blocks, matching the layout line height.
     */
    public static float layoutLineHeightPx(UIRenderer renderer) {
        return Math.max(1f, (float) renderer.getFontHeight());
    }

    /**
     * Number of wrapped lines for plain text at a layout width, using the active font for measurement.
     */
    public static int wrappedLineCount(String text, float layoutWidthPx, boolean textBold, UIRenderer renderer) {
        if (text == null || text.isBlank() || layoutWidthPx <= 0f) {
            return 0;
        }
        float wrapBudgetGui = wrapBudgetForLayoutWidthPx(layoutWidthPx);
        return TextLineLayout.wrappedLineCount(text, wrapBudgetGui, segment -> renderer.measureTextWidth(segment, textBold));
    }

    /**
     * Wrapped lines for drawing or measurement, using the same budget as {@link #wrappedLineCount}.
     */
    public static List<String> wrapLines(String text, float layoutWidthPx, boolean textBold, UIRenderer renderer) {
        if (text == null || text.isBlank() || layoutWidthPx <= 0f) {
            return List.of();
        }
        float wrapBudgetGui = wrapBudgetForLayoutWidthPx(layoutWidthPx);
        return TextLineLayout.wrapLines(text, wrapBudgetGui, segment -> renderer.measureTextWidth(segment, textBold));
    }

    /**
     * Height of a wrapped block at the layout width (blank text or invalid width still yields one line height).
     */
    public static float wrappedTextBlockHeightPx(String text, float layoutWidthPx, boolean textBold, UIRenderer renderer) {
        float lineHeight = layoutLineHeightPx(renderer);
        if (text == null || text.isBlank() || layoutWidthPx <= 0f) {
            return lineHeight;
        }
        int lineCount = wrappedLineCount(text, layoutWidthPx, textBold, renderer);
        return lineCountSafe(lineCount) * lineHeight;
    }

    /**
     * Intrinsic height for wrapped plain text at a layout width.
     */
    public static float textHostWrapIntrinsicHeightPx(String text, float layoutWidthPx, boolean textBold, UIRenderer renderer) {
        return wrappedTextBlockHeightPx(text, layoutWidthPx, textBold, renderer);
    }

    /**
     * Line count used when turning {@link TextLineLayout#wrappedLineCount} into a vertical block (at least one line).
     */
    public static int lineCountSafe(int rawLineCount) {
        return Math.max(1, rawLineCount);
    }

    /**
     * Vertical size for a block with a known safe line count and per-line height.
     */
    public static float blockHeightForLines(int lineCountSafe, float lineHeightPx) {
        return lineCountSafe * lineHeightPx;
    }
}
