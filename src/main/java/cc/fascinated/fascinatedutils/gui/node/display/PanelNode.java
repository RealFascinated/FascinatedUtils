package cc.fascinated.fascinatedutils.gui.node.display;

import cc.fascinated.fascinatedutils.gui.layout.PositionedNode;
import cc.fascinated.fascinatedutils.gui.render.AlumiteGuiRenderer;
import cc.fascinated.fascinatedutils.gui.theme.UiTheme;

/**
 * A square-cornered panel with the theme's panel fill and border.
 */
public class PanelNode extends PositionedNode<PanelNode> {
    private static final int BORDER_THICKNESS = 1;

    @Override
    protected void renderSelf(AlumiteGuiRenderer renderFrame, float deltaSeconds) {
        UiTheme theme = renderFrame.theme();
        RectNode.drawBox(renderFrame, bounds().positionX(), bounds().positionY(), bounds().width(), bounds().height(), 0, theme.panelFill(), theme.panelBorder(), BORDER_THICKNESS);
    }
}
