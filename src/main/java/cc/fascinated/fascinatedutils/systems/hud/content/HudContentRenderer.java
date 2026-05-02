package cc.fascinated.fascinatedutils.systems.hud.content;

import cc.fascinated.fascinatedutils.gui.renderer.GuiRenderer;
import cc.fascinated.fascinatedutils.systems.hud.HUDPanelBackground;
import cc.fascinated.fascinatedutils.systems.hud.HudHostModule;
import cc.fascinated.fascinatedutils.systems.hud.HudPanel;
import cc.fascinated.fascinatedutils.systems.hud.anchor.HudAnchorContentAlignment;
import cc.fascinated.fascinatedutils.systems.hud.anchor.HudAnchorLayout;
import net.minecraft.client.Minecraft;

import java.util.List;

public class HudContentRenderer {

    /**
     * Measure content dimensions and return a draw callback.
     * Dimensions are written to the panel HUD state before returning.
     *
     * @param glRenderer renderer for deferred draw callbacks
     * @param host       module supplying padding, chrome colors, shadows
     * @param panel      panel receiving layout commits
     * @return a draw callback, or {@code null} for {@link HudContent.Custom} content
     */
    public static Runnable prepare(GuiRenderer glRenderer, HudContent content, HudHostModule host, HudPanel panel, boolean editorMode) {
        return switch (content) {
            case HudContent.TextLines textLines -> prepareTextLines(glRenderer, textLines.miniMessageLines(), host, panel, editorMode);
            case HudContent.ItemRows itemRows -> prepareItemRows(glRenderer, itemRows.rows(), host, panel, editorMode);
            case HudContent.Custom _ -> null;
        };
    }

    private static Runnable prepareTextLines(GuiRenderer glRenderer, List<String> rawLines, HudHostModule host, HudPanel panel, boolean editorMode) {
        if (rawLines == null || rawLines.isEmpty()) {
            rawLines = List.of("");
        }

        float lineHeight = glRenderer.getFontHeight();
        float maxLineWidth = 0f;
        float[] lineWidths = new float[rawLines.size()];
        for (int index = 0; index < rawLines.size(); index++) {
            String miniMessageLine = rawLines.get(index);
            if (miniMessageLine == null) {
                miniMessageLine = "";
            }
            float width = glRenderer.measureMiniMessageTextWidth(miniMessageLine);
            lineWidths[index] = width;
            maxLineWidth = Math.max(maxLineWidth, width);
        }

        float horizontalPadding = host.getPadding();
        float verticalPadding = host.getPadding();
        float innerTextHeight = HUDPanelBackground.innerTextHeightForLineCount(rawLines.size(), lineHeight);
        float layoutWidth = Math.max(1f, Math.max(panel.getMinWidth(), maxLineWidth + 2f * horizontalPadding));
        float layoutHeight = Math.max(1f, innerTextHeight + 2f * verticalPadding);

        panel.getHudState().setLastLayoutWidth(layoutWidth);
        panel.getHudState().setLastLayoutHeight(layoutHeight);
        panel.getHudState().setCommittedLayoutWidth(layoutWidth);
        panel.getHudState().setCommittedLayoutHeight(layoutHeight);

        List<String> lines = rawLines;
        return () -> {
            host.drawHUDPanelBackground(glRenderer, layoutWidth, layoutHeight, editorMode);

            float innerHeight = layoutHeight - 2f * verticalPadding;
            float cursorY = verticalPadding + HudAnchorLayout.verticalOffsetInInnerBand(innerHeight, innerTextHeight, HudAnchorContentAlignment.Vertical.CENTER);
            float innerBandWidth = layoutWidth - 2f * horizontalPadding;

            for (int lineIndex = 0; lineIndex < lines.size(); lineIndex++) {
                String lineText = lines.get(lineIndex);
                if (lineText == null) {
                    lineText = "";
                }
                float lineWidth = lineWidths[lineIndex];
                float drawX = horizontalPadding + HudAnchorLayout.horizontalOffsetInInnerBand(innerBandWidth, lineWidth, panel.hudTextLineHorizontalAlignment());
                glRenderer.drawMiniMessageText(lineText, drawX, cursorY, host.isTextShadowEnabled());
                cursorY += lineHeight;
                if (lineIndex < lines.size() - 1) {
                    cursorY += HUDPanelBackground.LINE_GAP_PX;
                }
            }
        };
    }

