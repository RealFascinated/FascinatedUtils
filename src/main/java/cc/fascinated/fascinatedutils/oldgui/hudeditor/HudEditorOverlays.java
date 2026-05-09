package cc.fascinated.fascinatedutils.oldgui.hudeditor;

import cc.fascinated.fascinatedutils.Constants;
import cc.fascinated.fascinatedutils.client.ModUiTextures;
import cc.fascinated.fascinatedutils.oldgui.renderer.GuiRenderer;
import cc.fascinated.fascinatedutils.oldgui.renderer.RectCornerRoundMask;
import cc.fascinated.fascinatedutils.oldgui.theme.UITheme;
import cc.fascinated.fascinatedutils.oldgui.theme.UiColor;
import cc.fascinated.fascinatedutils.updater.Updater;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

public class HudEditorOverlays {

    private static final int SNAP_GUIDE_COLOR = UiColor.argb("#b3913de2");
    private static final int SNAP_GUIDE_CENTER_COLOR = UiColor.argb("#ffe0c84a");
    private static final float SNAP_GUIDE_THICKNESS = 1f;
    private static final int CENTER_CROSSHAIR_COLOR = UiColor.argb("#28913de2");
    private static final float CENTER_CROSSHAIR_DASH = 4f;
    private static final float CENTER_CROSSHAIR_GAP = 4f;
    private static final float BRANDING_TITLE_TO_MODS_GAP = 16f;
    private static final float SUNNY_ICON_SIZE = 32f;
    private static final float SUNNY_ICON_TO_TITLE_GAP = 12f;
    private static final float MODS_BUTTON_PAD_X = 32f;
    private static final float MODS_BUTTON_PAD_Y = 9f;

    private static final float CLOSE_EDITOR_ICON_SIZE = 16f;
    private static final float CLOSE_EDITOR_BUTTON_PADDING = 5f;
    private static final float CLOSE_EDITOR_MARGIN = 5f;

    private static float modsLeft;
    private static float modsTop;
    private static float modsWidth;
    private static float modsHeight;
    private static boolean modsHitValid;

    private static float closeEditorLeft;
    private static float closeEditorTop;
    private static float closeEditorWidth;
    private static float closeEditorHeight;
    private static boolean closeEditorHitValid;

    /**
     * Clears the last published MODS button and close-editor button hit regions before a new HUD editor frame is drawn.
     */
    public static void clearBrandingHitLayout() {
        modsHitValid = false;
        closeEditorHitValid = false;
    }

    /**
     * Whether the logical pointer lies over the MODS button from the last {@link #drawBrandingCenterOverlay} call.
     *
     * @param pointerX logical pointer X
     * @param pointerY logical pointer Y
     * @return true when the MODS hit region exists and contains the point
     */
    public static boolean hitTestModsButton(float pointerX, float pointerY) {
        return modsHitValid && pointerX >= modsLeft && pointerY >= modsTop && pointerX <= modsLeft + modsWidth && pointerY <= modsTop + modsHeight;
    }

