package cc.fascinated.fascinatedutils.systems.hud.content;

import cc.fascinated.fascinatedutils.gui.render.AlumiteGuiRenderer;
import cc.fascinated.fascinatedutils.systems.hud.HUDPanelBackground;
import cc.fascinated.fascinatedutils.systems.hud.HudHostModule;
import cc.fascinated.fascinatedutils.systems.hud.HudPanel;
import cc.fascinated.fascinatedutils.systems.hud.anchor.HudAnchorContentAlignment;
import cc.fascinated.fascinatedutils.systems.hud.anchor.HudAnchorLayout;
import net.minecraft.client.Minecraft;

import java.util.List;

public class HudContentRenderer {
    private static final List<String> BLANK_LINES = List.of("");

    /**
     * Measure content dimensions and return a draw callback.
     * Dimensions are written to the panel HUD state before returning.
     *
     * @param glRenderer renderer for deferred draw callbacks
     * @param host       module supplying padding, chrome colors, shadows
     * @param panel      panel receiving layout commits
     * @param textLinesPainter per-panel painter reused for text-line content
     * @param itemRowsPainter per-panel painter reused for item-row content
     * @return a draw callback, or {@code null} when there is nothing to draw
     */
    public static Runnable prepare(AlumiteGuiRenderer glRenderer, HudContent content, HudHostModule host, HudPanel panel, boolean editorMode, TextLinesPainter textLinesPainter, ItemRowsPainter itemRowsPainter) {
        return switch (content) {
            case HudContent.TextLines textLines ->
                    prepareTextLines(glRenderer, textLines.miniMessageLines(), host, panel, editorMode, textLinesPainter);
            case HudContent.ItemRows itemRows -> prepareItemRows(glRenderer, itemRows.rows(), host, panel, editorMode, itemRowsPainter);
        };
    }

    private static TextLinesPainter prepareTextLines(AlumiteGuiRenderer glRenderer, List<String> rawLines, HudHostModule host, HudPanel panel, boolean editorMode, TextLinesPainter painter) {
        List<String> lines = rawLines == null || rawLines.isEmpty() ? BLANK_LINES : rawLines;

        float lineHeight = glRenderer.fontHeight();
        float maxLineWidth = 0f;
        if (painter.lineWidths.length < lines.size()) {
            painter.lineWidths = new float[lines.size()];
        }
        float[] lineWidths = painter.lineWidths;
        for (int index = 0; index < lines.size(); index++) {
            String miniMessageLine = lines.get(index);
            if (miniMessageLine == null) {
                miniMessageLine = "";
            }
            float width = glRenderer.measureMiniMessageTextWidth(miniMessageLine);
            lineWidths[index] = width;
            maxLineWidth = Math.max(maxLineWidth, width);
        }

        float horizontalPadding = host.getPaddingValue();
        float verticalPadding = host.getPaddingValue();
        float innerTextHeight = HUDPanelBackground.innerTextHeightForLineCount(lines.size(), lineHeight);
        float topTrim = recordTopTrim(panel, verticalPadding, editorMode);
        float leftTrim = recordLeftTrim(panel, horizontalPadding, editorMode);
        float layoutWidth = Math.max(1f, Math.max(panel.getMinWidth(), maxLineWidth + 2f * horizontalPadding - leftTrim));
        float layoutHeight = Math.max(1f, innerTextHeight + 2f * verticalPadding - topTrim);

        panel.commitLayout(layoutWidth, layoutHeight);

        painter.glRenderer = glRenderer;
        painter.host = host;
        painter.panel = panel;
        painter.lines = lines;
        painter.lineHeight = lineHeight;
        painter.horizontalPadding = horizontalPadding;
        painter.verticalPadding = verticalPadding;
        painter.innerTextHeight = innerTextHeight;
        painter.topTrim = topTrim;
        painter.leftTrim = leftTrim;
        painter.layoutWidth = layoutWidth;
        painter.layoutHeight = layoutHeight;
        painter.editorMode = editorMode;
        return painter;
    }

