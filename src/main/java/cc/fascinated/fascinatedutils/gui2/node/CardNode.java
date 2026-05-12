package cc.fascinated.fascinatedutils.gui2.node;

import cc.fascinated.fascinatedutils.gui2.core.PositionedNode;
import cc.fascinated.fascinatedutils.gui2.core.UiNode;
import cc.fascinated.fascinatedutils.gui2.render.ClipRegion;
import cc.fascinated.fascinatedutils.gui2.render.RenderFrame;
import cc.fascinated.fascinatedutils.gui2.theme.UiTheme;
import cc.fascinated.fascinatedutils.gui2.theme.UiThemeRepository;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class CardNode extends PositionedNode<CardNode> {

    public static final int HEADER_HEIGHT = 28;
    public static final int FOOTER_HEIGHT = 28;

    private static final int CORNER_RADIUS = 6;
    private static final int CONTENT_INSET_X = 12;

    private boolean rounded = true;
    private Supplier<Integer> fillSupplier;
    private Supplier<Integer> borderSupplier;
    private int borderThickness = 1;
    private PositionedNode<?> headerNode;
    private PositionedNode<?> footerNode;
    private PositionedNode<?> contentsNode;

    public CardNode() {
        fillSupplier = () -> UiThemeRepository.get().panelFill();
        borderSupplier = () -> UiThemeRepository.get().panelBorder();
    }

    public CardNode setRounded(boolean rounded) {
        this.rounded = rounded;
        return this;
    }

    public CardNode setHeader(Consumer<PositionedNode<?>> builder) {
        if (headerNode == null) {
            headerNode = new CenteredRowNode().left(CONTENT_INSET_X).right(CONTENT_INSET_X).top(0).height(HEADER_HEIGHT).rowGap(8);
            addChild(headerNode);
        }
        builder.accept(headerNode);
        return this;
    }

    public CardNode setFooter(Consumer<PositionedNode<?>> builder) {
        if (footerNode == null) {
            footerNode = new CenteredRowNode().left(CONTENT_INSET_X).right(CONTENT_INSET_X).bottom(0).height(FOOTER_HEIGHT).rowGap(8);
            addChild(footerNode);
        }
        builder.accept(footerNode);
        return this;
    }

    public CardNode setContents(Consumer<PositionedNode<?>> builder) {
        if (contentsNode == null) {
            int topInset = headerNode != null ? HEADER_HEIGHT : 0;
            int bottomInset = footerNode != null ? FOOTER_HEIGHT : 0;
            PositionedNode<?> anchorNode = new PositionedNode<>().left(0).right(0).top(topInset).bottom(bottomInset);
            contentsNode = new PositionedNode<>().full();
            anchorNode.addChild(contentsNode);
            addChild(anchorNode);
        }
        builder.accept(contentsNode);
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
        int cornerRadius = rounded ? CORNER_RADIUS : 0;

        int fill = fillSupplier != null ? fillSupplier.get() : 0;
        int border = borderSupplier != null ? borderSupplier.get() : 0;

        if (headerNode == null && footerNode == null) {
            if (fill != 0 || border != 0) {
                renderFrame.drawRoundedRectFrame(posX, posY, width, height, cornerRadius, border, fill, borderThickness);
            }
            return;
        }

        if (fill != 0) {
            renderFrame.drawRoundedRect(posX, posY, width, height, cornerRadius, fill);
        }

        int accentFill = renderFrame.theme().panelHeaderFill();
        int divider = renderFrame.theme().divider();
        int sepThickness = renderFrame.theme().separatorThickness();

        if (headerNode != null) {
            if (accentFill != 0) {
                renderFrame.pushClip(new ClipRegion(posX, posY, width, HEADER_HEIGHT));
                renderFrame.drawRoundedRect(posX, posY, width, height, cornerRadius, accentFill);
                renderFrame.popClip();
            }
            if (divider != 0) {
                renderFrame.drawRect(posX, posY + HEADER_HEIGHT, width, sepThickness, divider);
            }
        }

        if (footerNode != null) {
            if (accentFill != 0) {
                renderFrame.pushClip(new ClipRegion(posX, posY + height - FOOTER_HEIGHT, width, FOOTER_HEIGHT));
                renderFrame.drawRoundedRect(posX, posY, width, height, cornerRadius, accentFill);
                renderFrame.popClip();
            }
            if (divider != 0) {
                renderFrame.drawRect(posX, posY + height - FOOTER_HEIGHT, width, sepThickness, divider);
            }
        }

        if (border != 0) {
            renderFrame.drawRoundedRectFrame(posX, posY, width, height, cornerRadius, border, 0, borderThickness);
        }
    }

    private static class CenteredRowNode extends PositionedNode<CenteredRowNode> {
        @Override
        public CenteredRowNode addChild(UiNode child) {
            if (child instanceof PositionedNode<?> posChild) {
                posChild.alignY(0.5f);
            }
            return (CenteredRowNode) super.addChild(child);
        }
    }
}
