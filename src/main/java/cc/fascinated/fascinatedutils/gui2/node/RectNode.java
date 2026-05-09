package cc.fascinated.fascinatedutils.gui2.node;

import cc.fascinated.fascinatedutils.gui2.core.PositionedNode;
import cc.fascinated.fascinatedutils.gui2.render.RenderFrame;
import cc.fascinated.fascinatedutils.gui2.theme.UiTheme;
import cc.fascinated.fascinatedutils.gui2.theme.UiThemeRepository;

import java.util.function.Function;
import java.util.function.Supplier;

public class RectNode extends PositionedNode {

    private Supplier<Integer> fillSupplier;
    private Supplier<Integer> borderSupplier;
    private int borderThickness = 1;
    private int cornerRadius = 0;

    public RectNode setFillArgb(int argb) {
        fillSupplier = () -> argb;
        return this;
    }

    public RectNode setFillResolver(Function<UiTheme, Integer> resolver) {
        fillSupplier = () -> resolver.apply(UiThemeRepository.get());
        return this;
    }

    public RectNode setFillSupplier(Supplier<Integer> supplier) {
        fillSupplier = supplier;
        return this;
    }

    public RectNode setBorderArgb(int argb) {
        borderSupplier = () -> argb;
        return this;
    }

    public RectNode setBorderResolver(Function<UiTheme, Integer> resolver) {
        borderSupplier = () -> resolver.apply(UiThemeRepository.get());
        return this;
    }

    public RectNode setBorderSupplier(Supplier<Integer> supplier) {
        borderSupplier = supplier;
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

    @Override
    protected void renderSelf(RenderFrame renderFrame, float deltaSeconds) {
        int posX = bounds().positionX();
        int posY = bounds().positionY();
        int width = bounds().width();
        int height = bounds().height();

        int fill = fillSupplier != null ? fillSupplier.get() : 0;
        if (fill != 0) {
            if (cornerRadius > 0) {
                renderFrame.drawRoundedRect(posX, posY, width, height, cornerRadius, fill);
            } else {
                renderFrame.drawRect(posX, posY, width, height, fill);
            }
        }

        if (borderSupplier != null) {
            int border = borderSupplier.get();
            if (border != 0) {
                if (cornerRadius > 0) {
                    renderFrame.drawRoundedRectFrame(posX, posY, width, height, cornerRadius, border, 0, borderThickness);
                } else {
                    renderFrame.drawBorder(posX, posY, width, height, borderThickness, border);
                }
            }
        }
    }
}