    /**
     * Draw callback for text-line content. Each panel owns one instance and refills it every frame,
     * so preparing a text-line panel allocates no callback or width buffer once the buffer has grown.
     */
    public static class TextLinesPainter implements Runnable {
        private AlumiteGuiRenderer glRenderer;
        private HudHostModule host;
        private HudPanel panel;
        private List<String> lines = BLANK_LINES;
        private float[] lineWidths = new float[0];
        private float lineHeight;
        private float horizontalPadding;
        private float verticalPadding;
        private float innerTextHeight;
        private float topTrim;
        private float leftTrim;
        private float layoutWidth;
        private float layoutHeight;
        private boolean editorMode;

        @Override
        public void run() {
            host.drawHUDPanelBackground(glRenderer, layoutWidth, layoutHeight, editorMode);

            float innerHeight = layoutHeight - 2f * verticalPadding + topTrim;
            float cursorY = verticalPadding - topTrim + HudAnchorLayout.verticalOffsetInInnerBand(innerHeight, innerTextHeight, HudAnchorContentAlignment.Vertical.CENTER);
            float innerBandWidth = layoutWidth - 2f * horizontalPadding + leftTrim;

            for (int lineIndex = 0; lineIndex < lines.size(); lineIndex++) {
                String lineText = lines.get(lineIndex);
                if (lineText == null) {
                    lineText = "";
                }
                float lineWidth = lineWidths[lineIndex];
                float drawX = horizontalPadding - leftTrim + HudAnchorLayout.horizontalOffsetInInnerBand(innerBandWidth, lineWidth, panel.hudTextLineHorizontalAlignment());
                glRenderer.drawMiniMessageText(lineText, drawX, cursorY, host.isTextShadowEnabled());
                cursorY += lineHeight;
                if (lineIndex < lines.size() - 1) {
                    cursorY += HUDPanelBackground.LINE_GAP_PX;
                }
            }
        }
    }

    private static ItemRowsPainter prepareItemRows(AlumiteGuiRenderer glRenderer, List<HudContent.ItemRow> rows, HudHostModule host, HudPanel panel, boolean editorMode, ItemRowsPainter painter) {
        if (rows == null || rows.isEmpty()) {
            panel.recordHudContentSkipped();
            return null;
        }

        float horizontalPadding = host.getPaddingValue();
        float verticalPadding = host.getPaddingValue();
        float lineHeight = glRenderer.fontHeight();
        float itemIconSize = 16f;
        float iconTextGap = 4f;
        float itemGapPx = 2f;
        float lineGapPx = 1f;

        float maxStripWidth = 0f;
        if (painter.textWidths.length < rows.size()) {
            painter.textWidths = new float[rows.size()];
            painter.iconBandWidths = new float[rows.size()];
        }
        float[] textWidths = painter.textWidths;
        float[] iconBandWidths = painter.iconBandWidths;

        for (int index = 0; index < rows.size(); index++) {
            HudContent.ItemRow row = rows.get(index);
            float textWidthPx = itemRowTextWidthPx(glRenderer, row.text());
            textWidths[index] = textWidthPx;
            float gapBeforeIcon = textWidthPx > 0f ? iconTextGap : 0f;
            float iconBandWidth = itemRowIconBandWidth(row, itemGapPx);
            iconBandWidths[index] = iconBandWidth;
            maxStripWidth = Math.max(maxStripWidth, textWidthPx + gapBeforeIcon + iconBandWidth);
        }

        float rowHeight = Math.max(itemIconSize, lineHeight);
        float topTrim = recordTopTrim(panel, verticalPadding, editorMode);
        float leftTrim = recordLeftTrim(panel, horizontalPadding, editorMode);
        float layoutWidth = Math.max(1f, Math.max(panel.getMinWidth(), maxStripWidth + 2f * horizontalPadding - leftTrim));
        float innerContentHeight = rows.size() * rowHeight + Math.max(0, rows.size() - 1) * lineGapPx;
        float layoutHeight = Math.max(1f, innerContentHeight + 2f * verticalPadding - topTrim);

        panel.commitLayout(layoutWidth, layoutHeight);

        painter.glRenderer = glRenderer;
        painter.host = host;
        painter.panel = panel;
        painter.rows = rows;
        painter.lineHeight = lineHeight;
        painter.horizontalPadding = horizontalPadding;
        painter.verticalPadding = verticalPadding;
        painter.rowHeight = rowHeight;
        painter.itemIconSize = itemIconSize;
        painter.iconTextGap = iconTextGap;
        painter.itemGapPx = itemGapPx;
        painter.lineGapPx = lineGapPx;
        painter.innerContentHeight = innerContentHeight;
        painter.topTrim = topTrim;
        painter.leftTrim = leftTrim;
        painter.layoutWidth = layoutWidth;
        painter.layoutHeight = layoutHeight;
        painter.editorMode = editorMode;
        return painter;
    }

