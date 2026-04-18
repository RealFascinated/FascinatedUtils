package cc.fascinated.fascinatedutils.gui.widgets;

import cc.fascinated.fascinatedutils.common.ColorUtils;
import cc.fascinated.fascinatedutils.gui.core.Align;
import cc.fascinated.fascinatedutils.gui.core.TextLayoutMetrics;
import cc.fascinated.fascinatedutils.gui.core.TextLineLayout;
import cc.fascinated.fascinatedutils.gui.core.TextOverflow;
import cc.fascinated.fascinatedutils.gui.renderer.GuiRenderer;
import cc.fascinated.fascinatedutils.gui.renderer.UIRenderer;
import lombok.Setter;

public class FLabelWidget extends FWidget {
    private String text = "";
    @Setter
    private int colorArgb;
    @Setter
    private boolean textBold;
    private TextOverflow overflow = TextOverflow.VISIBLE;
    @Setter
    private boolean centered;
    private Align alignX = Align.START;
    private Align alignY = Align.START;
    @Setter
    private boolean showCaret;
    private String caretPrefix = "";

    private static String ellipsize(String source, boolean bold, float maxWidth, UIRenderer measure) {
        if (source == null || source.isEmpty() || maxWidth <= 1f) {
            return source;
        }
        if (measure.measureTextWidth(source, bold) <= maxWidth) {
            return source;
        }
        String ellipsis = "â€¦";
        int lowBound = 0;
        int highBound = source.length();
        while (lowBound < highBound) {
            int middle = (lowBound + highBound + 1) >>> 1;
            String candidate = source.substring(0, middle) + ellipsis;
            if (measure.measureTextWidth(candidate, bold) <= maxWidth) {
                lowBound = middle;
            }
            else {
                highBound = middle - 1;
            }
        }
        return source.substring(0, lowBound) + ellipsis;
    }

    public void setText(String text) {
        this.text = text == null ? "" : text;
    }

    public void setOverflow(TextOverflow overflow) {
        this.overflow = overflow == null ? TextOverflow.VISIBLE : overflow;
    }

    public void setAlignX(Align alignX) {
        this.alignX = alignX == null ? Align.START : alignX;
    }

    public void setAlignY(Align alignY) {
        this.alignY = alignY == null ? Align.START : alignY;
    }

    public void setCaretPrefix(String caretPrefix) {
        this.caretPrefix = caretPrefix == null ? "" : caretPrefix;
    }

    @Override
    public void layout(UIRenderer measure, float layoutX, float layoutY, float layoutWidth, float layoutHeight) {
        setBounds(layoutX, layoutY, layoutWidth, layoutHeight);
    }

    @Override
    public float intrinsicHeightForColumn(UIRenderer measure, float widthBudget) {
        if (overflow == TextOverflow.WRAP) {
            return TextLayoutMetrics.wrappedTextBlockHeightPx(text, widthBudget, textBold, measure);
        }
        return TextLayoutMetrics.layoutLineHeightPx(measure);
    }

    @Override
    public float intrinsicWidthForRow(UIRenderer measure, float heightBudget) {
        return measure.measureTextWidth(text, textBold);
    }

    @Override
    protected void renderSelf(GuiRenderer graphics, float mouseX, float mouseY, float deltaSeconds) {
        float lineBlockH = TextLayoutMetrics.layoutLineHeightPx(graphics);
        float textOriginY = y();
        if (alignY == Align.CENTER) {
            textOriginY = y() + Math.max(0f, h() - lineBlockH) * 0.5f;
        }
        else if (alignY == Align.END) {
            textOriginY = y() + Math.max(0f, h() - lineBlockH);
        }
        String drawText = overflow == TextOverflow.ELLIPSIS ? ellipsize(text, textBold, w(), graphics) : text;
        float textWidth = graphics.measureTextWidth(drawText, textBold);
        float textOriginX = x();
        if (alignX == Align.CENTER) {
            textOriginX = x() + Math.max(0f, w() - textWidth) * 0.5f;
        }
        else if (alignX == Align.END) {
            textOriginX = x() + Math.max(0f, w() - textWidth);
        }
        boolean clipBox = overflow == TextOverflow.CLIP || overflow == TextOverflow.ELLIPSIS;
        if (clipBox) {
            graphics.pushClip(x(), y(), w(), h());
        }
        String colorTag = "<color:" + ColorUtils.rgbHex(colorArgb) + ">";
        String boldStart = textBold ? "<b>" : "";
        String boldEnd = textBold ? "</b>" : "";
        if (overflow == TextOverflow.WRAP) {
            float wrapBudget = TextLayoutMetrics.wrapBudgetForLayoutWidthPx(w());
            java.util.List<String> lines = TextLineLayout.wrapLines(text, wrapBudget, segment -> graphics.measureTextWidth(segment, textBold));
            float lineHeight = TextLayoutMetrics.layoutLineHeightPx(graphics);
            float cursorY = textOriginY;
            for (String line : lines) {
                graphics.drawMiniMessageText(colorTag + boldStart + line + boldEnd + "</color>", textOriginX, cursorY, false);
                cursorY += lineHeight;
            }
        }
        else if (centered) {
            graphics.drawMiniMessageText(colorTag + boldStart + drawText + boldEnd + "</color>", x() + w() * 0.5f - textWidth * 0.5f, textOriginY, false);
        }
        else {
            graphics.drawMiniMessageText(colorTag + boldStart + drawText + boldEnd + "</color>", textOriginX, textOriginY, false);
        }
        if (showCaret) {
            long blink = System.currentTimeMillis() / 530L;
            if ((blink & 1L) == 0L) {
                float caretX;
                if (caretPrefix.isEmpty()) {
                    caretX = textOriginX;
                }
                else if (caretPrefix.equals(drawText)) {
                    caretX = textOriginX + textWidth;
                }
                else {
                    caretX = textOriginX + graphics.measureTextWidth(caretPrefix, textBold);
                }
                float caretTop = textOriginY;
                graphics.drawRect(caretX, caretTop, 1f, lineBlockH, colorArgb);
            }
        }
        if (clipBox) {
            graphics.popClip();
        }
    }
}
