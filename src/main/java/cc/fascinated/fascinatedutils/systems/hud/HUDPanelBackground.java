package cc.fascinated.fascinatedutils.systems.hud;

import cc.fascinated.fascinatedutils.gui.render.AlumiteGuiRenderer;

public class HUDPanelBackground {

    public static final float LINE_GAP_PX = 1f;
    private static final int DOTTED_OUTLINE_COLOR = 0x80FFFFFF;
    private static final float DOTTED_OUTLINE_DOT_SIZE = 1f;
    private static final float DOTTED_OUTLINE_STEP = 3f;

    /**
     * Paints optional translucent HUD panel fill and/or border with optional rounded corners.
     *
     * @param backgroundArgb packed ARGB fill color
     * @param borderArgb     packed ARGB border color
     */
    public static void drawPanelChrome(AlumiteGuiRenderer glRenderer, float width, float height, boolean showBackground, float borderThickness, boolean showBorder, float cornerRadius, int roundedCornerMask, int backgroundArgb, int borderArgb, boolean editorMode) {
        if (!showBackground && !showBorder) {
            if (editorMode) {
                drawDottedOutline(glRenderer, width, height);
            }
            return;
        }
        float strokePx = Math.max(1f, borderThickness);
        if (cornerRadius > 0f) {
            if (showBackground) {
                glRenderer.fillRoundedRect(0f, 0f, width, height, cornerRadius, backgroundArgb, roundedCornerMask);
            }
            if (showBorder) {
                glRenderer.fillRoundedRectBorderRing(0f, 0f, width, height, cornerRadius, strokePx, borderArgb, roundedCornerMask);
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

    private static void drawDottedOutline(AlumiteGuiRenderer glRenderer, float width, float height) {
        if (width < DOTTED_OUTLINE_DOT_SIZE || height < DOTTED_OUTLINE_DOT_SIZE) {
            return;
        }

        glRenderer.drawDottedOutline(width, height, DOTTED_OUTLINE_DOT_SIZE, DOTTED_OUTLINE_STEP, DOTTED_OUTLINE_COLOR);
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
