package cc.fascinated.fascinatedutils.gui.social.components;

import cc.fascinated.fascinatedutils.gui.core.UiFrameContext;

import cc.fascinated.fascinatedutils.gui.renderer.GuiRenderer;
import cc.fascinated.fascinatedutils.gui.renderer.RectCornerRoundMask;
import cc.fascinated.fascinatedutils.gui.renderer.UIRenderer;
import cc.fascinated.fascinatedutils.gui.themes.FascinatedGuiTheme;
import cc.fascinated.fascinatedutils.gui.widgets.FWidget;

public class SocialMessageBubbleWidget {
    public static FWidget build(Props props, float width) {
        return new FWidget() {
            @Override
            public float intrinsicHeightForColumn(UIRenderer measure, float widthBudget) {
                int lines = Math.max(1, countLines(props.content()));
                return 20f + lines * (measure.getFontCapHeight() + 2f);
            }

            @Override
            public void layout(UIRenderer measure, float lx, float ly, float lw, float lh) {
                setBounds(lx, ly, width, lh);
            }

            @Override
            protected void renderSelf(GuiRenderer graphics, UiFrameContext frame, float deltaSeconds) {
                float mouseX = frame.pointerX();
                float mouseY = frame.pointerY();
                float contentWidth = Math.max(90f, graphics.measureTextWidth(longestLine(props.content()), false) + 18f);
                float bubbleW = Math.min(w() * 0.78f, contentWidth);
                float bubbleX = props.ownMessage() ? x() + w() - bubbleW : x();
                graphics.drawText(props.headerText(), bubbleX, y(), FascinatedGuiTheme.INSTANCE.textMuted(), false, false);
                graphics.fillRoundedRect(bubbleX, y() + 10f, bubbleW, h() - 10f, 4f, props.ownMessage() ? 0xFF2D80F0 : 0xFF2C313C, RectCornerRoundMask.ALL);

                float textY = y() + 16f;
                for (String line : splitLines(props.content())) {
                    graphics.drawText(line, bubbleX + 8f, textY, 0xFFFFFFFF, false, false);
                    textY += graphics.getFontCapHeight() + 2f;
                }
            }
        };
    }

    private static int countLines(String value) {
        return splitLines(value).length;
    }

    private static String[] splitLines(String value) {
        if (value == null || value.isEmpty()) {
            return new String[]{""};
        }
        return value.split("\\R", -1);
    }

    private static String longestLine(String value) {
        String longest = "";
        for (String line : splitLines(value)) {
            if (line.length() > longest.length()) {
                longest = line;
            }
        }
        return longest;
    }

    public record Props(String headerText, String content, boolean ownMessage) {}
}
