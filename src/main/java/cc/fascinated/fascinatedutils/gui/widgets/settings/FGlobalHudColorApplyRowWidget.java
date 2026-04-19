package cc.fascinated.fascinatedutils.gui.widgets.settings;

import cc.fascinated.fascinatedutils.common.ColorUtils;
import cc.fascinated.fascinatedutils.common.color.RainbowColors;
import cc.fascinated.fascinatedutils.common.color.SettingColor;
import cc.fascinated.fascinatedutils.common.setting.impl.ColorSetting;
import cc.fascinated.fascinatedutils.gui.GuiDesignSpace;
import cc.fascinated.fascinatedutils.gui.core.TextLineLayout;
import cc.fascinated.fascinatedutils.gui.renderer.GuiRenderer;
import cc.fascinated.fascinatedutils.gui.renderer.RectCornerRoundMask;
import cc.fascinated.fascinatedutils.gui.renderer.UIRenderer;
import cc.fascinated.fascinatedutils.gui.theme.ModSettingsTheme;
import cc.fascinated.fascinatedutils.gui.theme.SettingsUiMetrics;
import cc.fascinated.fascinatedutils.gui.theme.UITheme;
import cc.fascinated.fascinatedutils.gui.themes.fascinated.FascinatedGuiTheme;
import cc.fascinated.fascinatedutils.gui.widgets.FWidget;
import cc.fascinated.fascinatedutils.gui.widgets.WTooltip;
import net.minecraft.client.resources.language.I18n;

import java.util.function.Consumer;

/**
 * Widgets-tab row: color swatch bound to a staging {@link ColorSetting}, plus a trailing chip that applies the staged
 * color to a target registry setting.
 */
public class FGlobalHudColorApplyRowWidget extends FWidget {

    private static final float SWATCH_SIZE_DESIGN = 10f;
    private static final float SWATCH_CORNER_RADIUS_DESIGN = 2f;
    private static final float APPLY_CHIP_WIDTH_DESIGN = 112f;
    private static final float APPLY_HORIZONTAL_PAD_DESIGN = 10f;
    private static final float APPLY_LABEL_GAP_DESIGN = 8f;
    private final ColorSetting stagingColor;
    private final float outerWidth;
    private final float outerHeight;
    private final float valueColumnStartX;
    private final Runnable onApplyAll;
    private final Consumer<ColorSetting> openColorPicker;
    private boolean hoveredSwatch;
    private boolean hoveredApply;

    public FGlobalHudColorApplyRowWidget(ColorSetting stagingColor, float outerWidth, float outerHeight, float valueColumnStartX, Runnable onApplyAll, Consumer<ColorSetting> openColorPicker) {
        this.stagingColor = stagingColor;
        this.outerWidth = outerWidth;
        this.outerHeight = outerHeight;
        this.valueColumnStartX = Math.max(0f, valueColumnStartX);
        this.onApplyAll = onApplyAll;
        this.openColorPicker = openColorPicker;
    }

    private static boolean rectContains(float[] rect, float pointerX, float pointerY) {
        return pointerX >= rect[0] && pointerY >= rect[1] && pointerX < rect[0] + rect[2] && pointerY < rect[1] + rect[3];
    }

    @Override
    public float intrinsicHeightForColumn(UIRenderer measure, float widthBudget) {
        return outerHeight;
    }

    @Override
    public float intrinsicWidthForRow(UIRenderer measure, float heightBudget) {
        return outerWidth;
    }

    @Override
    public void layout(UIRenderer measure, float layoutX, float layoutY, float layoutWidth, float layoutHeight) {
        setBounds(layoutX, layoutY, layoutWidth, layoutHeight);
    }

    @Override
    public boolean wantsPointer() {
        return true;
    }

    @Override
    public boolean mouseLeave(float pointerX, float pointerY) {
        hoveredSwatch = false;
        hoveredApply = false;
        return false;
    }

    @Override
    public boolean mouseMove(float pointerX, float pointerY) {
        float[] swatch = swatchBounds();
        hoveredSwatch = rectContains(swatch, pointerX, pointerY);
        hoveredApply = rectContains(applyChipBounds(), pointerX, pointerY);
        return false;
    }

