package cc.fascinated.fascinatedutils.gui2.node;

import cc.fascinated.fascinatedutils.gui2.core.PositionedNode;
import cc.fascinated.fascinatedutils.gui2.render.RenderFrame;

/**
 * Simple rectangular panel with fill and border.
 */
public class PanelNode extends PositionedNode {
    private int borderThickness = 1;
    private Integer fillColorOverrideArgb;
    private Integer borderColorOverrideArgb;

    public PanelNode setFillColorArgb(int fillColorArgb) {
        this.fillColorOverrideArgb = fillColorArgb;
        return this;
    }

    public PanelNode setBorderColorArgb(int borderColorArgb) {
        this.borderColorOverrideArgb = borderColorArgb;
        return this;
    }

    public PanelNode setBorderThickness(int borderThickness) {
        this.borderThickness = Math.max(1, borderThickness);
        return this;
    }

    @Override
    protected void renderSelf(RenderFrame renderFrame, float deltaSeconds) {
        int posX = bounds().positionX();
        int posY = bounds().positionY();
        int width = bounds().width();
        int height = bounds().height();
        int fillColorArgb = fillColorOverrideArgb != null ? fillColorOverrideArgb : renderFrame.theme().panelFill();
        int borderColorArgb = borderColorOverrideArgb != null ? borderColorOverrideArgb : renderFrame.theme().panelBorder();
        renderFrame.drawRect(posX, posY, width, height, fillColorArgb);
        renderFrame.drawBorder(posX, posY, width, height, borderThickness, borderColorArgb);
    }
}
