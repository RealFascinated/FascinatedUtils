package cc.fascinated.fascinatedutils.gui.node.display;

import cc.fascinated.fascinatedutils.gui.layout.PositionedNode;
import cc.fascinated.fascinatedutils.gui.render.AlumiteGuiRenderer;
import cc.fascinated.fascinatedutils.gui.theme.UiMetrics;
import cc.fascinated.fascinatedutils.gui.theme.UiTheme;

/**
 * A raised, bordered bubble sized to a single line of text.
 */
public class TooltipNode extends PositionedNode<TooltipNode> {
    private static final int PADDING = 4;

    private String text = "";

    /**
     * Sets the text shown in the bubble.
     *
     * @param text the text to show; null is treated as empty
     * @return this node
     */
    public TooltipNode setText(String text) {
        String next = text == null ? "" : text;
        if (!next.equals(this.text)) {
            this.text = next;
            markLayoutDirty();
        }
        return this;
    }

    @Override
    protected int intrinsicWidth(AlumiteGuiRenderer renderFrame, int parentWidth) {
        return renderFrame.measureTextWidth(text, false) + PADDING * 2;
    }

    @Override
    protected int intrinsicHeight(AlumiteGuiRenderer renderFrame, int parentHeight, int resolvedWidth) {
        return renderFrame.fontHeight() + PADDING * 2;
    }

    @Override
    protected void renderSelf(AlumiteGuiRenderer renderFrame, float deltaSeconds) {
        UiTheme theme = renderFrame.theme();
        renderFrame.drawRoundedRectFrame(bounds().positionX(), bounds().positionY(), bounds().width(), bounds().height(), UiMetrics.RADIUS, theme.borderSubtle(), theme.surfaceRaised(), 1);
        renderFrame.drawText(text, bounds().positionX() + PADDING, bounds().positionY() + PADDING, theme.textPrimary(), false, false);
    }
}
