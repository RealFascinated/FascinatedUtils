package cc.fascinated.fascinatedutils.gui.node;

import cc.fascinated.fascinatedutils.gui.core.PositionedNode;
import cc.fascinated.fascinatedutils.gui.render.GuiRenderer;

public class PanelNode extends PositionedNode<PanelNode> {

    @Override
    protected void renderSelf(GuiRenderer renderFrame, float deltaSeconds) {
        int posX = bounds().positionX();
        int posY = bounds().positionY();
        int width = bounds().width();
        int height = bounds().height();
        renderFrame.drawRect(posX, posY, width, height, renderFrame.theme().panelFill());
        renderFrame.drawBorder(posX, posY, width, height, 1, renderFrame.theme().panelBorder());
    }
}
