package cc.fascinated.fascinatedutils.gui.node.display;

import cc.fascinated.fascinatedutils.gui.layout.PositionedNode;
import cc.fascinated.fascinatedutils.gui.render.AlumiteGuiRenderer;
import cc.fascinated.fascinatedutils.gui.theme.UiTheme;

import java.util.function.IntSupplier;
import java.util.function.ToIntFunction;

public class RectNode extends PositionedNode<RectNode> {

    private ToIntFunction<UiTheme> fillResolver;
    private ToIntFunction<UiTheme> borderResolver;
    private int borderThickness = 1;
    private int cornerRadius = 0;

    public RectNode setFillArgb(int argb) {
        fillResolver = theme -> argb;
        return this;
    }

    public RectNode setFillResolver(ToIntFunction<UiTheme> resolver) {
        fillResolver = resolver;
        return this;
    }

    public RectNode setFillSupplier(IntSupplier supplier) {
        fillResolver = supplier == null ? null : theme -> supplier.getAsInt();
        return this;
    }

    public RectNode setBorderArgb(int argb) {
        borderResolver = theme -> argb;
        return this;
    }

    public RectNode setBorderResolver(ToIntFunction<UiTheme> resolver) {
        borderResolver = resolver;
        return this;
    }

    public RectNode setBorderThickness(int borderThickness) {
        this.borderThickness = Math.max(1, borderThickness);
        return this;
    }

    public RectNode setCornerRadius(int cornerRadius) {
        this.cornerRadius = Math.max(0, cornerRadius);
        return this;
    }

    /**
     * Draws a filled box with an inside border; a zero colour skips that part and a zero radius draws square corners.
     * Shared by every node that paints a plain box so they all stay consistent.
     *
     * @param renderFrame     the active renderer
     * @param positionX       left edge
     * @param positionY       top edge
     * @param width           box width
     * @param height          box height
     * @param cornerRadius    corner radius, zero for square corners
     * @param fillArgb        fill colour, zero for none
     * @param borderArgb      border colour, zero for none
     * @param borderThickness border thickness in pixels
     */
    static void drawBox(AlumiteGuiRenderer renderFrame, int positionX, int positionY, int width, int height, int cornerRadius, int fillArgb, int borderArgb, int borderThickness) {
        if (fillArgb != 0) {
            if (cornerRadius > 0) {
                renderFrame.drawRoundedRect(positionX, positionY, width, height, cornerRadius, fillArgb);
            } else {
                renderFrame.drawRect(positionX, positionY, width, height, fillArgb);
            }
        }
        if (borderArgb != 0) {
            if (cornerRadius > 0) {
                renderFrame.drawRoundedRectFrame(positionX, positionY, width, height, cornerRadius, borderArgb, 0, borderThickness);
            } else {
                renderFrame.drawBorder(positionX, positionY, width, height, borderThickness, borderArgb);
            }
        }
    }

    @Override
    protected void renderSelf(AlumiteGuiRenderer renderFrame, float deltaSeconds) {
        UiTheme theme = renderFrame.theme();
        int fill = fillResolver != null ? fillResolver.applyAsInt(theme) : 0;
        int border = borderResolver != null ? borderResolver.applyAsInt(theme) : 0;
        drawBox(renderFrame, bounds().positionX(), bounds().positionY(), bounds().width(), bounds().height(), cornerRadius, fill, border, borderThickness);
    }
}
