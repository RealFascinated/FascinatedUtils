package cc.fascinated.fascinatedutils.systems.hud;

import cc.fascinated.fascinatedutils.gui.GuiDesignSpace;
import cc.fascinated.fascinatedutils.gui.renderer.GuiRenderer;
import cc.fascinated.fascinatedutils.gui.renderer.RectCornerRoundMask;
import cc.fascinated.fascinatedutils.gui.theme.UITheme;
import cc.fascinated.fascinatedutils.settings.SettingsRegistry;

public class HUDPanelBackground {

    public static final float HORIZONTAL_PADDING = UITheme.PADDING_SM;
    public static final float VERTICAL_PADDING = UITheme.PADDING_SM;
    public static final float LINE_GAP_PX = 1f;

    /**
     * Paints optional translucent HUD panel fill and/or border; rounded fill plus border flushes mesh between fill and ring.
     * Uses global HUD color settings from the settings registry.
     */
    public static void drawPanelChrome(GuiRenderer glRenderer, float width, float height, boolean showBackground, float borderThickness, boolean showBorder, boolean roundedCorners, float cornerRadius) {
        if (!showBackground && !showBorder) {
            return;
        }
        float strokePx = Math.max(1f, GuiDesignSpace.pxUniform(borderThickness));
        int borderArgb = SettingsRegistry.INSTANCE.getSettings().getHudBorderColor().getResolvedArgb();
        int backgroundArgb = SettingsRegistry.INSTANCE.getSettings().getHudBackgroundColor().getResolvedArgb();
        if (roundedCorners) {
            float radius = Math.max(0.5f, cornerRadius);
            if (showBackground && showBorder) {
                glRenderer.fillRoundedRect(0f, 0f, width, height, radius, backgroundArgb, RectCornerRoundMask.ALL);
                glRenderer.endRenderSegment();
                glRenderer.fillRoundedRectBorderRing(0f, 0f, width, height, radius, strokePx, borderArgb, RectCornerRoundMask.ALL);
            }
            else if (showBackground) {
                glRenderer.fillRoundedRect(0f, 0f, width, height, radius, backgroundArgb, RectCornerRoundMask.ALL);
            }
            else {
                glRenderer.fillRoundedRectBorderRing(0f, 0f, width, height, radius, strokePx, borderArgb, RectCornerRoundMask.ALL);
            }
            return;
        }
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
