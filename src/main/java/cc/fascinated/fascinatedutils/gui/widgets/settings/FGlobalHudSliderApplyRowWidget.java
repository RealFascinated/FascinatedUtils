package cc.fascinated.fascinatedutils.gui.widgets.settings;

import cc.fascinated.fascinatedutils.common.ColorUtils;
import cc.fascinated.fascinatedutils.common.setting.impl.SliderSetting;
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
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.util.Mth;

/**
 * Widgets-tab row: slider bound to a staging {@link SliderSetting}, plus a trailing chip that applies the staged value
 * to every HUD module.
 */
public class FGlobalHudSliderApplyRowWidget extends FWidget {

    private static final float APPLY_CHIP_WIDTH_DESIGN = 112f;
    private static final float APPLY_HORIZONTAL_PAD_DESIGN = 10f;
    private static final float APPLY_LABEL_GAP_DESIGN = 8f;
    private final SliderSetting stagingSlider;
    private final float outerWidth;
    private final float outerHeight;
    private final float valueColumnStartX;
    private final Runnable onApplyAll;
    private boolean dragging;
    private boolean hoveredTrack;
    private boolean hoveredApply;

    public FGlobalHudSliderApplyRowWidget(SliderSetting stagingSlider, float outerWidth, float outerHeight, float valueColumnStartX, Runnable onApplyAll) {
        this.stagingSlider = stagingSlider;
        this.outerWidth = outerWidth;
        this.outerHeight = outerHeight;
        this.valueColumnStartX = Math.max(0f, valueColumnStartX);
        this.onApplyAll = onApplyAll;
    }

    private static float valueFromPointer(float pointerX, float trackLeft, float trackWidth, float thumbSize, float min, float max, float step) {
        float thumbTravelStartX = trackLeft + thumbSize * 0.5f;
        float thumbTravelWidth = Math.max(1e-6f, trackWidth - thumbSize);
        float ratio = Mth.clamp((pointerX - thumbTravelStartX) / thumbTravelWidth, 0f, 1f);
        float raw = min + ratio * (max - min);
        return SliderSetting.snapValue(raw, min, max, step);
    }

    private static boolean inTrack(float pointerX, float pointerY, float trackLeft, float trackTop, float trackWidth, float trackHeight) {
        return pointerX >= trackLeft && pointerY >= trackTop && pointerX < trackLeft + trackWidth && pointerY < trackTop + trackHeight;
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
        hoveredTrack = false;
        hoveredApply = false;
        return false;
    }

