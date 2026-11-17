package cc.fascinated.fascinatedutils.gui.node.layout;

import cc.fascinated.fascinatedutils.gui.layout.PositionedNode;

/**
 * Stacks its children left to right with a fixed gap; children place themselves across the row with their vertical
 * box constraints.
 */
public class RowNode extends PositionedNode<RowNode> {

    /**
     * Creates a row.
     *
     * @param gap space between consecutive children in pixels
     */
    public RowNode(int gap) {
        rowGap(gap);
    }
}
