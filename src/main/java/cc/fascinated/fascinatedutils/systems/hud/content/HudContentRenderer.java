package cc.fascinated.fascinatedutils.systems.hud.content;

import cc.fascinated.fascinatedutils.gui.renderer.GuiRenderer;
import cc.fascinated.fascinatedutils.systems.hud.HUDPanelBackground;
import cc.fascinated.fascinatedutils.systems.hud.anchor.HudAnchorContentAlignment;
import cc.fascinated.fascinatedutils.systems.hud.anchor.HudAnchorLayout;
import cc.fascinated.fascinatedutils.systems.hud.HudModule;
import net.minecraft.client.Minecraft;

import java.util.List;

public class HudContentRenderer {

    /**
     * Measure content dimensions and return a draw callback.
     * Dimensions are written to the module's HUD state before returning.
     *
     * @return a draw callback, or {@code null} for {@link HudContent.Custom} content
     */
    public static Runnable prepare(GuiRenderer glRenderer, HudContent content, HudModule module) {
        return switch (content) {
            case HudContent.TextLines textLines -> prepareTextLines(glRenderer, textLines.miniMessageLines(), module);
            case HudContent.ItemRows itemRows -> prepareItemRows(glRenderer, itemRows.rows(), module);
            case HudContent.Custom _ -> null;
        };
    }

    private static Runnable prepareTextLines(GuiRenderer glRenderer, List<String> rawLines, HudModule module) {
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

        float horizontalPadding = module.getPadding();
        float verticalPadding = module.getPadding();
        float innerTextHeight = HUDPanelBackground.innerTextHeightForLineCount(rawLines.size(), lineHeight);
        float layoutWidth = Math.max(1f, Math.max(module.getMinWidth(), maxLineWidth + 2f * horizontalPadding));
        float layoutHeight = Math.max(1f, innerTextHeight + 2f * verticalPadding);

        module.getHudState().setLastLayoutWidth(layoutWidth);
        module.getHudState().setLastLayoutHeight(layoutHeight);
        module.getHudState().setCommittedLayoutWidth(layoutWidth);
        module.getHudState().setCommittedLayoutHeight(layoutHeight);

        List<String> lines = rawLines;
        return () -> {
            module.drawHUDPanelBackground(glRenderer, layoutWidth, layoutHeight);

            float innerHeight = layoutHeight - 2f * verticalPadding;
            float cursorY = verticalPadding + HudAnchorLayout.verticalOffsetInInnerBand(innerHeight, innerTextHeight, HudAnchorContentAlignment.Vertical.CENTER);
            float innerBandWidth = layoutWidth - 2f * horizontalPadding;

            for (int lineIndex = 0; lineIndex < lines.size(); lineIndex++) {
                String lineText = lines.get(lineIndex);
                if (lineText == null) {
                    lineText = "";
                }
                float lineWidth = lineWidths[lineIndex];
                float drawX = horizontalPadding + HudAnchorLayout.horizontalOffsetInInnerBand(innerBandWidth, lineWidth, module.hudTextLineHorizontalAlignment());
                glRenderer.drawMiniMessageText(lineText, drawX, cursorY, module.isTextShadowEnabled());
                cursorY += lineHeight;
                if (lineIndex < lines.size() - 1) {
                    cursorY += HUDPanelBackground.LINE_GAP_PX;
                }
            }
        };
    }

