package cc.fascinated.fascinatedutils.gui2.node.social;

import cc.fascinated.fascinatedutils.gui2.core.PositionedNode;
import cc.fascinated.fascinatedutils.gui2.render.RenderFrame;

/**
 * Muted uppercase section divider label used to group entries in the friends list
 * (e.g. "Incoming Requests", "Sent Requests").
 */
public class SocialSectionLabelNode extends PositionedNode {

    private String label;

    public SocialSectionLabelNode(String label) {
        this.label = label;
        height(20).fullWidth();
    }

    public SocialSectionLabelNode setLabel(String label) {
        this.label = label;
        return this;
    }

    @Override
    protected void renderSelf(RenderFrame renderFrame, float deltaSeconds) {
        int posX = bounds().positionX();
        int posY = bounds().positionY();
        int height = bounds().height();

        String text = label != null ? label.toUpperCase() : "";
        int textY = posY + (height - renderFrame.fontHeight()) / 2;
        renderFrame.drawText(text, posX + 4, textY, renderFrame.theme().textMuted(), false, true);
    }
}
