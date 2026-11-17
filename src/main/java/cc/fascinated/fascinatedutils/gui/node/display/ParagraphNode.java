package cc.fascinated.fascinatedutils.gui.node.display;

import cc.fascinated.fascinatedutils.gui.layout.PositionedNode;
import cc.fascinated.fascinatedutils.gui.render.AlumiteGuiRenderer;
import cc.fascinated.fascinatedutils.gui.theme.UiTheme;

import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Left-aligned text that soft-wraps to the node's width and can be limited to a number of lines, ending the last
 * line in an ellipsis when text is cut. With a fixed width it wraps at that width; without one it wraps at the width
 * available from its parent and is as wide as its longest line.
 */
public class ParagraphNode extends PositionedNode<ParagraphNode> {
    private final Supplier<String> textSupplier;
    private Integer colorOverrideArgb;
    private Function<UiTheme, Integer> colorResolver;
    private boolean bold;
    private int maxLines = Integer.MAX_VALUE;
    private int lineGap;

    public ParagraphNode(Supplier<String> textSupplier) {
        this.textSupplier = textSupplier == null ? () -> "" : textSupplier;
    }

    public ParagraphNode setColorArgb(int colorArgb) {
        this.colorOverrideArgb = colorArgb;
        this.colorResolver = null;
        return this;
    }

    public ParagraphNode setColorResolver(Function<UiTheme, Integer> colorResolver) {
        this.colorResolver = colorResolver;
        this.colorOverrideArgb = null;
        return this;
    }

    public ParagraphNode setBold(boolean bold) {
        this.bold = bold;
        markLayoutDirty();
        return this;
    }

    /**
     * Limits the paragraph to a number of lines; text beyond it is replaced by an ellipsis on the last line.
     *
     * @param maxLines the most lines to show, at least one
     * @return this node
     */
    public ParagraphNode setMaxLines(int maxLines) {
        this.maxLines = Math.max(1, maxLines);
        markLayoutDirty();
        return this;
    }

    /**
     * Sets the space between consecutive lines.
     *
     * @param lineGap the gap in pixels
     * @return this node
     */
    public ParagraphNode setLineGap(int lineGap) {
        this.lineGap = Math.max(0, lineGap);
        markLayoutDirty();
        return this;
    }

    @Override
    protected int intrinsicWidth(AlumiteGuiRenderer renderFrame, int parentWidth) {
        int widest = 0;
        for (String line : lines(renderFrame, parentWidth)) {
            widest = Math.max(widest, renderFrame.measureTextWidth(line, bold));
        }
        return widest;
    }

    @Override
    protected int intrinsicHeight(AlumiteGuiRenderer renderFrame, int parentHeight, int resolvedWidth) {
        int lineCount = lines(renderFrame, resolvedWidth).size();
        return lineCount == 0 ? 0 : lineCount * renderFrame.fontHeight() + (lineCount - 1) * lineGap;
    }

    @Override
    protected void renderSelf(AlumiteGuiRenderer renderFrame, float deltaSeconds) {
        int textColor = colorResolver != null ? colorResolver.apply(renderFrame.theme())
                : colorOverrideArgb != null ? colorOverrideArgb
                : renderFrame.theme().textPrimary();
        int lineStep = renderFrame.fontHeight() + lineGap;
        List<String> lines = lines(renderFrame, bounds().width());
        for (int lineIndex = 0; lineIndex < lines.size(); lineIndex++) {
            renderFrame.drawText(lines.get(lineIndex), bounds().positionX(), bounds().positionY() + lineIndex * lineStep, textColor, false, bold);
        }
    }

    private List<String> lines(AlumiteGuiRenderer renderFrame, int wrapWidth) {
        String text = textSupplier.get();
        return renderFrame.wrapLines(text == null ? "" : text, wrapWidth, bold, maxLines);
    }
}
