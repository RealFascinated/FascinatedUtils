package cc.fascinated.fascinatedutils.gui.widgets.settings;

import cc.fascinated.fascinatedutils.common.ColorUtils;
import cc.fascinated.fascinatedutils.common.setting.impl.SliderSetting;
import cc.fascinated.fascinatedutils.gui.core.TextLineLayout;
import cc.fascinated.fascinatedutils.gui.renderer.GuiRenderer;
import cc.fascinated.fascinatedutils.gui.renderer.RectCornerRoundMask;
import cc.fascinated.fascinatedutils.gui.renderer.UIRenderer;
import cc.fascinated.fascinatedutils.gui.theme.ModSettingsTheme;
import cc.fascinated.fascinatedutils.gui.theme.SettingsUiMetrics;
import cc.fascinated.fascinatedutils.gui.widgets.FWidget;
import cc.fascinated.fascinatedutils.systems.config.ModConfig;
import cc.fascinated.fascinatedutils.systems.modules.Module;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;

public class FSliderSettingRowWidget extends FWidget {
    private final SliderSetting sliderSetting;
    private final float outerWidth;
    private final float outerHeight;
    private final Runnable onPersist;
    private final float valueColumnStartX;
    private boolean dragging;
    private boolean hoveredTrack;
    private boolean hoveredReset;

    public FSliderSettingRowWidget(Module module, SliderSetting sliderSetting, float outerWidth, float outerHeight, float valueColumnStartX) {
        this(sliderSetting, outerWidth, outerHeight, () -> ModConfig.saveActiveModule(module), valueColumnStartX);
    }

    public FSliderSettingRowWidget(SliderSetting sliderSetting, float outerWidth, float outerHeight, Runnable onPersist, float valueColumnStartX) {
        this.sliderSetting = sliderSetting;
        this.outerWidth = outerWidth;
        this.outerHeight = outerHeight;
        this.onPersist = onPersist;
        this.valueColumnStartX = Math.max(0f, valueColumnStartX);
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
        hoveredReset = false;
        return false;
    }

    @Override
    public boolean mouseMove(float pointerX, float pointerY) {
        float trackLeft = sliderTrackLeft();
        float trackWidth = sliderTrackWidth();
        float trackLayoutTop = computeTrackTop();
        float trackLayoutHeight = computeTrackHeight();
        hoveredTrack = inTrack(pointerX, pointerY, trackLeft, trackLayoutTop, trackWidth, trackLayoutHeight);
        if (dragging) {
            applyPointer(pointerX, trackLeft, trackWidth);
            return true;
        }
        float[] resetSquare = inlineResetSquare();
        hoveredReset = SettingRowResetLayout.resetGlyphHitActive(resetSquare[0], resetSquare[1], resetSquare[2], pointerX, pointerY, sliderSetting.isAtDefault());
        return false;
    }

    @Override
    public boolean mouseDown(float pointerX, float pointerY, int button) {
        if (button != 0) {
            return false;
        }
        if (sliderSetting.isLocked()) {
            return hoveredTrack || hoveredReset;
        }
        float[] resetSquare = inlineResetSquare();
        if (SettingRowResetLayout.resetGlyphHitActive(resetSquare[0], resetSquare[1], resetSquare[2], pointerX, pointerY, sliderSetting.isAtDefault())) {
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
        if (sliderSetting.isLocked()) {
            return hoveredTrack || hoveredReset;
        }
        float[] resetSquare = inlineResetSquare();
        if (SettingRowResetLayout.resetGlyphHitActive(resetSquare[0], resetSquare[1], resetSquare[2], pointerX, pointerY, sliderSetting.isAtDefault())) {
            sliderSetting.resetToDefault();
            onPersist.run();
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
        if (hoveredTrack || hoveredReset) {
            WSettingTooltip.drawTooltipForSetting(graphics, mouseX, mouseY, sliderSetting, hoveredReset);
        }
    }

    @Override
    protected void renderSelf(GuiRenderer graphics, float mouseX, float mouseY, float deltaSeconds) {
        boolean locked = sliderSetting.isLocked();
        float bodyLeft = x() + bodyPadX();
        float textLineHeight = Math.max(1f, ModSettingsTheme.shellDesignBodyLineHeight());
        float textTop = innerMidY() - textLineHeight * 0.5f;
        float labelMaxWidth = Math.max(0f, sliderValueTextX(bodyLeft) - SettingsUiMetrics.SETTING_VALUE_CONTROL_GAP - bodyLeft);
        String label = TextLineLayout.ellipsize(sliderSetting.getTranslatedDisplayName(), labelMaxWidth, segment -> graphics.measureTextWidth(segment, false));
        int labelColor = locked ? graphics.theme().textMuted() : graphics.theme().textPrimary();
        graphics.drawMiniMessageText("<color:" + ColorUtils.rgbHex(labelColor) + ">" + label + "</color>", bodyLeft, textTop, false);
        float value = sliderSetting.getValue().floatValue();
        float min = sliderSetting.getMin();
        float max = sliderSetting.getMax();
        String valueText = sliderSetting.formatValueForDisplay();
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
        float[] resetSquare = inlineResetSquare();
        SettingRowResetLayout.paintGlyph(graphics, resetSquare[0], resetSquare[1], Math.max(1f, ModSettingsTheme.shellDesignBodyLineHeight()), hoveredReset && !locked, sliderSetting.isAtDefault());
    }

    private void applyPointer(float pointerX, float trackLeft, float trackWidth) {
        float min = sliderSetting.getMin();
        float max = sliderSetting.getMax();
        float step = sliderSetting.getStep();
        float next = valueFromPointer(pointerX, trackLeft, trackWidth, thumbDiameter(), min, max, step);
        if (Math.abs(next - sliderSetting.getValue().floatValue()) > 1e-6f) {
            sliderSetting.setValue(next);
            onPersist.run();
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
        String valueText = sliderSetting.formatValueForDisplay();
        float measuredValueWidth = minecraftClient.font.width(valueText);
        float valuePadding = 3f;
        return Math.max(minColumnWidth, measuredValueWidth + valuePadding);
    }

    private float sliderTrackLeft() {
        float bodyLeft = x() + bodyPadX();
        float valueColumnWidth = sliderValueColumnWidth();
        float gapAfterValue = SettingsUiMetrics.SETTING_VALUE_CONTROL_GAP;
        return sliderValueTextX(bodyLeft) + valueColumnWidth + gapAfterValue;
    }

    private float sliderTrackWidth() {
        float right = x() + w() - SettingRowResetLayout.trailingResetReservePx();
        return Math.max(25f, right - sliderTrackLeft());
    }

    private float[] inlineResetSquare() {
        float contentRight = x() + w();
        float resetLeft = SettingRowResetLayout.trailingResetLeftX(contentRight);
        float box = SettingRowResetLayout.glyphBoxPx();
        float sliderMidY = computeTrackTop() + computeTrackHeight() * 0.5f;
        float resetTop = sliderMidY - box * 0.5f;
        return new float[]{resetLeft, resetTop, box};
    }
}
