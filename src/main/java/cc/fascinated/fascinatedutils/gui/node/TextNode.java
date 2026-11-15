package cc.fascinated.fascinatedutils.gui.node;

import cc.fascinated.fascinatedutils.gui.core.PositionedNode;
import cc.fascinated.fascinatedutils.gui.render.GuiRenderer;
import cc.fascinated.fascinatedutils.gui.render.UiText;
import cc.fascinated.fascinatedutils.gui.theme.UiTheme;

import java.util.function.Function;
import java.util.function.Supplier;

public class TextNode extends PositionedNode<TextNode> {
    private Supplier<String> textSupplier;
    private Integer colorOverrideArgb;
    private Function<UiTheme, Integer> colorResolver;
    private float scale = 1.0f;
    private boolean bold;
    private float horizontalAlign = 0.5f;
    private float verticalAlign = 0.5f;

    public TextNode(Supplier<String> textSupplier) {
        this.textSupplier = textSupplier == null ? () -> "" : textSupplier;
    }

    public TextNode setColorArgb(int colorArgb) {
        this.colorOverrideArgb = colorArgb;
        this.colorResolver = null;
        return this;
    }

    public TextNode setColorResolver(Function<UiTheme, Integer> colorResolver) {
        this.colorResolver = colorResolver;
        this.colorOverrideArgb = null;
        return this;
    }

    public TextNode setScale(float scale) {
        this.scale = scale;
        return this;
    }

    public TextNode setBold(boolean bold) {
        this.bold = bold;
        return this;
    }

    public TextNode setTextAlign(float horizontalAlign, float verticalAlign) {
        this.horizontalAlign = Math.max(0f, Math.min(1f, horizontalAlign));
        this.verticalAlign = Math.max(0f, Math.min(1f, verticalAlign));
        return this;
    }

    @Override
    protected int intrinsicWidth(GuiRenderer renderFrame, int parentWidth) {
        return measureTextWidth(renderFrame);
    }

    @Override
    protected int intrinsicHeight(GuiRenderer renderFrame, int parentHeight, int resolvedWidth) {
        return renderFrame.fontHeight();
    }

    @Override
    protected void renderSelf(GuiRenderer renderFrame, float deltaSeconds) {
        String text = currentText();
        int textColor = colorResolver != null ? colorResolver.apply(renderFrame.theme())
                : colorOverrideArgb != null ? colorOverrideArgb
                : renderFrame.theme().textPrimary();
        int textWidth = Math.round(renderFrame.measureTextWidth(text, bold) * scale);
        int textPositionX = bounds().positionX() + Math.round((bounds().width() - textWidth) * horizontalAlign);
        int textPositionY = bounds().positionY() + Math.round((bounds().height() - renderFrame.fontHeight() * scale) * verticalAlign);
        UiText.of(text).color(textColor).bold(bold).scale(scale).draw(renderFrame, textPositionX, textPositionY);
    }

    private int measureTextWidth(GuiRenderer renderFrame) {
        return renderFrame.measureTextWidth(currentText(), bold);
    }

    private String currentText() {
        String text = textSupplier.get();
        return text == null ? "" : text;
    }
}