    /**
     * Draw callback for item-row content. Each panel owns one instance and refills it every frame,
     * so preparing an item-row panel allocates no callback or width buffers once the buffers have grown.
     */
    public static class ItemRowsPainter implements Runnable {
        private AlumiteGuiRenderer glRenderer;
        private HudHostModule host;
        private HudPanel panel;
        private List<HudContent.ItemRow> rows = List.of();
        private float[] textWidths = new float[0];
        private float[] iconBandWidths = new float[0];
        private float lineHeight;
        private float horizontalPadding;
        private float verticalPadding;
        private float rowHeight;
        private float itemIconSize;
        private float iconTextGap;
        private float itemGapPx;
        private float lineGapPx;
        private float innerContentHeight;
        private float topTrim;
        private float leftTrim;
        private float layoutWidth;
        private float layoutHeight;
        private boolean editorMode;

        @Override
        public void run() {
            host.drawHUDPanelBackground(glRenderer, layoutWidth, layoutHeight, editorMode);

            float innerHeight = layoutHeight - 2f * verticalPadding + topTrim;
            float cursorY = verticalPadding - topTrim + HudAnchorLayout.verticalOffsetInInnerBand(innerHeight, innerContentHeight, panel.hudContentVerticalAlignment());
            float innerRowBandWidth = layoutWidth - 2f * horizontalPadding + leftTrim;
            boolean isOnRight = panel.hudContentHorizontalAlignment() == HudAnchorContentAlignment.Horizontal.RIGHT;

            Minecraft minecraftClient = Minecraft.getInstance();
            for (int index = 0; index < rows.size(); index++) {
                HudContent.ItemRow row = rows.get(index);
                float textWidthPx = textWidths[index];
                float gapBeforeIcon = textWidthPx > 0f ? iconTextGap : 0f;
                float iconBandWidth = iconBandWidths[index];
                float stripWidth = textWidthPx + gapBeforeIcon + iconBandWidth;
                float stripLeft = horizontalPadding - leftTrim + (isOnRight ? innerRowBandWidth - stripWidth : HudAnchorLayout.horizontalOffsetInInnerBand(innerRowBandWidth, stripWidth, HudAnchorContentAlignment.Horizontal.LEFT));

                float iconY = cursorY + (rowHeight - itemIconSize) * 0.5f;
                float textY = cursorY + (rowHeight - lineHeight) * 0.5f;
                String rowText = row.text();
                boolean drawRowText = rowText != null && !rowText.isBlank();

                if (isOnRight) {
                    if (drawRowText) {
                        glRenderer.drawMiniMessageText(rowText, stripLeft, textY, host.isTextShadowEnabled());
                    }
                    drawItemRowIcons(glRenderer, minecraftClient, row, stripLeft + textWidthPx + gapBeforeIcon, iconY, itemGapPx);
                }
                else {
                    float textX = stripLeft + iconBandWidth + gapBeforeIcon;
                    drawItemRowIcons(glRenderer, minecraftClient, row, stripLeft, iconY, itemGapPx);
                    if (drawRowText) {
                        glRenderer.drawMiniMessageText(rowText, textX, textY, host.isTextShadowEnabled());
                    }
                }

                cursorY += rowHeight;
                if (index < rows.size() - 1) {
                    cursorY += lineGapPx;
                }
            }
        }
    }

