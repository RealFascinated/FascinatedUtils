package cc.fascinated.fascinatedutils.gui.widgets;

import cc.fascinated.fascinatedutils.common.MathUtils;
import cc.fascinated.fascinatedutils.gui.GuiDesignSpace;
import cc.fascinated.fascinatedutils.gui.UIScale;
import cc.fascinated.fascinatedutils.gui.core.TextLineLayout;
import cc.fascinated.fascinatedutils.gui.renderer.GuiRenderer;
import cc.fascinated.fascinatedutils.gui.renderer.RectCornerRoundMask;

import java.util.ArrayList;
import java.util.List;

public class WTooltip {

    /**
     * Draws a tooltip at the given mouse position with the given text.
     *
     * @param graphics the graphics object to draw on
     * @param mouseX   the x position of the mouse
     * @param mouseY   the y position of the mouse
     * @param text     the text to display in the tooltip
     */
    public static void draw(GuiRenderer graphics, float mouseX, float mouseY, String text) {
        if (text == null || text.isBlank()) {
            return;
        }

        draw(graphics, mouseX, mouseY, List.of(text), -1);
    }

    public static void draw(GuiRenderer graphics, float mouseX, float mouseY, List<String> lines, int boldLineIndex) {
        if (lines == null || lines.isEmpty()) {
            return;
        }

        graphics.absolutePost(() -> drawNow(graphics, mouseX, mouseY, lines, boldLineIndex));
    }

    private static void drawNow(GuiRenderer graphics, float mouseX, float mouseY, List<String> lines, int boldLineIndex) {
        graphics.resetMultiplyAlpha();

        float padX = GuiDesignSpace.pxX(6f);
        float padY = GuiDesignSpace.pxY(4f);
        float viewportPadding = GuiDesignSpace.pxUniform(2f);
        float horizontalOffset = GuiDesignSpace.pxX(10f);
        float verticalOffset = GuiDesignSpace.pxY(8f);
        float lineHeight = graphics.getFontHeight();
        float lineGap = GuiDesignSpace.pxY(1f);

        float canvasWidth = UIScale.logicalWidth() * GuiDesignSpace.scaleX();
        float canvasHeight = UIScale.logicalHeight() * GuiDesignSpace.scaleY();

        float maxTextWidth = Math.max(GuiDesignSpace.pxX(120f), Math.min(GuiDesignSpace.pxX(260f), canvasWidth * 0.45f));
        List<TooltipLine> wrappedLines = new ArrayList<>();
        for (int lineIndex = 0; lineIndex < lines.size(); lineIndex++) {
            String line = lines.get(lineIndex);
            if (line == null) {
                continue;
            }
            boolean bold = lineIndex == boldLineIndex;
            List<String> wrapped = TextLineLayout.wrapLines(line, maxTextWidth, segment -> graphics.measureTextWidth(segment, bold));
            if (wrapped.isEmpty()) {
                wrappedLines.add(new TooltipLine(line, bold));
                continue;
            }
            for (String wrappedLine : wrapped) {
                wrappedLines.add(new TooltipLine(wrappedLine, bold));
            }
        }
        if (wrappedLines.isEmpty()) {
            return;
        }

        float maxLineWidth = 0f;
        for (TooltipLine line : wrappedLines) {
            maxLineWidth = Math.max(maxLineWidth, graphics.measureTextWidth(line.text, line.bold));
        }

        float panelWidth = maxLineWidth + 2f * padX;
        float panelHeight = wrappedLines.size() * lineHeight + Math.max(0, wrappedLines.size() - 1) * lineGap + 2f * padY;
        float maxPanelX = canvasWidth - panelWidth - viewportPadding;
        float maxPanelY = canvasHeight - panelHeight - viewportPadding;

        float rightSidePanelX = mouseX + horizontalOffset;
        float leftSidePanelX = mouseX - horizontalOffset - panelWidth;
        float belowPanelY = mouseY + verticalOffset;
        float abovePanelY = mouseY - verticalOffset - panelHeight;

        boolean rightFits = rightSidePanelX >= viewportPadding && rightSidePanelX <= maxPanelX;
        boolean leftFits = leftSidePanelX >= viewportPadding && leftSidePanelX <= maxPanelX;
        boolean belowFits = belowPanelY >= viewportPadding && belowPanelY <= maxPanelY;
        boolean aboveFits = abovePanelY >= viewportPadding && abovePanelY <= maxPanelY;

        float panelX;
        float panelY;
        if (rightFits && belowFits) {
            panelX = rightSidePanelX;
            panelY = belowPanelY;
        }
        else if (leftFits && belowFits) {
            panelX = leftSidePanelX;
            panelY = belowPanelY;
        }
        else if (rightFits && aboveFits) {
            panelX = rightSidePanelX;
            panelY = abovePanelY;
        }
        else if (leftFits && aboveFits) {
            panelX = leftSidePanelX;
            panelY = abovePanelY;
        }
        else {
            panelX = MathUtils.clamp(rightSidePanelX, viewportPadding, Math.max(viewportPadding, maxPanelX));
            panelY = MathUtils.clamp(belowPanelY, viewportPadding, Math.max(viewportPadding, maxPanelY));
        }

        float cornerRadius = Math.max(GuiDesignSpace.pxUniform(2f), GuiDesignSpace.pxUniform(graphics.theme().cardCornerRadius() * 0.75f));
        float borderThickness = GuiDesignSpace.pxUniform(1f);
        float shadowOffset = GuiDesignSpace.pxUniform(1f);

        int backgroundColor = graphics.theme().hintBackground();
        int borderColor = graphics.theme().hintBorder();
        int textColor = graphics.theme().hintText();
        int shadowColor = withAlpha(0xFF000000, 110);

        graphics.fillRoundedRect(panelX + shadowOffset, panelY + shadowOffset, panelWidth, panelHeight, cornerRadius, shadowColor, RectCornerRoundMask.ALL);
        graphics.fillRoundedRect(panelX, panelY, panelWidth, panelHeight, cornerRadius, backgroundColor, RectCornerRoundMask.ALL);
        graphics.fillRoundedRectBorderRing(panelX, panelY, panelWidth, panelHeight, cornerRadius, borderThickness, borderColor, RectCornerRoundMask.ALL);

        float textX = panelX + padX;
        float textY = panelY + padY;
        for (TooltipLine line : wrappedLines) {
            graphics.drawText(line.text, textX, textY, textColor, true, line.bold);
            textY += lineHeight + lineGap;
        }

        graphics.endRenderSegment();
    }

    private static int withAlpha(int color, int alpha) {
        int clampedAlpha = Math.max(0, Math.min(255, alpha));
        return (clampedAlpha << 24) | (color & 0x00FFFFFF);
    }

    private record TooltipLine(String text, boolean bold) {}
}
