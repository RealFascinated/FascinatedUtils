package cc.fascinated.fascinatedutils.gui.hudeditor;

import cc.fascinated.fascinatedutils.gui.renderer.GuiRenderer;
import cc.fascinated.fascinatedutils.gui.renderer.RectCornerRoundMask;
import cc.fascinated.fascinatedutils.gui.theme.UITheme;
import cc.fascinated.fascinatedutils.gui.theme.UiColor;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

public class HudEditorOverlays {

    private static final int SNAP_GUIDE_COLOR = UiColor.argb("#b3913de2");
    private static final float SNAP_GUIDE_THICKNESS = 1f;
    private static final float BRANDING_TITLE_TO_MODS_GAP = 16f;
    private static final float MODS_BUTTON_PAD_X = 32f;
    private static final float MODS_BUTTON_PAD_Y = 9f;

    private static float modsLeft;
    private static float modsTop;
    private static float modsWidth;
    private static float modsHeight;
    private static boolean modsHitValid;

    /**
     * Clears the last published MODS button hit region before a new HUD editor frame is drawn.
     */
    public static void clearBrandingHitLayout() {
        modsHitValid = false;
    }

    /**
     * Whether the logical pointer lies over the MODS button from the last {@link #drawBrandingCenterOverlay} call.
     *
     * @param pointerX logical pointer X
     * @param pointerY logical pointer Y
     * @return true when the MODS hit region exists and contains the point
     */
    public static boolean hitTestModsButton(float pointerX, float pointerY) {
        return modsHitValid
                && pointerX >= modsLeft && pointerY >= modsTop
                && pointerX <= modsLeft + modsWidth && pointerY <= modsTop + modsHeight;
    }

    /**
     * Draws a centered title and MODS entry control (Lunar-style chrome) for the empty HUD editor state.
     *
     * @param glRenderer   renderer for this pass
     * @param canvasWidth  logical canvas width
     * @param canvasHeight logical canvas height
     * @param pointerX     logical pointer X (for MODS hover styling)
     * @param pointerY     logical pointer Y (for MODS hover styling)
     */
    public static void drawBrandingCenterOverlay(GuiRenderer glRenderer, float canvasWidth, float canvasHeight, float pointerX, float pointerY) {
        String titleMiniMessage = Component.translatable("fascinatedutils.setting.hud_editor.branding.title").getString();
        String modsLabel = Component.translatable("fascinatedutils.setting.hud_editor.branding.mods_button").getString();
        float lineHeight = glRenderer.getFontHeight();
        int titleWidth = glRenderer.measureMiniMessageTextWidth(titleMiniMessage);
        int modsTextWidth = glRenderer.measureTextWidth(modsLabel, true);
        float modsButtonWidth = modsTextWidth + MODS_BUTTON_PAD_X * 2f;
        float modsButtonHeight = lineHeight + MODS_BUTTON_PAD_Y * 2f;
        float stackHeight = lineHeight + BRANDING_TITLE_TO_MODS_GAP + modsButtonHeight;
        float blockTop = (canvasHeight - stackHeight) * 0.5f;
        float centerX = canvasWidth * 0.5f;
        float titleLeft = centerX - titleWidth * 0.5f;
        float btnLeft = centerX - modsButtonWidth * 0.5f;
        float btnTop = blockTop + lineHeight + BRANDING_TITLE_TO_MODS_GAP;
        modsLeft = btnLeft;
        modsTop = btnTop;
        modsWidth = modsButtonWidth;
        modsHeight = modsButtonHeight;
        modsHitValid = true;
        boolean modsHovered = hitTestModsButton(pointerX, pointerY);
        glRenderer.drawMiniMessageText(titleMiniMessage, titleLeft, blockTop, false);
        float borderThicknessX = UITheme.BORDER_THICKNESS_PX;
        float borderThicknessY = UITheme.BORDER_THICKNESS_PX;
        float maxCornerRadius = Math.min(modsButtonWidth, modsButtonHeight) * 0.5f - 0.01f;
        float themedCorner = glRenderer.theme().cardCornerRadius();
        float modsCornerRadius = Mth.clamp(themedCorner, 0.5f, Math.max(0.5f, maxCornerRadius - Math.min(borderThicknessX, borderThicknessY) * 0.5f));
        int modsBorderColor = modsHovered ? glRenderer.theme().borderHover() : glRenderer.theme().border();
        glRenderer.fillRoundedRectFrame(btnLeft, btnTop, modsButtonWidth, modsButtonHeight, modsCornerRadius, modsBorderColor, glRenderer.theme().surface(), borderThicknessX, borderThicknessY, RectCornerRoundMask.ALL);
        float modsTextY = btnTop + MODS_BUTTON_PAD_Y;
        glRenderer.drawCenteredText(modsLabel, centerX, modsTextY, glRenderer.theme().textPrimary(), false, false);
    }

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
}