    /**
     * Whether the logical pointer lies over the close-editor button from the last {@link #drawCloseEditorButton} call.
     *
     * @param pointerX logical pointer X
     * @param pointerY logical pointer Y
     * @return true when the close-editor hit region exists and contains the point
     */
    public static boolean hitTestCloseEditorButton(float pointerX, float pointerY) {
        return closeEditorHitValid && pointerX >= closeEditorLeft && pointerY >= closeEditorTop && pointerX <= closeEditorLeft + closeEditorWidth && pointerY <= closeEditorTop + closeEditorHeight;
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
        String titleMiniMessage = Component.translatable("alumite.setting.hud_editor.branding.title").getString();
        String modsLabel = Component.translatable("alumite.setting.hud_editor.branding.mods_button").getString();
        float lineHeight = glRenderer.getFontHeight();
        int titleWidth = glRenderer.measureMiniMessageTextWidth(titleMiniMessage);
        int modsTextWidth = glRenderer.measureTextWidth(modsLabel, true);
        float modsButtonWidth = modsTextWidth + MODS_BUTTON_PAD_X * 2f;
        float modsButtonHeight = lineHeight + MODS_BUTTON_PAD_Y * 2f;
        float stackHeight = SUNNY_ICON_SIZE + SUNNY_ICON_TO_TITLE_GAP + lineHeight + BRANDING_TITLE_TO_MODS_GAP + modsButtonHeight;
        float blockTop = (canvasHeight - stackHeight) * 0.5f;
        float centerX = canvasWidth * 0.5f;
        float titleLeft = centerX - titleWidth * 0.5f;
        float btnLeft = centerX - modsButtonWidth * 0.5f;
        float titleTop = blockTop + SUNNY_ICON_SIZE + SUNNY_ICON_TO_TITLE_GAP;
        float btnTop = titleTop + lineHeight + BRANDING_TITLE_TO_MODS_GAP;
        modsLeft = btnLeft;
        modsTop = btnTop;
        modsWidth = modsButtonWidth;
        modsHeight = modsButtonHeight;
        modsHitValid = true;
        boolean modsHovered = hitTestModsButton(pointerX, pointerY);
        float iconLeft = centerX - SUNNY_ICON_SIZE * 0.5f;
        glRenderer.drawSprite(ModUiTextures.SUNNY.getId(), iconLeft, blockTop, SUNNY_ICON_SIZE, SUNNY_ICON_SIZE, glRenderer.theme().textPrimary());
        glRenderer.drawMiniMessageText(titleMiniMessage, titleLeft, titleTop, false);
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
     * Draws a close button in the top-right corner of the HUD editor.
     *
     * @param glRenderer   renderer for this pass
     * @param canvasWidth  logical canvas width
     * @param pointerX     logical pointer X (for hover styling)
     * @param pointerY     logical pointer Y (for hover styling)
     */
    public static void drawCloseEditorButton(GuiRenderer glRenderer, float canvasWidth, float pointerX, float pointerY) {
        float buttonSize = CLOSE_EDITOR_ICON_SIZE + CLOSE_EDITOR_BUTTON_PADDING * 2f;
        float btnLeft = canvasWidth - buttonSize - CLOSE_EDITOR_MARGIN;
        float btnTop = CLOSE_EDITOR_MARGIN;
        closeEditorLeft = btnLeft;
        closeEditorTop = btnTop;
        closeEditorWidth = buttonSize;
        closeEditorHeight = buttonSize;
        closeEditorHitValid = true;
        boolean hovered = hitTestCloseEditorButton(pointerX, pointerY);
        float borderThickness = UITheme.BORDER_THICKNESS_PX;
        float maxCornerRadius = buttonSize * 0.5f - 0.01f;
        float cornerRadius = Mth.clamp(glRenderer.theme().cardCornerRadius(), 0.5f, maxCornerRadius - borderThickness * 0.5f);
        int borderColor = hovered ? glRenderer.theme().borderHover() : glRenderer.theme().border();
        glRenderer.fillRoundedRectFrame(btnLeft, btnTop, buttonSize, buttonSize, cornerRadius, borderColor, glRenderer.theme().surface(), borderThickness, borderThickness, RectCornerRoundMask.ALL);
        float iconLeft = btnLeft + CLOSE_EDITOR_BUTTON_PADDING;
        float iconTop = btnTop + CLOSE_EDITOR_BUTTON_PADDING;
        int iconColor = hovered ? 0xFFFFFFFF : 0xAAFFFFFF;
        glRenderer.drawTexture(ModUiTextures.CLOSE.getId(), iconLeft, iconTop, CLOSE_EDITOR_ICON_SIZE, CLOSE_EDITOR_ICON_SIZE, iconColor);
    }

    /**
     * Draws the mod version label in the bottom-right corner of the HUD editor.
     *
     * @param glRenderer   renderer for this pass
     * @param canvasWidth  logical canvas width
     * @param canvasHeight logical canvas height
     */
    public static void drawVersionLabel(GuiRenderer glRenderer, float canvasWidth, float canvasHeight) {
        String label = Component.translatable("alumite.setting.hud_editor.branding.version_label", Constants.GAME_VERSION, Constants.MOD_VERSION).getString();
        float lineHeight = glRenderer.getFontHeight();
        float posY = canvasHeight - lineHeight - CLOSE_EDITOR_MARGIN;
        if (Updater.INSTANCE.isHasUpdate()) {
            String updateLabel = Component.translatable("alumite.setting.hud_editor.branding.update_available").getString();
            glRenderer.drawText(updateLabel, CLOSE_EDITOR_MARGIN, posY - lineHeight, 0xFF55FF55, false, false);
        }
        glRenderer.drawText(label, CLOSE_EDITOR_MARGIN, posY, 0x80FFFFFF, false, false);
    }

    /**
     * Draws a faint dashed center crosshair so the screen center is always visible in the editor.
     *
     * @param glRenderer   renderer for this pass
     * @param canvasWidth  logical canvas width
     * @param canvasHeight logical canvas height
     */
    public static void drawEditorCenterCrosshair(GuiRenderer glRenderer, float canvasWidth, float canvasHeight) {
        float centerX = (float) Math.floor(canvasWidth * 0.5f);
        float centerY = (float) Math.floor(canvasHeight * 0.5f);
        drawDashedVerticalLine(glRenderer, centerX, canvasHeight, CENTER_CROSSHAIR_COLOR);
        drawDashedHorizontalLine(glRenderer, centerY, canvasWidth, CENTER_CROSSHAIR_COLOR);
    }

    private static void drawDashedHorizontalLine(GuiRenderer glRenderer, float y, float totalWidth, int color) {
        float x = 0f;
        boolean draw = true;
        while (x < totalWidth) {
            float segW = Math.min(draw ? CENTER_CROSSHAIR_DASH : CENTER_CROSSHAIR_GAP, totalWidth - x);
            if (draw) {
                glRenderer.drawRect(x, y, segW, 1f, color);
            }
            x += segW;
            draw = !draw;
        }
    }

    private static void drawDashedVerticalLine(GuiRenderer glRenderer, float x, float totalHeight, int color) {
        float y = 0f;
        boolean draw = true;
        while (y < totalHeight) {
            float segH = Math.min(draw ? CENTER_CROSSHAIR_DASH : CENTER_CROSSHAIR_GAP, totalHeight - y);
            if (draw) {
                glRenderer.drawRect(x, y, 1f, segH, color);
            }
            y += segH;
            draw = !draw;
        }
    }

    /**
     * Draws alignment snap guides when coordinates are finite. Center-axis guides are rendered in a
     * distinct golden color to distinguish them from edge and widget alignment guides.
     *
     * @param glRenderer         renderer for this pass
     * @param canvasWidth        logical canvas width
     * @param canvasHeight       logical canvas height
     * @param snapGuideX         vertical guide X, or NaN to skip
     * @param snapGuideY         horizontal guide Y, or NaN to skip
     * @param snapGuideXIsCenter whether the vertical guide is a center-axis snap
     * @param snapGuideYIsCenter whether the horizontal guide is a center-axis snap
     */
    public static void drawSnapGuides(GuiRenderer glRenderer, float canvasWidth, float canvasHeight, float snapGuideX, float snapGuideY, boolean snapGuideXIsCenter, boolean snapGuideYIsCenter) {
        if (Float.isFinite(snapGuideX)) {
            float guideX = Mth.clamp(snapGuideX, 0f, canvasWidth);
            int color = snapGuideXIsCenter ? SNAP_GUIDE_CENTER_COLOR : SNAP_GUIDE_COLOR;
            glRenderer.drawRect(guideX, 0f, SNAP_GUIDE_THICKNESS, canvasHeight, color);
        }
        if (Float.isFinite(snapGuideY)) {
            float guideY = Mth.clamp(snapGuideY, 0f, canvasHeight);
            int color = snapGuideYIsCenter ? SNAP_GUIDE_CENTER_COLOR : SNAP_GUIDE_COLOR;
            glRenderer.drawRect(0f, guideY, canvasWidth, SNAP_GUIDE_THICKNESS, color);
        }
    }
}