    @Override
    public boolean mouseDown(float pointerX, float pointerY, int button) {
        if (button != 0) {
            return false;
        }
        if (stagingColor.isLocked()) {
            return hoveredSwatch || hoveredApply;
        }
        if (rectContains(applyChipBounds(), pointerX, pointerY)) {
            return true;
        }
        return rectContains(swatchBounds(), pointerX, pointerY);
    }

    @Override
    public boolean click(float pointerX, float pointerY, int button) {
        if (button != 0) {
            return false;
        }
        if (stagingColor.isLocked()) {
            return hoveredSwatch || hoveredApply;
        }
        if (rectContains(applyChipBounds(), pointerX, pointerY)) {
            onApplyAll.run();
            return true;
        }
        if (rectContains(swatchBounds(), pointerX, pointerY)) {
            openColorPicker.accept(stagingColor);
            return true;
        }
        return false;
    }

    @Override
    public void renderOverlayAfterChildren(GuiRenderer graphics, float mouseX, float mouseY, float deltaSeconds) {
        if (hoveredSwatch) {
            WSettingTooltip.drawTooltipForSetting(graphics, mouseX, mouseY, stagingColor);
        }
        else if (hoveredApply) {
            String tooltip = I18n.get("fascinatedutils.setting.shell.global_hud_apply_all.description");
            if (tooltip != null && !tooltip.isBlank()) {
                WTooltip.draw(graphics, mouseX, mouseY, tooltip);
            }
        }
    }

    @Override
    protected void renderSelf(GuiRenderer graphics, float mouseX, float mouseY, float deltaSeconds) {
        boolean locked = stagingColor.isLocked();
        float padY = GuiDesignSpace.pxY(SettingsUiMetrics.SETTING_ROW_PADDING_Y);
        float padX = GuiDesignSpace.pxX(SettingsUiMetrics.SETTING_ROW_PADDING_X);
        float bodyLeft = x() + padX;
        float innerHeight = Math.max(0f, h() - 2f * padY);

        float swatchSize = GuiDesignSpace.pxUniform(SWATCH_SIZE_DESIGN);
        float titleRowHeight = Math.max(GuiDesignSpace.pxY(ModSettingsTheme.shellDesignBodyLineHeight()), swatchSize);
        float titleOriginY = y() + padY + (innerHeight - titleRowHeight) * 0.5f;

        String label = stagingColor.getTranslatedDisplayName();
        float[] swatch = swatchBounds();
        float labelMaxWidth = Math.max(0f, swatch[0] - GuiDesignSpace.pxX(SettingsUiMetrics.SETTING_VALUE_CONTROL_GAP) - bodyLeft);
        label = TextLineLayout.ellipsize(label, labelMaxWidth, segment -> graphics.measureTextWidth(segment, false));
        float labelY = titleOriginY + Math.max(0f, (swatchSize - graphics.getFontHeight()) * 0.5f);
        int labelColor = locked ? graphics.theme().textMuted() : graphics.theme().textPrimary();
        graphics.drawMiniMessageText("<color:" + ColorUtils.rgbHex(labelColor) + ">" + label + "</color>", bodyLeft, labelY, false);

        SettingColor color = stagingColor.getValue();
        int swatchArgb = color.isRainbow() ? RainbowColors.currentColor().getPackedArgb() | 0xFF000000 : color.getPackedArgb();

        float cornerRadius = GuiDesignSpace.pxUniform(SWATCH_CORNER_RADIUS_DESIGN);
        float borderPx = GuiDesignSpace.pxUniform(1f);
        int swatchBorder = hoveredSwatch && !locked ? graphics.theme().accentBright() : graphics.theme().border();
        graphics.fillRoundedRectFrame(swatch[0], swatch[1], swatch[2], swatch[3], cornerRadius, swatchBorder, swatchArgb, borderPx, borderPx, RectCornerRoundMask.ALL);

        if (color.isRainbow()) {
            float rainbowLabelX = swatch[0] + swatch[2] + GuiDesignSpace.pxX(4f);
            int rainbowTextColor = locked ? graphics.theme().textMuted() : graphics.theme().textAccent();
            graphics.drawMiniMessageText("<color:" + ColorUtils.rgbHex(rainbowTextColor) + ">Rainbow</color>", rainbowLabelX, labelY, false);
        }

        float[] apply = applyChipBounds();
        int fillColor = FascinatedGuiTheme.INSTANCE.surface();
        int outlineColor = hoveredApply ? FascinatedGuiTheme.INSTANCE.borderHover() : FascinatedGuiTheme.INSTANCE.border();
        float chipBorderX = GuiDesignSpace.pxX(UITheme.BORDER_THICKNESS_PX);
        float chipBorderY = GuiDesignSpace.pxY(UITheme.BORDER_THICKNESS_PX);
        float chipCorner = resolveApplyChipCornerRadius(graphics, apply[2], apply[3]);
        graphics.fillRoundedRectFrame(apply[0], apply[1], apply[2], apply[3], chipCorner, outlineColor, fillColor, chipBorderX, chipBorderY, RectCornerRoundMask.ALL);
        String applyLabel = I18n.get("fascinatedutils.setting.shell.global_hud_apply_all");
        float wrapBudget = Math.max(1f, apply[2] - 2f * GuiDesignSpace.pxX(APPLY_HORIZONTAL_PAD_DESIGN));
        applyLabel = TextLineLayout.ellipsize(applyLabel, wrapBudget, segment -> graphics.measureTextWidth(segment, false));
        float textX = apply[0] + (apply[2] - graphics.measureTextWidth(applyLabel, false)) * 0.5f;
        float textY = apply[1] + (apply[3] - graphics.getFontHeight()) * 0.5f;
        graphics.drawMiniMessageText("<color:" + ColorUtils.rgbHex(graphics.theme().textPrimary()) + ">" + applyLabel + "</color>", textX, textY, false);
    }

