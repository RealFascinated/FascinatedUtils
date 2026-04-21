package cc.fascinated.fascinatedutils.systems.hud;

import cc.fascinated.fascinatedutils.gui.renderer.GuiRenderer;
import cc.fascinated.fascinatedutils.gui.renderer.RectCornerRoundMask;
import cc.fascinated.fascinatedutils.gui.theme.UITheme;

public class HUDPanelBackground {

    public static final float HORIZONTAL_PADDING = UITheme.PADDING_SM;
    public static final float VERTICAL_PADDING = UITheme.PADDING_SM;
    public static final float LINE_GAP_PX = 1f;

    /**
     * Paints optional translucent HUD panel fill and/or border with optional rounded corners.
     *
     * @param backgroundArgb packed ARGB fill color
     * @param borderArgb     packed ARGB border color
     */
    public static void drawPanelChrome(GuiRenderer glRenderer, float width, float height, boolean showBackground, float borderThickness, boolean showBorder, float cornerRadius, int backgroundArgb, int borderArgb) {
        if (!showBackground && !showBorder) {
            return;
        }
        float strokePx = Math.max(1f, borderThickness);
        if (cornerRadius > 0f) {
            if (showBackground) {
                glRenderer.fillRoundedRect(0f, 0f, width, height, cornerRadius, backgroundArgb, RectCornerRoundMask.ALL);
            }
            if (showBorder) {
                glRenderer.fillRoundedRectBorderRing(0f, 0f, width, height, cornerRadius, strokePx, borderArgb, RectCornerRoundMask.ALL);
            }
        }
        else {
            if (showBackground && showBorder) {
                glRenderer.drawRect(0f, 0f, width, height, backgroundArgb);
                glRenderer.endRenderSegment();
                glRenderer.drawBorder(0f, 0f, width, height, strokePx, borderArgb);
            }
            else if (showBackground) {
                glRenderer.drawRect(0f, 0f, width, height, backgroundArgb);
            }
            else {
                glRenderer.drawBorder(0f, 0f, width, height, strokePx, borderArgb);
            }
        }
    }

    public static float innerTextHeightForLineCount(int lineCount, float lineHeight) {
        float innerTextHeight = 0f;
        for (int lineIndex = 0; lineIndex < lineCount; lineIndex++) {
            if (lineIndex > 0) {
                innerTextHeight += LINE_GAP_PX;
            }
            innerTextHeight += lineHeight;
        }
        return innerTextHeight;
    }
}
