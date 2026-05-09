package cc.fascinated.fascinatedutils.gui2.node;

import cc.fascinated.fascinatedutils.gui2.core.PositionedNode;
import cc.fascinated.fascinatedutils.gui2.render.RenderFrame;
import cc.fascinated.fascinatedutils.gui2.theme.UiTheme;
import cc.fascinated.fascinatedutils.gui2.theme.UiThemeRepository;

import java.util.function.Function;
import java.util.function.Supplier;

public class CardNode extends PositionedNode {

    private static final int DEFAULT_CORNER_RADIUS = 6;

    private int cornerRadius = DEFAULT_CORNER_RADIUS;
    private Supplier<Integer> fillSupplier;
    private Supplier<Integer> borderSupplier;
    private int borderThickness = 1;

    public CardNode() {
        fillSupplier = () -> UiThemeRepository.get().panelFill();
        borderSupplier = () -> UiThemeRepository.get().panelBorder();
    }

    public CardNode setCornerRadius(int cornerRadius) {
        this.cornerRadius = Math.max(0, cornerRadius);
        return this;
    }

    public CardNode setFillArgb(int argb) {
        fillSupplier = () -> argb;
        return this;
    }

    public CardNode setFillResolver(Function<UiTheme, Integer> resolver) {
        fillSupplier = () -> resolver.apply(UiThemeRepository.get());
        return this;
    }

    public CardNode setNoFill() {
        fillSupplier = null;
        return this;
    }

    public CardNode setBorderArgb(int argb) {
        borderSupplier = () -> argb;
        return this;
    }

    public CardNode setBorderResolver(Function<UiTheme, Integer> resolver) {
        borderSupplier = () -> resolver.apply(UiThemeRepository.get());
        return this;
    }

    public CardNode setNoBorder() {
        borderSupplier = null;
        return this;
    }

    public CardNode setBorderThickness(int borderThickness) {
        this.borderThickness = Math.max(1, borderThickness);
        return this;
    }

    @Override
    protected void renderSelf(RenderFrame renderFrame, float deltaSeconds) {
        int posX = bounds().positionX();
        int posY = bounds().positionY();
        int width = bounds().width();
        int height = bounds().height();

        int fill = fillSupplier != null ? fillSupplier.get() : 0;
        int border = borderSupplier != null ? borderSupplier.get() : 0;

        if (fill != 0 || border != 0) {
            renderFrame.drawRoundedRectFrame(posX, posY, width, height, cornerRadius, border, fill, borderThickness);
        }
    }
}
