package cc.fascinated.fascinatedutils.gui2.node;

import cc.fascinated.fascinatedutils.gui2.core.PositionedNode;
import cc.fascinated.fascinatedutils.gui2.render.RenderFrame;

public class DividerNode extends PositionedNode<DividerNode> {
    @Override
    protected void renderSelf(RenderFrame renderFrame, float deltaSeconds) {
        int posX = bounds().positionX();
        int posY = bounds().positionY();
        int width = Math.max(1, bounds().width());
        int height = Math.max(1, bounds().height());
        renderFrame.drawRect(posX, posY, width, height, renderFrame.theme().divider());
    }
}