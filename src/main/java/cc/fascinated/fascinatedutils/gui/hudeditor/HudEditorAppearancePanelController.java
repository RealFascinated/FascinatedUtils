package cc.fascinated.fascinatedutils.gui.hudeditor;

import cc.fascinated.fascinatedutils.common.setting.Setting;
import cc.fascinated.fascinatedutils.common.setting.impl.ColorSetting;
import cc.fascinated.fascinatedutils.gui.GuiDesignSpace;
import cc.fascinated.fascinatedutils.gui.UIScale;
import cc.fascinated.fascinatedutils.gui.core.InputEvent;
import cc.fascinated.fascinatedutils.gui.core.Ref;
import cc.fascinated.fascinatedutils.gui.modsettings.ColorPickerPopupWidget;
import cc.fascinated.fascinatedutils.gui.modsettings.ModSettingsCategoryRows;
import cc.fascinated.fascinatedutils.gui.modsettings.ModSettingsWidgetsTabBuilder;
import cc.fascinated.fascinatedutils.gui.renderer.GuiRenderer;
import cc.fascinated.fascinatedutils.gui.renderer.RectCornerRoundMask;
import cc.fascinated.fascinatedutils.gui.theme.UiColor;
import cc.fascinated.fascinatedutils.gui.widgets.FAbsoluteStackWidget;
import cc.fascinated.fascinatedutils.gui.widgets.FWidget;
import cc.fascinated.fascinatedutils.gui.widgets.FWidgetHost;
import cc.fascinated.fascinatedutils.systems.hud.HUDManager;
import cc.fascinated.fascinatedutils.systems.hud.HudModule;
import net.minecraft.util.Mth;
import org.jspecify.annotations.Nullable;

public class HudEditorAppearancePanelController {

    private static final int APPEARANCE_CARD_BACKGROUND = UiColor.argb("#d22a2434");
    private static final float APPEARANCE_PANEL_MARGIN = 6f;

    private final FWidgetHost appearancePanelHost = new FWidgetHost();
    private final Ref<Float> appearancePanelScrollY = Ref.of(0f);
    @Nullable
    private HudModule appearancePanelModel;
    private float appearancePanelContentWidth;
    private float appearancePanelLeft = Float.NaN;
    private float appearancePanelTop = Float.NaN;
    private float appearancePanelWidth;
    private float appearancePanelHeight;
    private boolean showColorPicker;
    @Nullable
    private ColorSetting activeColorSetting;

    private static boolean hasRenderableHudAppearanceSetting(HudModule model) {
        for (Setting<?> setting : model.getAllSettings()) {
            if (ModSettingsCategoryRows.isRenderableSetting(setting)) {
                return true;
            }
        }
        return false;
    }

    private static float computeAppearanceContentWidth(float canvasWidth) {
        float maxByCanvas = canvasWidth - APPEARANCE_PANEL_MARGIN * 2f;
        float target = Math.min(GuiDesignSpace.pxX(400f), maxByCanvas);
        return Math.max(GuiDesignSpace.pxX(240f), target);
    }

    private static boolean rectContains(float rectLeft, float rectTop, float rectWidth, float rectHeight, float pointerX, float pointerY) {
        return pointerX >= rectLeft && pointerY >= rectTop && pointerX <= rectLeft + rectWidth && pointerY <= rectTop + rectHeight;
    }

    /**
     * Clears cached panel bounds at the start of a frame before layout.
     */
    public void clearBoundsForFrame() {
        appearancePanelLeft = Float.NaN;
        appearancePanelTop = Float.NaN;
        appearancePanelWidth = 0f;
        appearancePanelHeight = 0f;
    }

    /**
     * Disposes hosted widgets when the editor closes.
     */
    public void dispose() {
        appearancePanelHost.dispose();
        appearancePanelModel = null;
    }

    public FWidgetHost host() {
        return appearancePanelHost;
    }

    /**
     * Whether the pointer lies inside the last laid-out appearance panel bounds.
     *
     * @param pointerX pointer X in logical space
     * @param pointerY pointer Y in logical space
     * @return true when the panel is hit-testable at the point
     */
    public boolean containsPoint(float pointerX, float pointerY) {
        return Float.isFinite(appearancePanelLeft) && rectContains(appearancePanelLeft, appearancePanelTop, appearancePanelWidth, appearancePanelHeight, pointerX, pointerY);
    }

    /**
     * Clears the panel when the selection is not eligible for appearance editing.
     */
    public void clearPanel() {
        appearancePanelHost.setRoot(null);
        appearancePanelModel = null;
        showColorPicker = false;
        activeColorSetting = null;
        clearBoundsForFrame();
    }

