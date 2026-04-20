package cc.fascinated.fascinatedutils.gui.widgets.settings;

import cc.fascinated.fascinatedutils.common.ColorUtils;
import cc.fascinated.fascinatedutils.common.color.RainbowColors;
import cc.fascinated.fascinatedutils.common.color.SettingColor;
import cc.fascinated.fascinatedutils.common.setting.impl.ColorSetting;
import cc.fascinated.fascinatedutils.gui.core.TextLineLayout;
import cc.fascinated.fascinatedutils.gui.renderer.GuiRenderer;
import cc.fascinated.fascinatedutils.gui.renderer.RectCornerRoundMask;
import cc.fascinated.fascinatedutils.gui.renderer.UIRenderer;
import cc.fascinated.fascinatedutils.gui.theme.ModSettingsTheme;
import cc.fascinated.fascinatedutils.gui.theme.SettingsUiMetrics;
import cc.fascinated.fascinatedutils.gui.widgets.FWidget;

import java.util.function.Consumer;

public class FColorSettingRowWidget extends FWidget {
    private static final float SWATCH_SIZE_DESIGN = 10f;
    private static final float SWATCH_CORNER_RADIUS_DESIGN = 2f;
    private final ColorSetting colorSetting;
    private final float outerWidth;
    private final float outerHeight;
    private final Runnable onPersist;
    private final float valueColumnStartX;
    private final Consumer<ColorSetting> openColorPicker;
    private boolean hoveredSwatch;
    private boolean hoveredReset;

    public FColorSettingRowWidget(ColorSetting colorSetting, float outerWidth, float outerHeight, Runnable onPersist, float valueColumnStartX, Consumer<ColorSetting> openColorPicker) {
        this.colorSetting = colorSetting;
        this.outerWidth = outerWidth;
        this.outerHeight = outerHeight;
        this.onPersist = onPersist;
        this.valueColumnStartX = Math.max(0f, valueColumnStartX);
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
        hoveredReset = false;
        return false;
    }

    @Override
    public boolean mouseMove(float pointerX, float pointerY) {
        float[] swatch = swatchBounds();
        hoveredSwatch = rectContains(swatch, pointerX, pointerY);
        float[] resetSquare = inlineResetSquare();
        hoveredReset = SettingRowResetLayout.resetGlyphHitActive(resetSquare[0], resetSquare[1], resetSquare[2], pointerX, pointerY, colorSetting.isAtDefault());
        return false;
    }

    @Override
    public boolean mouseDown(float pointerX, float pointerY, int button) {
        if (button != 0) {
            return false;
        }
        if (colorSetting.isLocked()) {
            return hoveredSwatch || hoveredReset;
        }
        float[] resetSquare = inlineResetSquare();
        if (SettingRowResetLayout.resetGlyphHitActive(resetSquare[0], resetSquare[1], resetSquare[2], pointerX, pointerY, colorSetting.isAtDefault())) {
            return true;
        }
        return rectContains(swatchBounds(), pointerX, pointerY);
    }

    @Override
    public boolean click(float pointerX, float pointerY, int button) {
        if (button != 0) {
            return false;
        }
        if (colorSetting.isLocked()) {
            return hoveredSwatch || hoveredReset;
        }
        float[] resetSquare = inlineResetSquare();
        if (SettingRowResetLayout.resetGlyphHitActive(resetSquare[0], resetSquare[1], resetSquare[2], pointerX, pointerY, colorSetting.isAtDefault())) {
            colorSetting.resetToDefault();
            onPersist.run();
            return true;
        }
        if (rectContains(swatchBounds(), pointerX, pointerY)) {
            openColorPicker();
            return true;
        }
        return false;
    }

    @Override
    public void renderOverlayAfterChildren(GuiRenderer graphics, float mouseX, float mouseY, float deltaSeconds) {
        if (hoveredSwatch || hoveredReset) {
            WSettingTooltip.drawTooltipForSetting(graphics, mouseX, mouseY, colorSetting, hoveredReset);
        }
    }

