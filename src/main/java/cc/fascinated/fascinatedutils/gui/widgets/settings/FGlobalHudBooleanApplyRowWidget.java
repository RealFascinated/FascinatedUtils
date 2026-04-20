package cc.fascinated.fascinatedutils.gui.widgets.settings;

import cc.fascinated.fascinatedutils.common.ColorUtils;
import cc.fascinated.fascinatedutils.common.setting.impl.BooleanSetting;
import cc.fascinated.fascinatedutils.gui.core.TextLineLayout;
import cc.fascinated.fascinatedutils.gui.hooks.AnimHandle;
import cc.fascinated.fascinatedutils.gui.renderer.GuiRenderer;
import cc.fascinated.fascinatedutils.gui.renderer.RectCornerRoundMask;
import cc.fascinated.fascinatedutils.gui.renderer.UIRenderer;
import cc.fascinated.fascinatedutils.gui.theme.ModSettingsTheme;
import cc.fascinated.fascinatedutils.gui.theme.SettingsUiMetrics;
import cc.fascinated.fascinatedutils.gui.theme.UITheme;
import cc.fascinated.fascinatedutils.gui.themes.fascinated.FascinatedGuiTheme;
import cc.fascinated.fascinatedutils.gui.widgets.FAnimatable;
import cc.fascinated.fascinatedutils.gui.widgets.FWidget;
import cc.fascinated.fascinatedutils.gui.widgets.WTooltip;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.util.Mth;

/**
 * Widgets-tab row: leading boolean toggle (staging only), label, and a trailing chip that applies the staged value to
 * every HUD module.
 */
public class FGlobalHudBooleanApplyRowWidget extends FWidget implements FAnimatable {

    private static final float TOGGLE_CORNER_FILLET_DESIGN = 2.5f;
    private static final float APPLY_CHIP_WIDTH_DESIGN = 112f;
    private static final float APPLY_HORIZONTAL_PAD_DESIGN = 10f;
    private static final float APPLY_LABEL_GAP_DESIGN = 8f;
    private final BooleanSetting stagingBoolean;
    private final float outerWidth;
    private final float outerHeight;
    private final Runnable onApplyAll;
    private final AnimHandle toggleProgressAnim = new AnimHandle(0f).speed(26f);
    private boolean hoveredToggle;
    private boolean hoveredApply;

    public FGlobalHudBooleanApplyRowWidget(BooleanSetting stagingBoolean, float outerWidth, float outerHeight, Runnable onApplyAll) {
        this.stagingBoolean = stagingBoolean;
        this.outerWidth = outerWidth;
        this.outerHeight = outerHeight;
        this.onApplyAll = onApplyAll;
        float initial = Boolean.TRUE.equals(stagingBoolean.getValue()) ? 1f : 0f;
        this.toggleProgressAnim.snap(initial);
    }

    private static boolean rectContains(float rectLeft, float rectTop, float rectWidth, float rectHeight, float pointerX, float pointerY) {
        return pointerX >= rectLeft && pointerY >= rectTop && pointerX < rectLeft + rectWidth && pointerY < rectTop + rectHeight;
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
        hoveredToggle = false;
        hoveredApply = false;
        return false;
    }

    @Override
    public boolean mouseMove(float pointerX, float pointerY) {
        hoveredToggle = rectContainsToggle(pointerX, pointerY);
        hoveredApply = rectContainsApply(pointerX, pointerY);
        return false;
    }

    @Override
    public boolean mouseDown(float pointerX, float pointerY, int button) {
        return button == 0 && (rectContainsToggle(pointerX, pointerY) || rectContainsApply(pointerX, pointerY));
    }

    @Override
    public boolean click(float pointerX, float pointerY, int button) {
        if (button != 0) {
            return false;
        }
        if (stagingBoolean.isLocked()) {
            return hoveredToggle || hoveredApply;
        }
        if (rectContainsApply(pointerX, pointerY)) {
            onApplyAll.run();
            return true;
        }
        if (rectContainsToggle(pointerX, pointerY)) {
            stagingBoolean.setValue(!Boolean.TRUE.equals(stagingBoolean.getValue()));
            toggleProgressAnim.target(Boolean.TRUE.equals(stagingBoolean.getValue()) ? 1f : 0f);
            return true;
        }
        return false;
    }

    @Override
    public void tickAnims(float deltaSeconds) {
        toggleProgressAnim.target(Boolean.TRUE.equals(stagingBoolean.getValue()) ? 1f : 0f);
        toggleProgressAnim.tick(deltaSeconds);
    }

