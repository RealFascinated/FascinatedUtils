package cc.fascinated.fascinatedutils.gui.widgets.settings;

import cc.fascinated.fascinatedutils.common.Colors;
import cc.fascinated.fascinatedutils.common.color.RainbowColors;
import cc.fascinated.fascinatedutils.common.color.SettingColor;
import cc.fascinated.fascinatedutils.common.setting.impl.ColorSetting;
import cc.fascinated.fascinatedutils.gui.core.TextLineLayout;
import cc.fascinated.fascinatedutils.gui.core.UiPointerCursor;
import cc.fascinated.fascinatedutils.gui.renderer.GuiRenderer;
import cc.fascinated.fascinatedutils.gui.renderer.RectCornerRoundMask;
import cc.fascinated.fascinatedutils.gui.theme.ModSettingsTheme;
import cc.fascinated.fascinatedutils.gui.theme.SettingsUiMetrics;
import cc.fascinated.fascinatedutils.gui.theme.UITheme;

import java.util.function.Consumer;

public class FColorSettingRowWidget extends FSettingRowWidget {
    private static final float SWATCH_SIZE_DESIGN = 10f;
    private static final float SWATCH_CORNER_RADIUS_DESIGN = UITheme.CORNER_RADIUS_XS;
    private final ColorSetting colorSetting;
    private final Consumer<ColorSetting> openColorPicker;
    private boolean hoveredSwatch;
    private boolean hoveredReset;

    public FColorSettingRowWidget(ColorSetting colorSetting, float outerWidth, float outerHeight, Runnable onPersist, float valueColumnStartX, Consumer<ColorSetting> openColorPicker) {
        super(outerWidth, outerHeight, onPersist, valueColumnStartX);
        this.colorSetting = colorSetting;
        this.openColorPicker = openColorPicker;
    }

    @Override
    public boolean mouseLeave(float pointerX, float pointerY) {
        super.mouseLeave(pointerX, pointerY);
        hoveredSwatch = false;
        hoveredReset = false;
        return false;
    }

    @Override
    public boolean mouseMove(float pointerX, float pointerY) {
        float[] swatch = swatchBounds();
        hoveredSwatch = rectContains(swatch, pointerX, pointerY);
        float[] resetSquare = inlineResetSquare();
        hoveredReset = SettingRowResetLayout.resetGlyphHitActive(resetSquare, pointerX, pointerY, colorSetting.isAtDefault());
        return false;
    }

    @Override
    public UiPointerCursor pointerCursor(float pointerX, float pointerY) {
        if (colorSetting.isLocked()) {
            return UiPointerCursor.DEFAULT;
        }
        if (rectContains(swatchBounds(), pointerX, pointerY)) {
            return UiPointerCursor.HAND;
        }
        if (SettingRowResetLayout.resetGlyphHitActive(inlineResetSquare(), pointerX, pointerY, colorSetting.isAtDefault())) {
            return UiPointerCursor.HAND;
        }
        return UiPointerCursor.DEFAULT;
    }

    @Override
    public boolean mouseDown(float pointerX, float pointerY, int button) {
        if (button != 0) {
            return false;
        }
        if (colorSetting.isLocked()) {
            return hoveredSwatch || hoveredReset;
        }
        if (hoveredReset) {
            return true;
        }
        return hoveredSwatch;
    }

    @Override
    public boolean click(float pointerX, float pointerY, int button) {
        if (button != 0) {
            return false;
        }
        if (colorSetting.isLocked()) {
            return hoveredSwatch || hoveredReset;
        }
        if (hoveredReset) {
            colorSetting.resetToDefault();
            onPersist.run();
            return true;
        }
        if (hoveredSwatch) {
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

        float titleRowHeight = Math.max(ModSettingsTheme.shellDesignBodyLineHeight(), SWATCH_SIZE_DESIGN);
        float titleOriginY = y() + padY + (innerHeight - titleRowHeight) * 0.5f;

        String label = colorSetting.getName();
        float[] swatch = swatchBounds();
        float labelMaxWidth = Math.max(0f, swatch[0] - SettingsUiMetrics.SETTING_VALUE_CONTROL_GAP - bodyLeft);
        label = TextLineLayout.ellipsize(label, labelMaxWidth, segment -> graphics.measureTextWidth(segment, false));
        float labelY = titleOriginY + Math.max(0f, (SWATCH_SIZE_DESIGN - graphics.getFontCapHeight()) * 0.5f);
        int labelColor = locked ? graphics.theme().textMuted() : graphics.theme().textPrimary();
        graphics.drawMiniMessageText("<color:" + Colors.rgbHex(labelColor) + ">" + label + "</color>", bodyLeft, labelY, false);

        SettingColor color = colorSetting.getValue();
        int swatchArgb = color.isRainbow() ? RainbowColors.currentColor().getPackedArgb() | 0xFF000000 : color.getPackedArgb();

        float borderPx = 1f;
        int swatchBorder = hoveredSwatch && !locked ? graphics.theme().accentBright() : graphics.theme().border();
        graphics.fillRoundedRectFrame(swatch[0], swatch[1], swatch[2], swatch[3], SWATCH_CORNER_RADIUS_DESIGN, swatchBorder, swatchArgb, borderPx, borderPx, RectCornerRoundMask.ALL);

        if (color.isRainbow()) {
            float rainbowLabelX = swatch[0] + swatch[2] + 4f;
            int rainbowTextColor = locked ? graphics.theme().textMuted() : graphics.theme().textAccent();
            graphics.drawMiniMessageText("<color:" + Colors.rgbHex(rainbowTextColor) + ">Rainbow</color>", rainbowLabelX, labelY, false);
        }

        float[] resetSquare = inlineResetSquare();
        SettingRowResetLayout.paintGlyph(graphics, resetSquare[0], resetSquare[1], SWATCH_SIZE_DESIGN, hoveredReset && !locked, colorSetting.isAtDefault());
    }

    private void openColorPicker() {
        openColorPicker.accept(colorSetting);
    }

    private float[] swatchBounds() {
        float padY = SettingsUiMetrics.SETTING_ROW_PADDING_Y;
        float padX = SettingsUiMetrics.SETTING_ROW_PADDING_X;
        float innerHeight = Math.max(0f, h() - 2f * padY);
        float titleRowHeight = Math.max(ModSettingsTheme.shellDesignBodyLineHeight(), SWATCH_SIZE_DESIGN);
        float titleOriginY = y() + padY + (innerHeight - titleRowHeight) * 0.5f;
        float bodyLeft = x() + padX;
        float resetLeft = SettingRowResetLayout.trailingResetLeftX(x() + outerWidth);
        float maxSwatchLeft = Math.max(bodyLeft, resetLeft - SettingsUiMetrics.SETTING_VALUE_CONTROL_GAP - SWATCH_SIZE_DESIGN);
        float swatchLeft = Math.min(bodyLeft + valueColumnStartX, maxSwatchLeft);
        return new float[]{swatchLeft, titleOriginY, SWATCH_SIZE_DESIGN, SWATCH_SIZE_DESIGN};
    }

    private float[] inlineResetSquare() {
        float[] swatch = swatchBounds();
        float resetLeft = SettingRowResetLayout.trailingResetLeftX(x() + outerWidth);
        float resetTop = SettingRowResetLayout.verticallyCenteredTop(swatch[1], swatch[3]);
        float box = SettingRowResetLayout.glyphBoxPx();
        return new float[]{resetLeft, resetTop, box};
    }
}