    @Override
    protected void renderSelf(GuiRenderer graphics, float mouseX, float mouseY, float deltaSeconds) {
        boolean locked = colorSetting.isLocked();
        float padY = SettingsUiMetrics.SETTING_ROW_PADDING_Y;
        float padX = SettingsUiMetrics.SETTING_ROW_PADDING_X;
        float bodyLeft = x() + padX;
        float innerHeight = Math.max(0f, h() - 2f * padY);

        float swatchSize = SWATCH_SIZE_DESIGN;
        float titleRowHeight = Math.max(ModSettingsTheme.shellDesignBodyLineHeight(), swatchSize);
        float titleOriginY = y() + padY + (innerHeight - titleRowHeight) * 0.5f;

        String label = colorSetting.getTranslatedDisplayName();
        float[] swatch = swatchBounds();
        float labelMaxWidth = Math.max(0f, swatch[0] - SettingsUiMetrics.SETTING_VALUE_CONTROL_GAP - bodyLeft);
        label = TextLineLayout.ellipsize(label, labelMaxWidth, segment -> graphics.measureTextWidth(segment, false));
        float labelY = titleOriginY + Math.max(0f, (swatchSize - graphics.getFontHeight()) * 0.5f);
        int labelColor = locked ? graphics.theme().textMuted() : graphics.theme().textPrimary();
        graphics.drawMiniMessageText("<color:" + ColorUtils.rgbHex(labelColor) + ">" + label + "</color>", bodyLeft, labelY, false);

        SettingColor color = colorSetting.getValue();
        int swatchArgb = color.isRainbow() ? RainbowColors.currentColor().getPackedArgb() | 0xFF000000 : color.getPackedArgb();

        float cornerRadius = SWATCH_CORNER_RADIUS_DESIGN;
        float borderPx = 1f;
        int swatchBorder = hoveredSwatch && !locked ? graphics.theme().accentBright() : graphics.theme().border();
        graphics.fillRoundedRectFrame(swatch[0], swatch[1], swatch[2], swatch[3], cornerRadius, swatchBorder, swatchArgb, borderPx, borderPx, RectCornerRoundMask.ALL);

        if (color.isRainbow()) {
            float rainbowLabelX = swatch[0] + swatch[2] + 4f;
            int rainbowTextColor = locked ? graphics.theme().textMuted() : graphics.theme().textAccent();
            graphics.drawMiniMessageText("<color:" + ColorUtils.rgbHex(rainbowTextColor) + ">Rainbow</color>", rainbowLabelX, labelY, false);
        }

        float[] resetSquare = inlineResetSquare();
        SettingRowResetLayout.paintGlyph(graphics, resetSquare[0], resetSquare[1], swatchSize, hoveredReset && !locked, colorSetting.isAtDefault());
    }

    private void openColorPicker() {
        openColorPicker.accept(colorSetting);
    }

    private float[] swatchBounds() {
        float padY = SettingsUiMetrics.SETTING_ROW_PADDING_Y;
        float padX = SettingsUiMetrics.SETTING_ROW_PADDING_X;
        float innerHeight = Math.max(0f, h() - 2f * padY);
        float swatchSize = SWATCH_SIZE_DESIGN;
        float titleRowHeight = Math.max(ModSettingsTheme.shellDesignBodyLineHeight(), swatchSize);
        float titleOriginY = y() + padY + (innerHeight - titleRowHeight) * 0.5f;
        float bodyLeft = x() + padX;
        float resetLeft = SettingRowResetLayout.trailingResetLeftX(x() + outerWidth);
        float maxSwatchLeft = Math.max(bodyLeft, resetLeft - SettingsUiMetrics.SETTING_VALUE_CONTROL_GAP - swatchSize);
        float swatchLeft = Math.min(bodyLeft + valueColumnStartX, maxSwatchLeft);
        return new float[]{swatchLeft, titleOriginY, swatchSize, swatchSize};
    }

    private float[] inlineResetSquare() {
        float[] swatch = swatchBounds();
        float resetLeft = SettingRowResetLayout.trailingResetLeftX(x() + outerWidth);
        float resetTop = SettingRowResetLayout.verticallyCenteredTop(swatch[1], swatch[3]);
        float box = SettingRowResetLayout.glyphBoxPx();
        return new float[]{resetLeft, resetTop, box};
    }
}