    private static Runnable prepareItemRows(GuiRenderer glRenderer, List<HudContent.ItemRow> rows, HudHostModule host, HudPanel panel, boolean editorMode) {
        if (rows == null || rows.isEmpty()) {
            panel.recordHudContentSkipped();
            return null;
        }

        float horizontalPadding = host.getPadding();
        float verticalPadding = host.getPadding();
        float lineHeight = glRenderer.getFontHeight();
        float itemIconSize = 16f;
        float iconTextGap = 4f;
        float itemGapPx = 2f;
        float lineGapPx = 1f;

        float maxStripWidth = 0f;
        float[] textWidths = new float[rows.size()];
        float[] iconBandWidths = new float[rows.size()];

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
        float layoutWidth = Math.max(1f, Math.max(panel.getMinWidth(), maxStripWidth + 2f * horizontalPadding));
        float innerContentHeight = rows.size() * rowHeight + Math.max(0, rows.size() - 1) * lineGapPx;
        float layoutHeight = Math.max(1f, innerContentHeight + 2f * verticalPadding);

        panel.getHudState().setLastLayoutWidth(layoutWidth);
        panel.getHudState().setLastLayoutHeight(layoutHeight);
        panel.getHudState().setCommittedLayoutWidth(layoutWidth);
        panel.getHudState().setCommittedLayoutHeight(layoutHeight);

        return () -> {
            host.drawHUDPanelBackground(glRenderer, layoutWidth, layoutHeight, editorMode);

            float innerHeight = layoutHeight - 2f * verticalPadding;
            float cursorY = verticalPadding + HudAnchorLayout.verticalOffsetInInnerBand(innerHeight, innerContentHeight, panel.hudContentVerticalAlignment());
            float innerRowBandWidth = layoutWidth - 2f * horizontalPadding;
            boolean isOnRight = panel.hudContentHorizontalAlignment() == HudAnchorContentAlignment.Horizontal.RIGHT;

            Minecraft minecraftClient = Minecraft.getInstance();
            for (int index = 0; index < rows.size(); index++) {
                HudContent.ItemRow row = rows.get(index);
                float textWidthPx = textWidths[index];
                float gapBeforeIcon = textWidthPx > 0f ? iconTextGap : 0f;
                float iconBandWidth = iconBandWidths[index];
                float stripWidth = textWidthPx + gapBeforeIcon + iconBandWidth;
                float stripLeft = horizontalPadding + (isOnRight ? innerRowBandWidth - stripWidth : HudAnchorLayout.horizontalOffsetInInnerBand(innerRowBandWidth, stripWidth, HudAnchorContentAlignment.Horizontal.LEFT));

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
        };
    }

    private static float itemRowTextWidthPx(GuiRenderer glRenderer, String miniMessageText) {
        // Blank side text must not reserve the icon gap (measurement clamps empty components to 1px).
        if (miniMessageText == null || miniMessageText.isBlank()) {
            return 0f;
        }
        return glRenderer.measureMiniMessageTextWidth(miniMessageText);
    }

    private static float itemRowIconBandWidth(HudContent.ItemRow row, float itemGapPx) {
        float width = 0f;
        boolean hasPreviousStack = false;
        for (net.minecraft.world.item.ItemStack leadingStack : row.leadingStacks()) {
            if (hasPreviousStack) {
                width += itemGapPx;
            }
            width += itemStackBandWidth(leadingStack);
            hasPreviousStack = true;
        }
        if (hasPreviousStack) {
            width += itemGapPx;
        }
        return width + itemStackBandWidth(row.stack());
    }

    private static float itemStackBandWidth(net.minecraft.world.item.ItemStack stack) {
        return itemCountOverlayLeftOverflowPx(stack) + 16f + itemCountOverlayRightOverflowPx(stack);
    }

    private static void drawItemRowIcons(GuiRenderer glRenderer, Minecraft minecraftClient, HudContent.ItemRow row, float bandLeft, float iconY, float itemGapPx) {
        float cursorX = bandLeft;
        boolean hasPreviousStack = false;
        for (net.minecraft.world.item.ItemStack leadingStack : row.leadingStacks()) {
            if (hasPreviousStack) {
                cursorX += itemGapPx;
            }
            cursorX = drawItemRowIcon(glRenderer, minecraftClient, leadingStack, cursorX, iconY);
            hasPreviousStack = true;
        }
        if (hasPreviousStack) {
            cursorX += itemGapPx;
        }
        drawItemRowIcon(glRenderer, minecraftClient, row.stack(), cursorX, iconY);
    }

    private static float drawItemRowIcon(GuiRenderer glRenderer, Minecraft minecraftClient, net.minecraft.world.item.ItemStack stack, float bandLeft, float iconY) {
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