    /**
     * Lays out and draws the appearance settings panel for the selected widget.
     *
     * @param glRenderer     renderer for this pass
     * @param selectedWidget widget whose settings are shown
     * @param canvasWidth    logical canvas width
     * @param canvasHeight   logical canvas height
     * @param deltaSeconds   animation delta in seconds
     */
    public void layoutAndDraw(GuiRenderer glRenderer, HudModule selectedWidget, float canvasWidth, float canvasHeight, float deltaSeconds) {
        if (!hasRenderableHudAppearanceSetting(selectedWidget)) {
            appearancePanelHost.setRoot(null);
            appearancePanelModel = null;
            clearBoundsForFrame();
            return;
        }
        if (selectedWidget != appearancePanelModel) {
            appearancePanelModel = selectedWidget;
            appearancePanelScrollY.setValue(0f);
            showColorPicker = false;
            activeColorSetting = null;
            appearancePanelContentWidth = computeAppearanceContentWidth(canvasWidth);
            appearancePanelHost.setRoot(buildHudAppearancePanelRoot(selectedWidget, appearancePanelContentWidth));
        }
        float contentWidth = appearancePanelContentWidth;
        float panelHeight = Mth.clamp(canvasHeight * 0.42f, GuiDesignSpace.pxY(140f), GuiDesignSpace.pxY(340f));
        float desiredLeft = selectedWidget.getHudState().getPositionX() + (selectedWidget.getScaledWidth() - contentWidth) * 0.5f;
        float maxLeft = Math.max(0f, canvasWidth - contentWidth - APPEARANCE_PANEL_MARGIN);
        float panelLeft = Mth.clamp(desiredLeft, APPEARANCE_PANEL_MARGIN, maxLeft);

        float belowTop = selectedWidget.getHudState().getPositionY() + selectedWidget.getScaledHeight() + APPEARANCE_PANEL_MARGIN;
        float aboveTop = selectedWidget.getHudState().getPositionY() - panelHeight - APPEARANCE_PANEL_MARGIN;
        float maxTop = Math.max(0f, canvasHeight - panelHeight - APPEARANCE_PANEL_MARGIN);
        boolean belowFits = belowTop >= APPEARANCE_PANEL_MARGIN && belowTop <= maxTop;
        boolean aboveFits = aboveTop >= APPEARANCE_PANEL_MARGIN && aboveTop <= maxTop;

        float panelTop;
        if (belowFits) {
            panelTop = belowTop;
        }
        else if (aboveFits) {
            panelTop = aboveTop;
        }
        else {
            float spaceBelow = canvasHeight - (selectedWidget.getHudState().getPositionY() + selectedWidget.getScaledHeight()) - APPEARANCE_PANEL_MARGIN;
            float spaceAbove = selectedWidget.getHudState().getPositionY() - APPEARANCE_PANEL_MARGIN;
            float preferredTop = spaceBelow >= spaceAbove ? belowTop : aboveTop;
            panelTop = Mth.clamp(preferredTop, APPEARANCE_PANEL_MARGIN, maxTop);
        }

        appearancePanelLeft = panelLeft;
        appearancePanelTop = panelTop;
        appearancePanelWidth = contentWidth;
        appearancePanelHeight = panelHeight;
        appearancePanelHost.tickAnimations(deltaSeconds);
        appearancePanelHost.layoutOnly(glRenderer, panelLeft, panelTop, contentWidth, panelHeight);
        float pointerX = UIScale.logicalPointerX();
        float pointerY = UIScale.logicalPointerY();
        paintAppearancePanel(glRenderer, panelLeft, panelTop, contentWidth, panelHeight, pointerX, pointerY, deltaSeconds);
    }

    private void paintAppearancePanel(GuiRenderer glRenderer, float panelLeft, float panelTop, float contentWidth, float panelHeight, float pointerX, float pointerY, float deltaSeconds) {
        float borderThickness = GuiDesignSpace.pxUniform(1f);
        float cornerRadius = Mth.clamp(GuiDesignSpace.pxUniform(glRenderer.theme().cardCornerRadius()), 0.5f, Math.min(contentWidth, panelHeight) * 0.5f - borderThickness * 0.5f - 0.01f);
        glRenderer.fillRoundedRectFrame(panelLeft, panelTop, contentWidth, panelHeight, cornerRadius, glRenderer.theme().border(), APPEARANCE_CARD_BACKGROUND, borderThickness, borderThickness, RectCornerRoundMask.ALL);
        appearancePanelHost.renderOnly(glRenderer, pointerX, pointerY, deltaSeconds);
    }

    /**
     * Dispatches a mouse move to the appearance host when a root is mounted.
     */
    public void dispatchMouseMoveLogical() {
        if (appearancePanelHost.root() != null) {
            float pointerX = UIScale.logicalPointerX();
            float pointerY = UIScale.logicalPointerY();
            appearancePanelHost.dispatchInput(new InputEvent.MouseMove(pointerX, pointerY));
        }
    }

    private FWidget buildHudAppearancePanelRoot(HudModule model, float settingsContentWidth) {
        FWidget settingsScroll = ModSettingsWidgetsTabBuilder.buildHudWidgetAppearanceSettingsScroll(model, settingsContentWidth, model.getName() + " Appearance", appearancePanelScrollY, this::openColorPicker);

        if (!showColorPicker || activeColorSetting == null) {
            return settingsScroll;
        }

        FAbsoluteStackWidget rootStack = new FAbsoluteStackWidget();
        rootStack.addChild(settingsScroll);
        ColorSetting captured = activeColorSetting;
        rootStack.addChild(new ColorPickerPopupWidget(captured.getValue(), newColor -> {
            captured.setValue(newColor);
            HUDManager.INSTANCE.saveAll();
            closeColorPicker();
        }, this::closeColorPicker));
        return rootStack;
    }

    private void openColorPicker(ColorSetting colorSetting) {
        if (showColorPicker) {
            return;
        }
        activeColorSetting = colorSetting;
        showColorPicker = true;
        rebuildPanel();
    }

    private void closeColorPicker() {
        if (!showColorPicker) {
            return;
        }
        showColorPicker = false;
        activeColorSetting = null;
        rebuildPanel();
    }

    private void rebuildPanel() {
        if (appearancePanelModel != null) {
            appearancePanelHost.setRoot(buildHudAppearancePanelRoot(appearancePanelModel, appearancePanelContentWidth));
        }
    }
}