    private static Runnable prepareItemRows(GuiRenderer glRenderer, List<HudContent.ItemRow> rows, HudModule module) {
        if (rows == null || rows.isEmpty()) {
            module.recordHudContentSkipped();
            return null;
        }

        float horizontalPadding = module.getPadding();
        float verticalPadding = module.getPadding();
        float lineHeight = glRenderer.getFontHeight();
        float itemIconSize = 16f;
        float iconTextGap = 4f;
        float lineGapPx = 1f;

        float maxStripWidth = 0f;
        float[] textWidths = new float[rows.size()];
        float[] countOverlayLeftOverflow = new float[rows.size()];
        float[] countOverlayRightOverflow = new float[rows.size()];

        for (int index = 0; index < rows.size(); index++) {
            HudContent.ItemRow row = rows.get(index);
            float textWidthPx = itemRowTextWidthPx(glRenderer, row.text());
            textWidths[index] = textWidthPx;
            float leftOverflowPx = itemCountOverlayLeftOverflowPx(row.stack());
            float rightOverflowPx = itemCountOverlayRightOverflowPx(row.stack());
            countOverlayLeftOverflow[index] = leftOverflowPx;
            countOverlayRightOverflow[index] = rightOverflowPx;
            float gapBeforeIcon = textWidthPx > 0f ? iconTextGap : 0f;
            float iconBandWidth = leftOverflowPx + itemIconSize + rightOverflowPx;
            maxStripWidth = Math.max(maxStripWidth, textWidthPx + gapBeforeIcon + iconBandWidth);
        }

        float rowHeight = Math.max(itemIconSize, lineHeight);
        float layoutWidth = Math.max(1f, Math.max(module.getMinWidth(), maxStripWidth + 2f * horizontalPadding));
        float innerContentHeight = rows.size() * rowHeight + Math.max(0, rows.size() - 1) * lineGapPx;
        float layoutHeight = Math.max(1f, innerContentHeight + 2f * verticalPadding);

        module.getHudState().setLastLayoutWidth(layoutWidth);
        module.getHudState().setLastLayoutHeight(layoutHeight);
        module.getHudState().setCommittedLayoutWidth(layoutWidth);
        module.getHudState().setCommittedLayoutHeight(layoutHeight);

        return () -> {
            module.drawHUDPanelBackground(glRenderer, layoutWidth, layoutHeight);

            float innerHeight = layoutHeight - 2f * verticalPadding;
            float cursorY = verticalPadding + HudAnchorLayout.verticalOffsetInInnerBand(innerHeight, innerContentHeight, module.hudContentVerticalAlignment());
            float innerRowBandWidth = layoutWidth - 2f * horizontalPadding;
            boolean isOnRight = module.hudContentHorizontalAlignment() == HudAnchorContentAlignment.Horizontal.RIGHT;

            Minecraft minecraftClient = Minecraft.getInstance();
            for (int index = 0; index < rows.size(); index++) {
                HudContent.ItemRow row = rows.get(index);
                float textWidthPx = textWidths[index];
                float leftOverflowPx = countOverlayLeftOverflow[index];
                float rightOverflowPx = countOverlayRightOverflow[index];
                float gapBeforeIcon = textWidthPx > 0f ? iconTextGap : 0f;
                float iconBandWidth = leftOverflowPx + itemIconSize + rightOverflowPx;
                float stripWidth = textWidthPx + gapBeforeIcon + iconBandWidth;
                float stripLeft = horizontalPadding + (isOnRight ? innerRowBandWidth - stripWidth : HudAnchorLayout.horizontalOffsetInInnerBand(innerRowBandWidth, stripWidth, HudAnchorContentAlignment.Horizontal.LEFT));

                float iconY = cursorY + (rowHeight - itemIconSize) * 0.5f;
                float textY = cursorY + (rowHeight - lineHeight) * 0.5f;
                String rowText = row.text();
                boolean drawRowText = rowText != null && !rowText.isBlank();

                if (isOnRight) {
                    float iconX = stripLeft + textWidthPx + gapBeforeIcon + leftOverflowPx;
                    if (drawRowText) {
                        glRenderer.drawMiniMessageText(rowText, stripLeft, textY, module.isTextShadowEnabled());
                    }
                    if (minecraftClient.player != null) {
                        glRenderer.drawGuiItem(minecraftClient.player, row.stack(), iconX, iconY);
                    }
                    else {
                        glRenderer.drawGuiItem(row.stack(), iconX, iconY);
                    }
                }
                else {
                    float iconX = stripLeft + leftOverflowPx;
                    float textX = stripLeft + iconBandWidth + gapBeforeIcon;
                    if (minecraftClient.player != null) {
                        glRenderer.drawGuiItem(minecraftClient.player, row.stack(), iconX, iconY);
                    }
                    else {
                        glRenderer.drawGuiItem(row.stack(), iconX, iconY);
                    }
                    if (drawRowText) {
                        glRenderer.drawMiniMessageText(rowText, textX, textY, module.isTextShadowEnabled());
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
