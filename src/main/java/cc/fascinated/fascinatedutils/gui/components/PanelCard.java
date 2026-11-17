package cc.fascinated.fascinatedutils.gui.components;

import cc.fascinated.fascinatedutils.gui.layout.PositionedNode;
import cc.fascinated.fascinatedutils.gui.layout.SpacerNode;
import cc.fascinated.fascinatedutils.gui.node.display.CardNode;
import cc.fascinated.fascinatedutils.gui.node.display.TextNode;
import cc.fascinated.fascinatedutils.gui.theme.UiMetrics;
import cc.fascinated.fascinatedutils.gui.theme.UiTheme;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Window-level card used as the body of a screen: panel fill, subtle border and a title header with trailing actions.
 */
public class PanelCard extends CardNode {

    /**
     * Creates a card with the panel fill and subtle border.
     */
    public PanelCard() {
        setFillResolver(UiTheme::panelFill);
        setBorderResolver(UiTheme::borderSubtle);
    }

    /**
     * Fills the header with a bold title, a spacer and the trailing actions pushed to the right edge.
     *
     * @param title    supplies the title text
     * @param trailing adds the action nodes shown after the title
     * @return this card
     */
    public PanelCard setTitleHeader(Supplier<String> title, Consumer<PositionedNode<?>> trailing) {
        setHeader(header -> {
            header.addChild(new TextNode(title).setBold(true));
            header.addChild(new SpacerNode());
            header.right(UiMetrics.SPACE_XS);
            trailing.accept(header);
        });
        return this;
    }
}