    private static float recordTopTrim(HudPanel panel, float verticalPadding, boolean editorMode) {
        float topTrim = !editorMode && panel.getHudState().isTrimTopPadding() ? verticalPadding : 0f;
        panel.getHudState().setAppliedTopTrim(topTrim);
        return topTrim;
    }

    private static float recordLeftTrim(HudPanel panel, float horizontalPadding, boolean editorMode) {
        float leftTrim = !editorMode && panel.getHudState().isTrimLeftPadding() ? horizontalPadding : 0f;
        panel.getHudState().setAppliedLeftTrim(leftTrim);
        return leftTrim;
    }

    private static float itemRowTextWidthPx(AlumiteGuiRenderer glRenderer, String miniMessageText) {
        // Blank side text must not reserve the icon gap (measurement clamps empty components to 1px).
        if (miniMessageText == null || miniMessageText.isBlank()) {
            return 0f;
        }
        return glRenderer.measureMiniMessageTextWidth(miniMessageText);
    }

    private static float itemRowIconBandWidth(HudContent.ItemRow row, float itemGapPx) {
        float width = 0f;
        boolean hasPreviousStack = false;
        for (HudContent.HudItemSpec leadingSpec : row.leadingSpecs()) {
            if (hasPreviousStack) {
                width += itemGapPx;
            }
            width += itemStackBandWidth(leadingSpec.toStack());
            hasPreviousStack = true;
        }
        if (hasPreviousStack) {
            width += itemGapPx;
        }
        return width + itemStackBandWidth(row.spec().toStack());
    }

    private static float itemStackBandWidth(net.minecraft.world.item.ItemStack stack) {
        return itemCountOverlayLeftOverflowPx(stack) + 16f + itemCountOverlayRightOverflowPx(stack);
    }

    private static void drawItemRowIcons(AlumiteGuiRenderer glRenderer, Minecraft minecraftClient, HudContent.ItemRow row, float bandLeft, float iconY, float itemGapPx) {
        float cursorX = bandLeft;
        boolean hasPreviousStack = false;
        for (HudContent.HudItemSpec leadingSpec : row.leadingSpecs()) {
            if (hasPreviousStack) {
                cursorX += itemGapPx;
            }
            cursorX = drawItemRowIcon(glRenderer, minecraftClient, leadingSpec.toStack(), cursorX, iconY);
            hasPreviousStack = true;
        }
        if (hasPreviousStack) {
            cursorX += itemGapPx;
        }
        drawItemRowIcon(glRenderer, minecraftClient, row.spec().toStack(), cursorX, iconY);
    }

    private static float drawItemRowIcon(AlumiteGuiRenderer glRenderer, Minecraft minecraftClient, net.minecraft.world.item.ItemStack stack, float bandLeft, float iconY) {
        float iconX = bandLeft + itemCountOverlayLeftOverflowPx(stack);
        if (minecraftClient.player != null) {
            glRenderer.drawGuiItem(minecraftClient.player, stack, iconX, iconY);
        }
        else {
            glRenderer.drawGuiItem(stack, iconX, iconY);
        }
        return bandLeft + itemStackBandWidth(stack);
    }

    private static float itemCountOverlayLeftOverflowPx(net.minecraft.world.item.ItemStack stack) {
        if (!hasVisibleStackCountOverlay(stack)) {
            return 0f;
        }
        int amountTextWidth = Minecraft.getInstance().font.width(String.valueOf(stack.getCount()));
        return Math.max(0f, amountTextWidth - 17f);
    }

    private static float itemCountOverlayRightOverflowPx(net.minecraft.world.item.ItemStack stack) {
        return hasVisibleStackCountOverlay(stack) ? 1f : 0f;
    }

    private static boolean hasVisibleStackCountOverlay(net.minecraft.world.item.ItemStack stack) {
        return !stack.isEmpty() && stack.getCount() != 1;
    }
}
