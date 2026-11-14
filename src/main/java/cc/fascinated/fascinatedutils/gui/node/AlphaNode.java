package cc.fascinated.fascinatedutils.gui.node;

import cc.fascinated.fascinatedutils.gui.core.PositionedNode;
import cc.fascinated.fascinatedutils.gui.render.GuiRenderer;

import java.util.function.Supplier;

/**
 * Container that renders its subtree with an alpha multiplier and skips drawing when fully transparent.
 */
public class AlphaNode extends PositionedNode<AlphaNode> {

    private final Supplier<Float> alphaSupplier;

    public AlphaNode(Supplier<Float> alphaSupplier) {
        this.alphaSupplier = alphaSupplier;
    }

    @Override
    public void render(GuiRenderer renderFrame, float deltaSeconds) {
        float alpha = alphaSupplier.get();
        if (alpha <= 0.01f) {
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
