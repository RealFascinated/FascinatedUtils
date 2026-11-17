package cc.fascinated.fascinatedutils.gui.node.display;

import cc.fascinated.fascinatedutils.gui.layout.PositionedNode;
import cc.fascinated.fascinatedutils.gui.render.AlumiteGuiRenderer;
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
        markLayoutDirty();
        return this;
    }

    public TextNode setBold(boolean bold) {
        this.bold = bold;
        markLayoutDirty();
        return this;
    }

    public TextNode setTextAlign(float horizontalAlign, float verticalAlign) {
        this.horizontalAlign = Math.max(0f, Math.min(1f, horizontalAlign));
        this.verticalAlign = Math.max(0f, Math.min(1f, verticalAlign));
        return this;
    }

    @Override
    protected int intrinsicWidth(AlumiteGuiRenderer renderFrame, int parentWidth) {
        return Math.round(measureTextWidth(renderFrame) * scale);
    }

    @Override
    protected int intrinsicHeight(AlumiteGuiRenderer renderFrame, int parentHeight, int resolvedWidth) {
        return Math.round(renderFrame.fontHeight() * scale);
    }

    @Override
    protected void renderSelf(AlumiteGuiRenderer renderFrame, float deltaSeconds) {
        String text = currentText();
        int textColor = colorResolver != null ? colorResolver.apply(renderFrame.theme())
                : colorOverrideArgb != null ? colorOverrideArgb
                : renderFrame.theme().textPrimary();
        int textWidth = Math.round(renderFrame.measureTextWidth(text, bold) * scale);
        int textPositionX = bounds().positionX() + Math.round((bounds().width() - textWidth) * horizontalAlign);
        int textPositionY = bounds().positionY() + Math.round((bounds().height() - renderFrame.fontHeight() * scale) * verticalAlign);
        if (text.isEmpty()) {
            return;
        }
        if (scale != 1.0f) {
            renderFrame.pushTransform(textPositionX, textPositionY, scale, scale);
            renderFrame.drawText(text, 0, 0, textColor, false, bold);
            renderFrame.popTransform();
        } else {
            renderFrame.drawText(text, textPositionX, textPositionY, textColor, false, bold);
        }
    }

    private int measureTextWidth(AlumiteGuiRenderer renderFrame) {
        return renderFrame.measureTextWidth(currentText(), bold);
    }

    private String currentText() {
        String text = textSupplier.get();
        return text == null ? "" : text;
    }
}
