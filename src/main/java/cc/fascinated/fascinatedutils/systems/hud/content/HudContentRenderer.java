package cc.fascinated.fascinatedutils.systems.hud.content;

import cc.fascinated.fascinatedutils.gui.renderer.GuiRenderer;
import cc.fascinated.fascinatedutils.systems.hud.HUDPanelBackground;
import cc.fascinated.fascinatedutils.systems.hud.HudAnchorContentAlignment;
import cc.fascinated.fascinatedutils.systems.hud.HudAnchorLayout;
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
            case HudContent.Custom custom -> null;
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

        float horizontalPadding = HUDPanelBackground.HORIZONTAL_PADDING;
        float verticalPadding = HUDPanelBackground.VERTICAL_PADDING;
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
            HudAnchorContentAlignment.Horizontal horizontal = module.hudContentHorizontalAlignment();
            HudAnchorContentAlignment.Vertical vertical = module.hudContentVerticalAlignment();
            float cursorY = verticalPadding + HudAnchorLayout.verticalOffsetInInnerBand(innerHeight, innerTextHeight, vertical);
            float innerBandWidth = layoutWidth - 2f * horizontalPadding;

            for (int lineIndex = 0; lineIndex < lines.size(); lineIndex++) {
                String lineText = lines.get(lineIndex);
                if (lineText == null) {
                    lineText = "";
                }
                float lineWidth = lineWidths[lineIndex];
                float drawX = horizontalPadding + HudAnchorLayout.horizontalOffsetInInnerBand(innerBandWidth, lineWidth, horizontal);
                glRenderer.drawMiniMessageText(lineText, drawX, cursorY, false);
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

        float horizontalPadding = HUDPanelBackground.HORIZONTAL_PADDING;
        float verticalPadding = HUDPanelBackground.VERTICAL_PADDING;
        float lineHeight = glRenderer.getFontHeight();
        float itemIconSize = 16f;
        float iconTextGap = 4f;
        float lineGapPx = 1f;

        float maxStripWidth = 0f;
        float[] textWidths = new float[rows.size()];

        for (int index = 0; index < rows.size(); index++) {
            HudContent.ItemRow row = rows.get(index);
            float textWidthPx = glRenderer.measureMiniMessageTextWidth(row.text());
            textWidths[index] = textWidthPx;
            float gapBeforeIcon = textWidthPx > 0 ? iconTextGap : 0f;
            maxStripWidth = Math.max(maxStripWidth, textWidthPx + gapBeforeIcon + itemIconSize);
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
                float gapBeforeIcon = textWidthPx > 0 ? iconTextGap : 0f;
                float stripWidth = textWidthPx + gapBeforeIcon + itemIconSize;
                float stripLeft = horizontalPadding + (isOnRight ? innerRowBandWidth - stripWidth : HudAnchorLayout.horizontalOffsetInInnerBand(innerRowBandWidth, stripWidth, HudAnchorContentAlignment.Horizontal.LEFT));

                float iconY = cursorY + (rowHeight - itemIconSize) * 0.5f;
                float textY = cursorY + (rowHeight - lineHeight) * 0.5f;

                if (isOnRight) {
                    float iconX = stripLeft + textWidthPx + gapBeforeIcon;
                    glRenderer.drawMiniMessageText(row.text(), stripLeft, textY, false);
                    if (minecraftClient.player != null) {
                        glRenderer.drawGuiItem(minecraftClient.player, row.stack(), iconX, iconY);
                    }
                    else {
                        glRenderer.drawGuiItem(row.stack(), iconX, iconY);
                    }
                }
                else {
                    float iconX = stripLeft;
                    float textX = stripLeft + itemIconSize + gapBeforeIcon;
                    if (minecraftClient.player != null) {
                        glRenderer.drawGuiItem(minecraftClient.player, row.stack(), iconX, iconY);
                    }
                    else {
                        glRenderer.drawGuiItem(row.stack(), iconX, iconY);
                    }
                    glRenderer.drawMiniMessageText(row.text(), textX, textY, false);
                }

                cursorY += rowHeight;
                if (index < rows.size() - 1) {
                    cursorY += lineGapPx;
                }
            }
        };
    }
}
