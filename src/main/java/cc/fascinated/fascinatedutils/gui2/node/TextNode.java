package cc.fascinated.fascinatedutils.gui2.node;

import cc.fascinated.fascinatedutils.gui2.core.PositionedNode;
import cc.fascinated.fascinatedutils.gui2.core.UiNode;
import cc.fascinated.fascinatedutils.gui2.layout.AxisConstraints;
import cc.fascinated.fascinatedutils.gui2.render.RenderFrame;
import cc.fascinated.fascinatedutils.gui2.render.UiText;
import cc.fascinated.fascinatedutils.gui2.theme.UiTheme;
import cc.fascinated.fascinatedutils.oldgui.core.TextLineLayout;

import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

public class TextNode extends PositionedNode {
    private Supplier<String> textSupplier;
    private Integer colorOverrideArgb;
    private Function<UiTheme, Integer> colorResolver;
    private float scale = 1.0f;
    private boolean shadow;
    private boolean bold;
    private float horizontalAlign = 0.5f;
    private float verticalAlign = 0.5f;
    private boolean wrap;

    public TextNode(String text) {
        this(() -> text);
    }

    public TextNode(Supplier<String> textSupplier) {
        this.textSupplier = textSupplier == null ? () -> "" : textSupplier;
    }

    public TextNode setText(String text) {
        this.textSupplier = () -> text == null ? "" : text;
        return this;
    }

    public TextNode setTextSupplier(Supplier<String> textSupplier) {
        this.textSupplier = textSupplier == null ? () -> "" : textSupplier;
        return this;
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

    public TextNode setShadow(boolean shadow) {
        this.shadow = shadow;
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

    public TextNode setWrap(boolean wrap) {
        this.wrap = wrap;
        return this;
    }

    @Override
    public void layout(RenderFrame renderFrame, int parentX, int parentY, int parentWidth, int parentHeight) {
        AxisConstraints horizontalConstraints = boxLayout().horizontal();
        AxisConstraints verticalConstraints = boxLayout().vertical();

        int resolvedWidth = resolveAxisSize(horizontalConstraints, parentWidth, measureTextWidth(renderFrame), "horizontal");
        int intrinsicHeight;
        if (wrap) {
            int lineCount = Math.max(1, TextLineLayout.wrapLines(currentText(), resolvedWidth, segment -> renderFrame.measureTextWidth(segment, bold)).size());
            intrinsicHeight = lineCount * renderFrame.fontHeight();
        } else {
            intrinsicHeight = renderFrame.fontHeight();
        }
        int resolvedHeight = resolveAxisSize(verticalConstraints, parentHeight, intrinsicHeight, "vertical");
        int resolvedX = horizontalConstraints.resolvePosition(parentX, parentWidth, resolvedWidth);
        int resolvedY = verticalConstraints.resolvePosition(parentY, parentHeight, resolvedHeight);
        bounds().set(resolvedX, resolvedY, resolvedWidth, resolvedHeight);

        for (UiNode childNode : childrenView()) {
            childNode.layout(renderFrame, resolvedX, resolvedY, resolvedWidth, resolvedHeight);
        }
    }

    @Override
    protected void renderSelf(RenderFrame renderFrame, float deltaSeconds) {
        String text = currentText();
        int textColor = colorResolver != null ? colorResolver.apply(renderFrame.theme())
                : colorOverrideArgb != null ? colorOverrideArgb
                : renderFrame.theme().textPrimary();
        if (wrap) {
            List<String> lines = TextLineLayout.wrapLines(text, bounds().width(), segment -> renderFrame.measureTextWidth(segment, bold));
            int lineHeight = Math.round(renderFrame.fontHeight() * scale);
            int totalHeight = lines.size() * lineHeight;
            int cursorY = bounds().positionY() + Math.round((bounds().height() - totalHeight) * verticalAlign);
            for (String line : lines) {
                int lineWidth = Math.round(renderFrame.measureTextWidth(line, bold) * scale);
                int lineX = bounds().positionX() + Math.round((bounds().width() - lineWidth) * horizontalAlign);
                UiText.of(line).color(textColor).shadow(shadow).bold(bold).scale(scale).draw(renderFrame, lineX, cursorY);
                cursorY += lineHeight;
            }
        } else {
            int textWidth = Math.round(renderFrame.measureTextWidth(text, bold) * scale);
            int textPositionX = bounds().positionX() + Math.round((bounds().width() - textWidth) * horizontalAlign);
            int textPositionY = bounds().positionY() + Math.round((bounds().height() - renderFrame.fontHeight() * scale) * verticalAlign);
            UiText.of(text).color(textColor).shadow(shadow).bold(bold).scale(scale).draw(renderFrame, textPositionX, textPositionY);
        }
    }

    private int resolveAxisSize(AxisConstraints axisConstraints, int parentSize, int intrinsicSize, String axisLabel) {
        if (axisConstraints.hasSizeConstraint() || (axisConstraints.hasStartConstraint() && axisConstraints.hasEndConstraint())) {
            return axisConstraints.resolveSize(parentSize, axisLabel, debugPath());
        }
        return intrinsicSize;
    }

    private int measureTextWidth(RenderFrame renderFrame) {
        return renderFrame.measureTextWidth(currentText(), bold);
    }

    private String currentText() {
        String text = textSupplier.get();
        return text == null ? "" : text;
    }
}