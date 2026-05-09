package cc.fascinated.fascinatedutils.gui2.node.social.chat;

import cc.fascinated.fascinatedutils.gui2.core.PositionedNode;
import cc.fascinated.fascinatedutils.gui2.node.RectNode;
import cc.fascinated.fascinatedutils.gui2.node.TextNode;
import cc.fascinated.fascinatedutils.gui2.render.RenderFrame;
import cc.fascinated.fascinatedutils.gui2.theme.UiTheme;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

class ChatDaySeparatorNode extends PositionedNode {

    private static final DateTimeFormatter LABEL_FORMATTER = DateTimeFormatter.ofPattern("MMMM d, yyyy");
    private static final int PAD_V = 6;
    private static final int LINE_GAP = 8;

    private final LocalDate date;
    private final RectNode leftLine;
    private final RectNode rightLine;
    private final TextNode dateLabel;

    ChatDaySeparatorNode(LocalDate date) {
        this.date = date;
        fullWidth();

        leftLine = new RectNode();
        leftLine.setFillResolver(UiTheme::divider);

        rightLine = new RectNode();
        rightLine.setFillResolver(UiTheme::divider);

        dateLabel = new TextNode(this::formatLabel)
                .setColorResolver(UiTheme::textMuted)
                .setTextAlign(0.5f, 0.5f);
        dateLabel.fullWidth();

        addChild(leftLine);
        addChild(rightLine);
        addChild(dateLabel);
    }

    @Override
    public void layout(RenderFrame renderFrame, int parentX, int parentY, int parentWidth, int parentHeight) {
        int height = PAD_V * 2 + renderFrame.fontHeight();
        bounds().set(parentX, parentY, parentWidth, height);

        dateLabel.layout(renderFrame, parentX, parentY + PAD_V, parentWidth, renderFrame.fontHeight());

        int textWidth = renderFrame.measureTextWidth(formatLabel(), false);
        int textX = parentX + (parentWidth - textWidth) / 2;
        int lineThickness = renderFrame.theme().separatorThickness();
        int centerY = parentY + PAD_V + renderFrame.fontHeight() / 2;

        int leftLineW = textX - LINE_GAP - parentX;
        leftLine.setVisible(leftLineW > 0);
        if (leftLineW > 0) {
            leftLine.bounds().set(parentX, centerY, leftLineW, lineThickness);
        }

        int textRight = textX + textWidth;
        int rightLineX = textRight + LINE_GAP;
        int rightLineW = parentX + parentWidth - rightLineX;
        rightLine.setVisible(rightLineW > 0);
        if (rightLineW > 0) {
            rightLine.bounds().set(rightLineX, centerY, rightLineW, lineThickness);
        }
    }

    private String formatLabel() {
        LocalDate today = LocalDate.now(ZoneId.systemDefault());
        if (date.equals(today)) {
            return "Today";
        }
        if (date.equals(today.minusDays(1))) {
            return "Yesterday";
        }
        return LABEL_FORMATTER.format(date);
    }
}
