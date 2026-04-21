package cc.fascinated.fascinatedutils.gui.modsettings;

import org.joml.Matrix3x2fStack;
import org.lwjgl.glfw.GLFW;

import cc.fascinated.fascinatedutils.common.Colors;
import cc.fascinated.fascinatedutils.gui.input.MouseButtons;
import cc.fascinated.fascinatedutils.gui.renderer.GuiRenderer;
import cc.fascinated.fascinatedutils.gui.renderer.RectCornerRoundMask;
import cc.fascinated.fascinatedutils.gui.theme.Icons;
import cc.fascinated.fascinatedutils.gui.theme.ModSettingsTheme;
import cc.fascinated.fascinatedutils.gui.theme.UITheme;
import cc.fascinated.fascinatedutils.gui.widgets.FShellTabStripWidget;
import cc.fascinated.fascinatedutils.gui.widgets.FWidgetHost;
import cc.fascinated.fascinatedutils.renderer.Renderer2D;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

public class ModSettingsShellFrame {

    @FunctionalInterface
    public interface ShellBodyMounter {
        void beforeBodyLayout(float bodyWidth, float bodyHeight);
    }

    /**
     * Lays out hosted widgets, draws scrim and shell chrome, and renders the three widget hosts.
     *
     * @param graphics            draw context for matrix stack
     * @param canvasWidth         canvas width in UI pixels
     * @param canvasHeight        canvas height in UI pixels
     * @param glUiRenderer        GUI renderer for this pass
     * @param titleStr            window title string
     * @param pointerScreenX      pointer X in screen space for shell mapping
     * @param pointerScreenY      pointer Y in screen space for shell mapping
     * @param deltaSeconds        animation delta in seconds
     * @param minecraftClient     client for window handle and mouse state
     * @param topBarTabStrip      shell tab strip widget
     * @param selectedShellTabKey tab key to reflect selection
     * @param bodyHost            body widget host
     * @param topBarTabsHost      host wrapping the tab strip
     * @param hudLayoutButtonHost host wrapping the HUD layout chip
     * @param mountBodyIfNeeded   callback that mounts the body root when dimensions or tab require a rebuild
     * @return hit regions and layout pointer coordinates for this frame
     */
    public static ModSettingsShellFrameResult render(GuiGraphicsExtractor graphics, float canvasWidth, float canvasHeight, GuiRenderer glUiRenderer, String titleStr, float pointerScreenX, float pointerScreenY, float deltaSeconds, Minecraft minecraftClient, FShellTabStripWidget topBarTabStrip, String selectedShellTabKey, FWidgetHost bodyHost, FWidgetHost topBarTabsHost, FWidgetHost hudLayoutButtonHost, ShellBodyMounter mountBodyIfNeeded) {
        ModSettingsShellLayout.ShellBounds shell = ModSettingsShellLayout.computeShell(canvasWidth, canvasHeight);
        ModSettingsShellLayout.ShellPointerMapping shellMapping = ModSettingsShellLayout.computePointMapping(shell);
        float pointerLayoutX = shellMapping.layoutX(pointerScreenX);
        float pointerLayoutY = shellMapping.layoutY(pointerScreenY);
        float topBarHeight = ModSettingsTheme.TOPBAR_HEIGHT;
        ModSettingsShellLayout.ShellBounds topBar = new ModSettingsShellLayout.ShellBounds(shell.positionX(), shell.positionY(), shell.width(), topBarHeight);
        float shellStrokeRequest = UITheme.BORDER_THICKNESS_PX;
        float shellBorderThickness = Renderer2D.roundedRectFrameBorderThickness(shellStrokeRequest, shellStrokeRequest);
        float innerTitleTop = topBar.positionY() + shellBorderThickness;
        float innerTitleHeight = Math.max(0f, topBar.height() - shellBorderThickness);
        float shellCornerRadius = ModSettingsTheme.SHELL_CORNER_RADIUS;
        float closeButtonInset = 4f;
        float closeButtonSize = ModSettingsTheme.titleBarSquareControlSizePx();
        ModSettingsShellLayout.ShellBounds closeButtonRect = closeButtonBoundsForTopBar(topBar, shellBorderThickness, innerTitleTop, innerTitleHeight, closeButtonInset, closeButtonSize);
        float bodyTop = shell.positionY() + topBarHeight;
        float bodyHeight = Math.max(0f, shell.height() - topBarHeight - shellBorderThickness);
        float bodyPositionX = shell.positionX() + shellBorderThickness;
        float bodyWidth = Math.max(0f, shell.width() - 2f * shellBorderThickness);
        boolean closeOver = closeButtonRect.contains(pointerLayoutX, pointerLayoutY);
        boolean closePress = closeOver && (mouseButtonMask(minecraftClient.getWindow().handle()) & MouseButtons.LEFT) != 0;
        int closeBg = closePress ? glUiRenderer.theme().moduleListRowHover() : closeOver ? glUiRenderer.theme().surfaceElevated() : glUiRenderer.theme().surface();
        int closeBorder = closeOver ? glUiRenderer.theme().borderHover() : glUiRenderer.theme().border();
        int closeText = closeOver ? glUiRenderer.theme().textPrimary() : glUiRenderer.theme().textMuted();
        float titleLeftInset = 7f;
        int titleTextWidth = glUiRenderer.measureTextWidth(titleStr, false);
        boolean showHudLayoutChip = hudLayoutButtonHost.root() != null;
        ModSettingsShellLayout.ShellBounds hudLayoutButtonRect;
        if (showHudLayoutChip) {
            String hudLayoutLabel = Component.translatable("fascinatedutils.setting.shell.edit_hud_layout").getString();
            float hudButtonW = Math.max(34f, glUiRenderer.measureTextWidth(hudLayoutLabel, false) + 2f * FHudLayoutEditorChipWidget.HORIZONTAL_TEXT_PAD_DESIGN);
            float hudButtonH = FHudLayoutEditorChipWidget.chipHeightPx();
            float betweenHudAndClose = 3f;
            float hudButtonX = closeButtonRect.positionX() - betweenHudAndClose - hudButtonW;
            float hudButtonY = innerTitleTop + (innerTitleHeight - hudButtonH) * 0.5f;
            hudLayoutButtonRect = new ModSettingsShellLayout.ShellBounds(hudButtonX, hudButtonY, hudButtonW, hudButtonH);
        }
        else {
            hudLayoutButtonRect = new ModSettingsShellLayout.ShellBounds(0f, 0f, 0f, 0f);
        }
        float titleStartX = topBar.positionX() + shellBorderThickness + titleLeftInset;
        // Give the tab strip the full inner title bar width so FShellTabStripWidget can
        // center the pill track against the true midpoint of the title bar.
        float titleBarInnerLeft = topBar.positionX() + shellBorderThickness;
        float titleBarInnerRight = topBar.positionX() + topBar.width() - shellBorderThickness;
        float topBarTabsWidth = Math.max(0f, titleBarInnerRight - titleBarInnerLeft);
        ModSettingsShellLayout.ShellBounds topBarTabsRect = new ModSettingsShellLayout.ShellBounds(titleBarInnerLeft, innerTitleTop, topBarTabsWidth, innerTitleHeight);
        float scrimAlpha = 1f;
        Matrix3x2fStack drawMatrices = graphics.pose();
        drawMatrices.pushMatrix();
        try {
            glUiRenderer.setMultiplyAlpha(scrimAlpha);
            glUiRenderer.drawRect(0f, 0f, canvasWidth, canvasHeight, ModSettingsTheme.SCRIM);
            glUiRenderer.resetMultiplyAlpha();
            drawMatrices.pushMatrix();
            drawMatrices.translate(shellMapping.centerX(), shellMapping.centerY());
            drawMatrices.scale(shellMapping.scale(), shellMapping.scale());
            drawMatrices.translate(-shellMapping.centerX(), -shellMapping.centerY());
            try {
                mountBodyIfNeeded.beforeBodyLayout(bodyWidth, bodyHeight);
                bodyHost.layoutOnly(glUiRenderer, bodyPositionX, bodyTop, bodyWidth, bodyHeight);
                topBarTabStrip.setSelectedKey(selectedShellTabKey);
                topBarTabsHost.layoutOnly(glUiRenderer, topBarTabsRect.positionX(), topBarTabsRect.positionY(), topBarTabsRect.width(), topBarTabsRect.height());
                if (showHudLayoutChip) {
                    hudLayoutButtonHost.layoutOnly(glUiRenderer, hudLayoutButtonRect.positionX(), hudLayoutButtonRect.positionY(), hudLayoutButtonRect.width(), hudLayoutButtonRect.height());
                }
                glUiRenderer.setMultiplyAlpha(1f, 1f);
                float closeButtonCornerRadius = Math.min(4f, closeButtonRect.height() * 0.5f - 0.01f);
                glUiRenderer.fillRoundedRectFrame(shell.positionX(), shell.positionY(), shell.width(), shell.height(), shellCornerRadius, ModSettingsTheme.SHELL_BORDER, glUiRenderer.theme().surface(), shellBorderThickness, shellBorderThickness, RectCornerRoundMask.ALL);
                float titleFillX = topBar.positionX() + shellBorderThickness;
                float titleFillY = topBar.positionY() + shellBorderThickness;
                float titleFillW = topBar.width() - 2f * shellBorderThickness;
                float titleFillH = topBar.height() - shellBorderThickness;
                float titleBarInnerCornerRadius = Math.min(Math.max(0f, shellCornerRadius - shellBorderThickness), Math.min(titleFillW, titleFillH) * 0.5f - 0.01f);
                glUiRenderer.fillRoundedRect(titleFillX, titleFillY, titleFillW, titleFillH, titleBarInnerCornerRadius, glUiRenderer.theme().background(), RectCornerRoundMask.TOP);
                glUiRenderer.fillRoundedRectFrame(closeButtonRect.positionX(), closeButtonRect.positionY(), closeButtonRect.width(), closeButtonRect.height(), closeButtonCornerRadius, closeBorder, closeBg, UITheme.BORDER_THICKNESS_PX, UITheme.BORDER_THICKNESS_PX, RectCornerRoundMask.ALL);
                Icons.paintModSettingsCloseIcon(glUiRenderer, closeButtonRect.positionX(), closeButtonRect.positionY(), closeButtonRect.width(), closeButtonRect.height(), closeText);
                glUiRenderer.drawMiniMessageText("<color:" + Colors.rgbHex(glUiRenderer.theme().textPrimary()) + ">" + titleStr + "</color>", titleStartX, innerTitleTop + (innerTitleHeight - glUiRenderer.getFontCapHeight()) * 0.5f, false);
                topBarTabsHost.renderOnly(glUiRenderer, pointerLayoutX, pointerLayoutY, deltaSeconds);
                if (showHudLayoutChip) {
                    hudLayoutButtonHost.renderOnly(glUiRenderer, pointerLayoutX, pointerLayoutY, deltaSeconds);
                }
                bodyHost.renderOnly(glUiRenderer, pointerLayoutX, pointerLayoutY, deltaSeconds);
                glUiRenderer.resetMultiplyAlpha();
            } finally {
                drawMatrices.popMatrix();
            }
        } finally {
            glUiRenderer.end();
            drawMatrices.popMatrix();
        }
        return new ModSettingsShellFrameResult(new ModSettingsShellHitRegions(closeButtonRect, topBarTabsRect, hudLayoutButtonRect, bodyPositionX, bodyTop, bodyWidth, bodyHeight), pointerLayoutX, pointerLayoutY);
    }

    private static int mouseButtonMask(long window) {
        int mask = 0;
        for (int buttonIndex = 0; buttonIndex <= 2; buttonIndex++) {
            if (GLFW.glfwGetMouseButton(window, buttonIndex) == GLFW.GLFW_PRESS) {
                mask |= MouseButtons.bitForGlfwButton(buttonIndex);
            }
        }
        return mask;
    }

    private static ModSettingsShellLayout.ShellBounds closeButtonBoundsForTopBar(ModSettingsShellLayout.ShellBounds topBar, float shellBorderThickness, float innerTitleTop, float innerTitleHeight, float closeButtonInset, float closeButtonSize) {
        float closeButtonX = Mth.floor(topBar.positionX() + topBar.width() - shellBorderThickness - closeButtonInset - closeButtonSize);
        float closeButtonY = Mth.floor(innerTitleTop + (innerTitleHeight - closeButtonSize) * 0.5f + 0.5f);
        return new ModSettingsShellLayout.ShellBounds(closeButtonX, closeButtonY, closeButtonSize, closeButtonSize);
    }
}
