package cc.fascinated.fascinatedutils.gui.components;

import cc.fascinated.fascinatedutils.gui.layout.PositionedNode;
import cc.fascinated.fascinatedutils.gui.node.layout.ScrollColumnNode;

/**
 * Full-width slot of a fixed height that hosts a scrolling column.
 */
public class ScrollHost extends PositionedNode<ScrollHost> {

    /**
     * Creates the slot.
     *
     * @param scroll the scrolling column to host
     * @param height the slot height in pixels
     */
    public ScrollHost(ScrollColumnNode scroll, int height) {
        fullWidth().height(height);
        addChild(scroll);
    }
}