    private float resolveApplyChipCornerRadius(GuiRenderer graphics, float chipWidth, float chipHeight) {
        float maxRadius = Math.min(chipWidth, chipHeight) * 0.5f - 0.01f;
        float themed = GuiDesignSpace.pxUniform(graphics.theme().cardCornerRadius());
        return Math.max(0.5f, Math.min(themed, maxRadius));
    }

    private float[] swatchBounds() {
        float padY = GuiDesignSpace.pxY(SettingsUiMetrics.SETTING_ROW_PADDING_Y);
        float padX = GuiDesignSpace.pxX(SettingsUiMetrics.SETTING_ROW_PADDING_X);
        float innerHeight = Math.max(0f, h() - 2f * padY);
        float swatchSize = GuiDesignSpace.pxUniform(SWATCH_SIZE_DESIGN);
        float titleRowHeight = Math.max(GuiDesignSpace.pxY(ModSettingsTheme.shellDesignBodyLineHeight()), swatchSize);
        float titleOriginY = y() + padY + (innerHeight - titleRowHeight) * 0.5f;
        float bodyLeft = x() + padX;
        float applyLeft = applyChipLeftX();
        float maxSwatchLeft = Math.max(bodyLeft, applyLeft - GuiDesignSpace.pxX(APPLY_LABEL_GAP_DESIGN) - swatchSize);
        float swatchLeft = Math.min(bodyLeft + valueColumnStartX, maxSwatchLeft);
        return new float[]{swatchLeft, titleOriginY, swatchSize, swatchSize};
    }

    private float applyChipLeftX() {
        float padX = GuiDesignSpace.pxX(SettingsUiMetrics.SETTING_ROW_PADDING_X);
        float chipWidth = GuiDesignSpace.pxX(APPLY_CHIP_WIDTH_DESIGN);
        return x() + w() - padX - chipWidth;
    }

    private float[] applyChipBounds() {
        float padY = GuiDesignSpace.pxY(SettingsUiMetrics.SETTING_ROW_PADDING_Y);
        float innerHeight = Math.max(0f, h() - 2f * padY);
        float chipWidth = GuiDesignSpace.pxX(APPLY_CHIP_WIDTH_DESIGN);
        float swatchSize = GuiDesignSpace.pxUniform(SWATCH_SIZE_DESIGN);
        float chipHeight = Math.max(swatchSize, GuiDesignSpace.pxY(SettingsUiMetrics.SHELL_CONTROL_HEIGHT_DESIGN));
        chipHeight = Math.min(chipHeight, innerHeight);
        float chipLeft = applyChipLeftX();
        float bodyTop = y() + padY;
        float chipTop = bodyTop + Math.max(0f, (innerHeight - chipHeight) * 0.5f);
        return new float[]{chipLeft, chipTop, chipWidth, chipHeight};
    }
}