    @Override
    public boolean mouseMove(float pointerX, float pointerY) {
        hoveredApply = rectContainsApply(pointerX, pointerY);
        float trackLeft = sliderTrackLeft();
        float trackWidth = sliderTrackWidth();
        float trackLayoutTop = computeTrackTop();
        float trackLayoutHeight = computeTrackHeight();
        hoveredTrack = inTrack(pointerX, pointerY, trackLeft, trackLayoutTop, trackWidth, trackLayoutHeight);
        if (dragging) {
            applyPointer(pointerX, trackLeft, trackWidth);
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseDown(float pointerX, float pointerY, int button) {
        if (button != 0) {
            return false;
        }
        if (stagingSlider.isLocked()) {
            return hoveredTrack || hoveredApply;
        }
        if (rectContainsApply(pointerX, pointerY)) {
            return true;
        }
        float trackLeft = sliderTrackLeft();
        float trackWidth = sliderTrackWidth();
        float trackLayoutTop = computeTrackTop();
        float trackLayoutHeight = computeTrackHeight();
        if (inTrack(pointerX, pointerY, trackLeft, trackLayoutTop, trackWidth, trackLayoutHeight)) {
            dragging = true;
            applyPointer(pointerX, trackLeft, trackWidth);
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseUp(float pointerX, float pointerY, int button) {
        if (dragging) {
            dragging = false;
            return true;
        }
        return false;
    }

    @Override
    public boolean click(float pointerX, float pointerY, int button) {
        if (button != 0) {
            return false;
        }
        if (stagingSlider.isLocked()) {
            return hoveredTrack || hoveredApply;
        }
        if (rectContainsApply(pointerX, pointerY)) {
            onApplyAll.run();
            return true;
        }
        float trackLeft = sliderTrackLeft();
        float trackWidth = sliderTrackWidth();
        float trackLayoutTop = computeTrackTop();
        float trackLayoutHeight = computeTrackHeight();
        return inTrack(pointerX, pointerY, trackLeft, trackLayoutTop, trackWidth, trackLayoutHeight);
    }

    @Override
    public void renderOverlayAfterChildren(GuiRenderer graphics, float mouseX, float mouseY, float deltaSeconds) {
        if (hoveredTrack) {
            WSettingTooltip.drawTooltipForSetting(graphics, mouseX, mouseY, stagingSlider);
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
        boolean locked = stagingSlider.isLocked();
        float bodyLeft = x() + bodyPadX();
        float textLineHeight = Math.max(1f, ModSettingsTheme.shellDesignBodyLineHeight());
        float textTop = innerMidY() - textLineHeight * 0.5f;
        float[] apply = applyChipBounds();
        float labelMaxWidth = Math.max(0f, sliderValueTextX(bodyLeft) - SettingsUiMetrics.SETTING_VALUE_CONTROL_GAP - bodyLeft);
        String label = TextLineLayout.ellipsize(stagingSlider.getTranslatedDisplayName(), labelMaxWidth, segment -> graphics.measureTextWidth(segment, false));
        int labelColor = locked ? graphics.theme().textMuted() : graphics.theme().textPrimary();
        graphics.drawMiniMessageText("<color:" + ColorUtils.rgbHex(labelColor) + ">" + label + "</color>", bodyLeft, textTop, false);
        float value = stagingSlider.getValue().floatValue();
        float min = stagingSlider.getMin();
        float max = stagingSlider.getMax();
        String valueText = stagingSlider.formatValueForDisplay();
        float valueDrawX = sliderValueTextX(bodyLeft);
        int valueColor = locked ? graphics.theme().textMuted() : graphics.theme().textAccent();
        graphics.drawMiniMessageText("<color:" + ColorUtils.rgbHex(valueColor) + ">" + valueText + "</color>", valueDrawX, textTop, false);
        float trackLayoutTop = computeTrackTop();
        float trackLayoutHeight = computeTrackHeight();
        float trackLeft = sliderTrackLeft();
        float trackWidth = sliderTrackWidth();
        float trackMidY = trackLayoutTop + trackLayoutHeight * 0.5f;
        float trackThickness = trackVisualThickness();
        int trackColor = locked ? WSettingTooltip.dimColor(graphics.theme().border(), 0.55f) : graphics.theme().border();
        graphics.drawRect(trackLeft, trackMidY - trackThickness * 0.5f, trackWidth, trackThickness, trackColor);
        float ratio = (value - min) / Math.max(1e-6f, max - min);
        float thumbSize = thumbDiameter();
        float thumbCenterX = trackLeft + thumbSize * 0.5f + ratio * Math.max(0f, trackWidth - thumbSize);
        int thumbColor = locked ? WSettingTooltip.dimColor(graphics.theme().accentBright(), 0.5f) : graphics.theme().accentBright();
        graphics.fillRoundedRect(thumbCenterX - thumbSize * 0.5f, trackMidY - thumbSize * 0.5f, thumbSize, thumbSize, thumbSize * 0.5f, thumbColor, RectCornerRoundMask.ALL);

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

    private void applyPointer(float pointerX, float trackLeft, float trackWidth) {
        float min = stagingSlider.getMin();
        float max = stagingSlider.getMax();
        float step = stagingSlider.getStep();
        float next = valueFromPointer(pointerX, trackLeft, trackWidth, thumbDiameter(), min, max, step);
        if (Math.abs(next - stagingSlider.getValue().floatValue()) > 1e-6f) {
            stagingSlider.setValue(next);
        }
    }

    private float bodyPadX() {
        return SettingsUiMetrics.SETTING_ROW_PADDING_X;
    }

    private float bodyPadY() {
        return SettingsUiMetrics.SETTING_ROW_PADDING_Y;
    }

    private float innerMidY() {
        return y() + bodyPadY() + (h() - 2f * bodyPadY()) * 0.5f;
    }

    private float computeTrackTop() {
        return innerMidY() - computeTrackHeight() * 0.5f;
    }

    private float computeTrackHeight() {
        return 13f;
    }

    private float trackVisualThickness() {
        return 3f;
    }

    private float thumbDiameter() {
        return 7f;
    }

    private float sliderValueTextX(float bodyLeft) {
        return bodyLeft + valueColumnStartX;
    }

    private float sliderValueColumnWidth() {
        float minColumnWidth = SettingsUiMetrics.SLIDER_VALUE_COL_W;
        Minecraft minecraftClient = Minecraft.getInstance();
        if (minecraftClient == null) {
            return minColumnWidth;
        }
        String valueText = stagingSlider.formatValueForDisplay();
        float measuredValueWidth = minecraftClient.font.width(valueText);
        float valuePadding = 3f;
        return Math.max(minColumnWidth, measuredValueWidth + valuePadding);
    }

    private float applyChipLeftX() {
        float padX = bodyPadX();
        float chipWidth = APPLY_CHIP_WIDTH_DESIGN;
        return x() + w() - padX - chipWidth;
    }

    private float[] applyChipBounds() {
        float padY = bodyPadY();
        float innerHeight = Math.max(0f, h() - 2f * padY);
        float chipWidth = APPLY_CHIP_WIDTH_DESIGN;
        float chipHeight = Math.max(computeTrackHeight(), SettingsUiMetrics.SHELL_CONTROL_HEIGHT_DESIGN);
        chipHeight = Math.min(chipHeight, innerHeight);
        float chipLeft = applyChipLeftX();
        float bodyTop = y() + padY;
        float chipTop = bodyTop + Math.max(0f, (innerHeight - chipHeight) * 0.5f);
        return new float[]{chipLeft, chipTop, chipWidth, chipHeight};
    }

    private boolean rectContainsApply(float pointerX, float pointerY) {
        float[] apply = applyChipBounds();
        return rectContains(apply[0], apply[1], apply[2], apply[3], pointerX, pointerY);
    }

    private float sliderTrackLeft() {
        float bodyLeft = x() + bodyPadX();
        float valueColumnWidth = sliderValueColumnWidth();
        float gapAfterValue = SettingsUiMetrics.SETTING_VALUE_CONTROL_GAP;
        return sliderValueTextX(bodyLeft) + valueColumnWidth + gapAfterValue;
    }

    private float sliderTrackWidth() {
        float gapBeforeApply = APPLY_LABEL_GAP_DESIGN;
        float trackRight = applyChipLeftX() - gapBeforeApply;
        return Math.max(25f, trackRight - sliderTrackLeft());
    }
}