    @Override
    public void renderOverlayAfterChildren(GuiRenderer graphics, float mouseX, float mouseY, float deltaSeconds) {
        if (hoveredToggle) {
            WSettingTooltip.drawTooltipForSetting(graphics, mouseX, mouseY, stagingBoolean);
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
        boolean locked = stagingBoolean.isLocked();
        float innerHeight = Math.max(0f, h() - 2f * SettingsUiMetrics.SETTING_ROW_PADDING_Y);
        float padY = SettingsUiMetrics.SETTING_ROW_PADDING_Y;
        float padX = SettingsUiMetrics.SETTING_ROW_PADDING_X;
        float[] toggle = toggleBounds(padX, innerHeight, padY);
        float toggleW = toggle[2];
        float toggleH = toggle[3];
        float titleRowHeight = Math.max(ModSettingsTheme.shellDesignBodyLineHeight(), toggleH);
        float titleOriginY = titleRowTop(innerHeight, padY, titleRowHeight);
        float[] apply = applyChipBounds(padX, innerHeight, padY, toggleH);
        float labelGap = SettingsUiMetrics.SETTING_VALUE_CONTROL_GAP;
        float labelX = toggle[0] + toggleW + labelGap;
        float labelMaxWidth = Math.max(0f, apply[0] - APPLY_LABEL_GAP_DESIGN - labelX);
        String label = stagingBoolean.getTranslatedDisplayName();
        label = TextLineLayout.ellipsize(label, labelMaxWidth, segment -> graphics.measureTextWidth(segment, false));
        float labelY = titleOriginY + Math.max(0f, (toggleH - graphics.getFontHeight()) * 0.5f);
        int labelColor = locked ? graphics.theme().textMuted() : graphics.theme().textPrimary();
        graphics.drawMiniMessageText("<color:" + ColorUtils.rgbHex(labelColor) + ">" + label + "</color>", labelX, labelY, false);
        float progress = Mth.clamp(toggleProgressAnim.value(), 0f, 1f);
        int trackFillOff = hoveredToggle && !locked ? graphics.theme().toggleOffFillHover() : graphics.theme().toggleOffFill();
        int trackFillOn = hoveredToggle && !locked ? graphics.theme().toggleOnFillHover() : graphics.theme().toggleOnFill();
        int trackFill = ColorUtils.mixArgb(progress, trackFillOff, trackFillOn);
        int trackBorder = ColorUtils.mixArgb(progress, hoveredToggle && !locked ? graphics.theme().toggleOffBorderHover() : graphics.theme().toggleOffBorder(), graphics.theme().toggleOnBorder());
        if (locked) {
            trackFill = WSettingTooltip.dimColor(trackFill, 0.45f);
            trackBorder = WSettingTooltip.dimColor(trackBorder, 0.6f);
        }
        float borderThickness = 1f;
        float filletRadius = TOGGLE_CORNER_FILLET_DESIGN;
        float maxTrackCornerRadius = Math.max(0.5f, Math.min(toggleH * 0.5f - borderThickness * 0.5f, toggleW * 0.5f - borderThickness * 0.5f));
        float trackCornerRadius = Mth.clamp(filletRadius, 0.5f, maxTrackCornerRadius);
        graphics.fillRoundedRectFrame(toggle[0], titleOriginY, toggleW, toggleH, trackCornerRadius, trackBorder, trackFill, borderThickness, borderThickness, RectCornerRoundMask.ALL);
        float knobSize = toggleH - 5f;
        float knobTravelLeft = toggle[0] + 2f;
        float knobTravelRight = toggle[0] + toggleW - knobSize - 2f;
        float knobX = Mth.lerp(progress, knobTravelLeft, knobTravelRight);
        float knobY = titleOriginY + (toggleH - knobSize) * 0.5f;
        float maxKnobCornerRadius = Math.max(0.5f, knobSize * 0.5f - 0.01f);
        float knobCornerRadius = Mth.clamp(filletRadius, 0.5f, maxKnobCornerRadius);
        int thumbColor = locked ? WSettingTooltip.dimColor(graphics.theme().thumb(), 0.5f) : graphics.theme().thumb();
        graphics.fillRoundedRect(knobX, knobY, knobSize, knobSize, knobCornerRadius, thumbColor, RectCornerRoundMask.ALL);

        int fillColor = FascinatedGuiTheme.INSTANCE.surface();
        int outlineColor = hoveredApply ? FascinatedGuiTheme.INSTANCE.borderHover() : FascinatedGuiTheme.INSTANCE.border();
        float chipBorderX = UITheme.BORDER_THICKNESS_PX;
        float chipBorderY = UITheme.BORDER_THICKNESS_PX;
        float chipCorner = resolveApplyChipCornerRadius(graphics, apply[2], apply[3]);
        graphics.fillRoundedRectFrame(apply[0], apply[1], apply[2], apply[3], chipCorner, outlineColor, fillColor, chipBorderX, chipBorderY, RectCornerRoundMask.ALL);
        String applyLabel = I18n.get("fascinatedutils.setting.shell.global_hud_apply_all");
        float wrapBudget = Math.max(1f, apply[2] - 2f * APPLY_HORIZONTAL_PAD_DESIGN);
        applyLabel = TextLineLayout.ellipsize(applyLabel, wrapBudget, segment -> graphics.measureTextWidth(segment, false));
        float textX = apply[0] + (apply[2] - graphics.measureTextWidth(applyLabel, false)) * 0.5f;
        float textY = apply[1] + (apply[3] - graphics.getFontHeight()) * 0.5f;
        graphics.drawMiniMessageText("<color:" + ColorUtils.rgbHex(graphics.theme().textPrimary()) + ">" + applyLabel + "</color>", textX, textY, false);
    }

    private float resolveApplyChipCornerRadius(GuiRenderer graphics, float chipWidth, float chipHeight) {
        float maxRadius = Math.min(chipWidth, chipHeight) * 0.5f - 0.01f;
        float themed = graphics.theme().cardCornerRadius();
        return Math.max(0.5f, Math.min(themed, maxRadius));
    }

    private boolean rectContainsToggle(float pointerX, float pointerY) {
        float innerHeight = Math.max(0f, h() - 2f * SettingsUiMetrics.SETTING_ROW_PADDING_Y);
        float padY = SettingsUiMetrics.SETTING_ROW_PADDING_Y;
        float padX = SettingsUiMetrics.SETTING_ROW_PADDING_X;
        float[] toggle = toggleBounds(padX, innerHeight, padY);
        return rectContains(toggle[0], toggle[1], toggle[2], toggle[3], pointerX, pointerY);
    }

    private boolean rectContainsApply(float pointerX, float pointerY) {
        float innerHeight = Math.max(0f, h() - 2f * SettingsUiMetrics.SETTING_ROW_PADDING_Y);
        float padY = SettingsUiMetrics.SETTING_ROW_PADDING_Y;
        float padX = SettingsUiMetrics.SETTING_ROW_PADDING_X;
        float toggleH = SettingsUiMetrics.BOOLEAN_TOGGLE_OUTER_H;
        float[] apply = applyChipBounds(padX, innerHeight, padY, toggleH);
        return rectContains(apply[0], apply[1], apply[2], apply[3], pointerX, pointerY);
    }

    private float[] toggleBounds(float padX, float innerHeight, float padY) {
        float toggleW = SettingsUiMetrics.BOOLEAN_TOGGLE_OUTER_W;
        float toggleH = SettingsUiMetrics.BOOLEAN_TOGGLE_OUTER_H;
        float titleRowHeight = Math.max(ModSettingsTheme.shellDesignBodyLineHeight(), toggleH);
        float titleOriginY = titleRowTop(innerHeight, padY, titleRowHeight);
        float toggleLeft = x() + padX;
        return new float[]{toggleLeft, titleOriginY, toggleW, toggleH};
    }

    /**
     * @return left, top, width, height of the apply chip in layout space
     */
    private float[] applyChipBounds(float padX, float innerHeight, float padY, float toggleHeight) {
        float chipWidth = APPLY_CHIP_WIDTH_DESIGN;
        float chipHeight = Math.max(toggleHeight, SettingsUiMetrics.SHELL_CONTROL_HEIGHT_DESIGN);
        chipHeight = Math.min(chipHeight, innerHeight);
        float bodyRight = x() + w() - padX;
        float chipLeft = bodyRight - chipWidth;
        float bodyTop = y() + padY;
        float chipTop = bodyTop + Math.max(0f, (innerHeight - chipHeight) * 0.5f);
        return new float[]{chipLeft, chipTop, chipWidth, chipHeight};
    }

    private float titleRowTop(float innerHeight, float padY, float titleRowHeight) {
        float bodyTop = y() + padY;
        return bodyTop + (innerHeight - titleRowHeight) * 0.5f;
    }
}
