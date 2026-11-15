package cc.fascinated.fascinatedutils.gui.node;

import cc.fascinated.fascinatedutils.gui.core.PositionedNode;
import cc.fascinated.fascinatedutils.gui.render.AlumiteGuiRenderer;

public class DividerNode extends PositionedNode<DividerNode> {
    @Override
    protected void renderSelf(AlumiteGuiRenderer renderFrame, float deltaSeconds) {
        int posX = bounds().positionX();
        int posY = bounds().positionY();
        int width = Math.max(1, bounds().width());
        int height = Math.max(1, bounds().height());
        renderFrame.drawRect(posX, posY, width, height, renderFrame.theme().divider());
    }
}