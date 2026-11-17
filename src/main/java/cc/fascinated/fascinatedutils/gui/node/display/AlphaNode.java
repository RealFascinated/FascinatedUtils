package cc.fascinated.fascinatedutils.gui.node.display;

import cc.fascinated.fascinatedutils.gui.layout.PositionedNode;
import cc.fascinated.fascinatedutils.gui.render.AlumiteGuiRenderer;

import java.util.function.DoubleSupplier;

/**
 * Container that renders its subtree with an alpha multiplier. Below a small threshold the subtree is not drawn and
 * no longer receives pointer events or focus.
 */
public class AlphaNode extends PositionedNode<AlphaNode> {
    private static final double VISIBLE_ALPHA_THRESHOLD = 0.01;

    private final DoubleSupplier alphaSupplier;

    public AlphaNode(DoubleSupplier alphaSupplier) {
        this.alphaSupplier = alphaSupplier;
    }

    @Override
    public boolean acceptsInput() {
        return alphaSupplier.getAsDouble() > VISIBLE_ALPHA_THRESHOLD;
    }

    @Override
    public void render(AlumiteGuiRenderer renderFrame, float deltaSeconds) {
        float alpha = (float) alphaSupplier.getAsDouble();
        if (alpha <= VISIBLE_ALPHA_THRESHOLD) {
            notifySubtreeViewportLeave();
            return;
        }
        renderFrame.pushAlpha(alpha);
        try {
            super.render(renderFrame, deltaSeconds);
        }
        finally {
            renderFrame.popAlpha();
        }
    }
}
