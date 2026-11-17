package cc.fascinated.fascinatedutils.gui.components;

import cc.fascinated.fascinatedutils.gui.node.display.CardNode;
import cc.fascinated.fascinatedutils.gui.theme.UiTheme;

/**
 * Inset card used for list rows and tiles: card surface fill with a subtle border.
 */
public class SurfaceCard extends CardNode {

    /**
     * Creates a card with the surface fill and subtle border.
     */
    public SurfaceCard() {
        setFillResolver(UiTheme::surfaceCard);
        setBorderResolver(UiTheme::borderSubtle);
    }
}
