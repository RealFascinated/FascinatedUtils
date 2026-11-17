package cc.fascinated.fascinatedutils.gui.components;

import cc.fascinated.fascinatedutils.gui.layout.PositionedNode;
import cc.fascinated.fascinatedutils.gui.node.input.TextInputNode;
import cc.fascinated.fascinatedutils.gui.theme.UiMetrics;

/**
 * Fixed-width, control-height slot that hosts a text input inside a row.
 */
public class InputHost extends PositionedNode<InputHost> {

    /**
     * Creates the slot.
     *
     * @param input the input to host
     * @param width the slot width in pixels
     */
    public InputHost(TextInputNode<?> input, int width) {
        width(width).height(UiMetrics.CONTROL_HEIGHT);
        addChild(input);
    }
}
