package cc.fascinated.fascinatedutils.gui.hudeditor;

import cc.fascinated.fascinatedutils.common.ColorUtils;
import cc.fascinated.fascinatedutils.gui.renderer.GuiRenderer;
import cc.fascinated.fascinatedutils.gui.theme.UiColor;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

public class HudEditorOverlays {

    private static final int HINT_CARD_BACKGROUND = UiColor.argb("#a61c1824");
    private static final int HINT_CARD_BORDER = UiColor.argb("#8b5cf6");
    private static final int HINT_TITLE_COLOR = UiColor.argb("#faf5ff");
    private static final int HINT_BODY_COLOR = UiColor.argb("#d4d4d8");
    private static final int SNAP_GUIDE_COLOR = UiColor.argb("#b3913de2");
    private static final float SNAP_GUIDE_THICKNESS = 1f;
    private static final float HINT_CARD_PADDING_X = 10f;
    private static final float HINT_CARD_PADDING_Y = 8f;
    private static final float HINT_LINE_GAP = 2f;

    /**
     * Draws alignment snap guides when coordinates are finite.
     *
     * @param glRenderer   renderer for this pass
     * @param canvasWidth  logical canvas width
     * @param canvasHeight logical canvas height
     * @param snapGuideX   vertical guide X, or NaN to skip
     * @param snapGuideY   horizontal guide Y, or NaN to skip
     */
    public static void drawSnapGuides(GuiRenderer glRenderer, float canvasWidth, float canvasHeight, float snapGuideX, float snapGuideY) {
        if (Float.isFinite(snapGuideX)) {
            float guideX = Mth.clamp(snapGuideX, 0f, canvasWidth);
            glRenderer.drawRect(guideX, 0f, SNAP_GUIDE_THICKNESS, canvasHeight, SNAP_GUIDE_COLOR);
        }
        if (Float.isFinite(snapGuideY)) {
            float guideY = Mth.clamp(snapGuideY, 0f, canvasHeight);
            glRenderer.drawRect(0f, guideY, canvasWidth, SNAP_GUIDE_THICKNESS, SNAP_GUIDE_COLOR);
        }
    }

    /**
     * Draws the centered controls hint card.
     *
     * @param glRenderer   renderer for this pass
     * @param canvasWidth  logical canvas width
     * @param canvasHeight logical canvas height
     */
    public static void drawControlsHint(GuiRenderer glRenderer, float canvasWidth, float canvasHeight) {
        String title = Component.translatable("fascinatedutils.setting.hud_editor.hint.title").getString();
        String moveHint = Component.translatable("fascinatedutils.setting.hud_editor.hint.move").getString();
        String closeHint = Component.translatable("fascinatedutils.setting.hud_editor.hint.close").getString();
        int maxTextWidth = Math.max(glRenderer.measureTextWidth(title, true), Math.max(glRenderer.measureTextWidth(moveHint, false), glRenderer.measureTextWidth(closeHint, false)));
        float lineHeight = glRenderer.getFontHeight();
        float panelWidth = maxTextWidth + HINT_CARD_PADDING_X * 2f;
        float panelHeight = lineHeight * 3f + HINT_LINE_GAP * 2f + HINT_CARD_PADDING_Y * 2f;
        float panelX = (canvasWidth - panelWidth) * 0.5f;
        float panelY = (canvasHeight - panelHeight) * 0.5f;
        glRenderer.drawRect(panelX, panelY, panelWidth, panelHeight, HINT_CARD_BACKGROUND);
        glRenderer.drawBorder(panelX, panelY, panelWidth, panelHeight, 1f, HINT_CARD_BORDER);
        float textY = panelY + HINT_CARD_PADDING_Y;
        float centerX = panelX + panelWidth * 0.5f;
        glRenderer.drawMiniMessageText("<b><color:" + ColorUtils.rgbHex(HINT_TITLE_COLOR) + ">" + title + "</color></b>", centerX - glRenderer.measureMiniMessageTextWidth("<b><color:" + ColorUtils.rgbHex(HINT_TITLE_COLOR) + ">" + title + "</color></b>") * 0.5f, textY, false);
        textY += lineHeight + HINT_LINE_GAP;
        glRenderer.drawMiniMessageText("<color:" + ColorUtils.rgbHex(HINT_BODY_COLOR) + ">" + moveHint + "</color>", centerX - glRenderer.measureMiniMessageTextWidth("<color:" + ColorUtils.rgbHex(HINT_BODY_COLOR) + ">" + moveHint + "</color>") * 0.5f, textY, false);
        textY += lineHeight + HINT_LINE_GAP;
        glRenderer.drawMiniMessageText("<color:" + ColorUtils.rgbHex(HINT_BODY_COLOR) + ">" + closeHint + "</color>", centerX - glRenderer.measureMiniMessageTextWidth("<color:" + ColorUtils.rgbHex(HINT_BODY_COLOR) + ">" + closeHint + "</color>") * 0.5f, textY, false);
    }
}
