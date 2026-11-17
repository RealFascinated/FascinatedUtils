package cc.fascinated.fascinatedutils.gui.node.display;

import cc.fascinated.fascinatedutils.gui.layout.PositionedNode;
import cc.fascinated.fascinatedutils.gui.render.AlumiteGuiRenderer;
import cc.fascinated.fascinatedutils.gui.render.RectCornerRoundMask;

import java.util.function.IntSupplier;

/**
 * A filled box with an optional ring border, rounded corners, per-corner squaring and an editor outline. Unlike
 * {@code RectNode} it can square individual corners, so boxes that touch can read as one shape. A zero colour skips
 * that part.
 */
public class PanelBoxNode extends PositionedNode<PanelBoxNode> {
    private static final int DOTTED_OUTLINE_ARGB = 0x80FFFFFF;
    private static final float DOTTED_OUTLINE_DOT_SIZE = 1f;
    private static final float DOTTED_OUTLINE_STEP = 3f;

    private int fillArgb;
    private int borderArgb;
    private float borderThickness = 1f;
    private float cornerRadius;
    private boolean dottedOutline;
    private IntSupplier squaredCorners = () -> RectCornerRoundMask.NONE;

    public PanelBoxNode setFillArgb(int fillArgb) {
        this.fillArgb = fillArgb;
        return this;
    }

    public PanelBoxNode setBorderArgb(int borderArgb) {
        this.borderArgb = borderArgb;
        return this;
    }

    public PanelBoxNode setBorderThickness(float borderThickness) {
        this.borderThickness = Math.max(1f, borderThickness);
        return this;
    }

    public PanelBoxNode setCornerRadius(float cornerRadius) {
        this.cornerRadius = Math.max(0f, cornerRadius);
        return this;
    }

    /**
     * Draws a dotted outline when the box has neither fill nor border, so an otherwise invisible box stays visible.
     *
     * @param dottedOutline whether to draw the outline
     * @return this node
     */
    public PanelBoxNode setDottedOutline(boolean dottedOutline) {
        this.dottedOutline = dottedOutline;
        return this;
    }

    /**
     * Sets which corners are drawn square instead of rounded; read at draw time because it can change after layout.
     *
     * @param squaredCorners supplies a {@link RectCornerRoundMask} bit set of the corners to square
     * @return this node
     */
    public PanelBoxNode setSquaredCorners(IntSupplier squaredCorners) {
        this.squaredCorners = squaredCorners;
        return this;
    }

    @Override
    protected void renderSelf(AlumiteGuiRenderer renderFrame, float deltaSeconds) {
        float boxX = bounds().positionX();
        float boxY = bounds().positionY();
        float boxWidth = bounds().width();
        float boxHeight = bounds().height();
        if (fillArgb == 0 && borderArgb == 0) {
            if (dottedOutline && boxWidth >= DOTTED_OUTLINE_DOT_SIZE && boxHeight >= DOTTED_OUTLINE_DOT_SIZE) {
                renderFrame.pushTranslate(boxX, boxY);
                renderFrame.drawDottedOutline(boxWidth, boxHeight, DOTTED_OUTLINE_DOT_SIZE, DOTTED_OUTLINE_STEP, DOTTED_OUTLINE_ARGB);
                renderFrame.popTranslate();
            }
            return;
        }
        if (cornerRadius > 0f) {
            int roundedMask = RectCornerRoundMask.ALL & ~squaredCorners.getAsInt();
            if (fillArgb != 0) {
                renderFrame.fillRoundedRect(boxX, boxY, boxWidth, boxHeight, cornerRadius, fillArgb, roundedMask);
            }
            if (borderArgb != 0) {
                renderFrame.fillRoundedRectBorderRing(boxX, boxY, boxWidth, boxHeight, cornerRadius, borderThickness, borderArgb, roundedMask);
            }
        }
        else {
            if (fillArgb != 0) {
                renderFrame.drawRect(boxX, boxY, boxWidth, boxHeight, fillArgb);
            }
            if (borderArgb != 0) {
                renderFrame.drawBorder(boxX, boxY, boxWidth, boxHeight, borderThickness, borderArgb);
            }
        }
    }
}
