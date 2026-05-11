package cc.fascinated.fascinatedutils.gui2.node;

import cc.fascinated.fascinatedutils.gui2.core.PositionedNode;
import cc.fascinated.fascinatedutils.gui2.render.RenderFrame;
import cc.fascinated.fascinatedutils.gui2.render.UiText;

import java.util.function.Supplier;

public class BadgeNode extends PositionedNode {

    private static final int PAD_H = 2;
    private static final int PAD_V = 1;

    private final Supplier<Integer> countSupplier;
    private int cap = 99;

    public BadgeNode(Supplier<Integer> countSupplier) {
        this.countSupplier = countSupplier;
    }

    /**
     * Sets the maximum count displayed before appending "+". Defaults to 99.
     *
     * @param cap the inclusive maximum value to show as a number
     */
    public BadgeNode setCap(int cap) {
        this.cap = cap;
        return this;
    }

    @Override
    public void layout(RenderFrame renderFrame, int parentX, int parentY, int parentWidth, int parentHeight) {
        int count = countSupplier.get();
        if (count <= 0) {
            setVisible(false);
            bounds().set(parentX, parentY, 0, 0);
            return;
        }
        setVisible(true);
        super.layout(renderFrame, parentX, parentY, parentWidth, parentHeight);
    }

    @Override
    protected int intrinsicWidth(RenderFrame renderFrame, int parentWidth) {
        int count = countSupplier.get();
        String text = count > cap ? cap + "+" : String.valueOf(count);
        return renderFrame.measureTextWidth(text, true) + PAD_H * 2;
    }

    @Override
    protected int intrinsicHeight(RenderFrame renderFrame, int parentHeight, int resolvedWidth) {
        return renderFrame.fontHeight() + PAD_V * 2;
    }

    @Override
    protected void renderSelf(RenderFrame renderFrame, float deltaSeconds) {
        int count = countSupplier.get();
        if (count <= 0) {
            return;
        }
        String text = count > cap ? cap + "+" : String.valueOf(count);
        int posX = bounds().positionX();
        int posY = bounds().positionY();
        int width = bounds().width();
        int height = bounds().height();
        renderFrame.drawRoundedRect(posX, posY, width, height, height / 2, renderFrame.theme().danger());
        UiText.of(text).color(renderFrame.theme().onDanger()).bold().draw(renderFrame, posX + PAD_H, posY + PAD_V);
    }
}
