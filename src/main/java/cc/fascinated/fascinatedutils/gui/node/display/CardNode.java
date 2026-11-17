package cc.fascinated.fascinatedutils.gui.node.display;

import cc.fascinated.fascinatedutils.gui.layout.PositionedNode;
import cc.fascinated.fascinatedutils.gui.core.UiNode;
import cc.fascinated.fascinatedutils.gui.render.RectCornerRoundMask;
import cc.fascinated.fascinatedutils.gui.render.AlumiteGuiRenderer;
import cc.fascinated.fascinatedutils.gui.theme.UiMetrics;
import cc.fascinated.fascinatedutils.gui.theme.UiTheme;

import java.util.function.Consumer;
import java.util.function.ToIntFunction;

public class CardNode extends PositionedNode<CardNode> {

    public static final int HEADER_HEIGHT = UiMetrics.HEADER_HEIGHT;
    public static final int FOOTER_HEIGHT = UiMetrics.HEADER_HEIGHT;

    private static final int CORNER_RADIUS = UiMetrics.RADIUS;

    private ToIntFunction<UiTheme> fillResolver = UiTheme::panelFill;
    private ToIntFunction<UiTheme> borderResolver = UiTheme::panelBorder;
    private int borderThickness = 1;
    private PositionedNode<?> headerNode;
    private PositionedNode<?> footerNode;
    private PositionedNode<?> contentsNode;

    public CardNode setHeader(Consumer<PositionedNode<?>> builder) {
        if (headerNode == null) {
            headerNode = new CenteredRowNode().left(UiMetrics.CARD_INSET).right(UiMetrics.CARD_INSET).top(0).height(HEADER_HEIGHT).rowGap(UiMetrics.CARD_INSET);
            addChild(headerNode);
        }
        builder.accept(headerNode);
        return this;
    }

    public CardNode setFooter(Consumer<PositionedNode<?>> builder) {
        if (footerNode == null) {
            footerNode = new CenteredRowNode().left(UiMetrics.CARD_INSET).right(UiMetrics.CARD_INSET).bottom(0).height(FOOTER_HEIGHT).rowGap(UiMetrics.CARD_INSET);
            addChild(footerNode);
        }
        builder.accept(footerNode);
        return this;
    }

    public CardNode setContents(Consumer<PositionedNode<?>> builder) {
        if (contentsNode == null) {
            contentsNode = new PositionedNode<>().full();
            addChild(contentsNode);
        }
        builder.accept(contentsNode);
        return this;
    }

    public CardNode setFillArgb(int argb) {
        fillResolver = theme -> argb;
        return this;
    }

    public CardNode setFillResolver(ToIntFunction<UiTheme> resolver) {
        fillResolver = resolver;
        return this;
    }

    public CardNode setBorderArgb(int argb) {
        borderResolver = theme -> argb;
        return this;
    }

    public CardNode setBorderResolver(ToIntFunction<UiTheme> resolver) {
        borderResolver = resolver;
        return this;
    }

    public CardNode setNoBorder() {
        borderResolver = null;
        return this;
    }

    public CardNode setBorderThickness(int borderThickness) {
        this.borderThickness = Math.max(1, borderThickness);
        return this;
    }

    @Override
    protected void arrangeChildren(AlumiteGuiRenderer renderFrame, int positionX, int positionY, int width, int height) {
        int topInset = headerNode != null ? HEADER_HEIGHT : 0;
        int bottomInset = footerNode != null ? FOOTER_HEIGHT : 0;
        for (UiNode childNode : childrenView()) {
            if (!childNode.visible()) {
                continue;
            }
            if (childNode == contentsNode) {
                childNode.layout(renderFrame, positionX, positionY + topInset, width, Math.max(0, height - topInset - bottomInset));
            }
            else {
                childNode.layout(renderFrame, positionX, positionY, width, height);
            }
        }
    }

    @Override
    protected void renderSelf(AlumiteGuiRenderer renderFrame, float deltaSeconds) {
        UiTheme theme = renderFrame.theme();
        int posX = bounds().positionX();
        int posY = bounds().positionY();
        int width = bounds().width();
        int height = bounds().height();
        int cornerRadius = CORNER_RADIUS;

        int fill = fillResolver != null ? fillResolver.applyAsInt(theme) : 0;
        int border = borderResolver != null ? borderResolver.applyAsInt(theme) : 0;

        if (headerNode == null && footerNode == null) {
            RectNode.drawBox(renderFrame, posX, posY, width, height, cornerRadius, fill, border, borderThickness);
            return;
        }

        if (fill != 0) {
            renderFrame.drawRoundedRect(posX, posY, width, height, cornerRadius, fill);
        }

        int accentFill = theme.panelHeaderFill();
        int divider = theme.divider();
        int sepThickness = theme.separatorThickness();

        if (headerNode != null) {
            if (accentFill != 0) {
                renderFrame.fillRoundedRect(posX, posY, width, HEADER_HEIGHT, cornerRadius, accentFill, RectCornerRoundMask.TOP);
            }
            if (divider != 0) {
                renderFrame.drawRect(posX, posY + HEADER_HEIGHT, width, sepThickness, divider);
            }
        }

        if (footerNode != null) {
            if (accentFill != 0) {
                renderFrame.fillRoundedRect(posX, posY + height - FOOTER_HEIGHT, width, FOOTER_HEIGHT, cornerRadius, accentFill, RectCornerRoundMask.BOTTOM);
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
