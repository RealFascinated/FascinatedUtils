package cc.fascinated.fascinatedutils.gui.node.layout;

import cc.fascinated.fascinatedutils.gui.layout.PositionedNode;

/**
 * Stacks its children top to bottom with a fixed gap; children place themselves across the column with their
 * horizontal box constraints.
 */
public class ColumnNode extends PositionedNode<ColumnNode> {

    /**
     * Creates a column.
     *
     * @param gap space between consecutive children in pixels
     */
    public ColumnNode(int gap) {
        columnGap(gap);
    }
}
