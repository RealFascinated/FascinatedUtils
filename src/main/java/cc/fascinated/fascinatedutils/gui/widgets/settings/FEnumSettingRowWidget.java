package cc.fascinated.fascinatedutils.gui.widgets.settings;

import cc.fascinated.fascinatedutils.common.Colors;
import cc.fascinated.fascinatedutils.common.setting.impl.EnumSetting;
import cc.fascinated.fascinatedutils.gui.core.TextLineLayout;
import cc.fascinated.fascinatedutils.gui.core.UiPointerCursor;
import cc.fascinated.fascinatedutils.gui.renderer.GuiRenderer;
import cc.fascinated.fascinatedutils.gui.renderer.RectCornerRoundMask;
import cc.fascinated.fascinatedutils.gui.theme.ModSettingsTheme;
import cc.fascinated.fascinatedutils.gui.theme.SettingsUiMetrics;
import cc.fascinated.fascinatedutils.systems.config.ModConfig;
import cc.fascinated.fascinatedutils.systems.modules.Module;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;

public class FEnumSettingRowWidget extends FSettingRowWidget {

    private final EnumSetting<?> enumSetting;
    private boolean hoveredChip;
    private boolean hoveredReset;

    public FEnumSettingRowWidget(Module module, EnumSetting<?> enumSetting, float outerWidth, float outerHeight, float valueColumnStartX) {
        this(enumSetting, outerWidth, outerHeight, () -> ModConfig.profiles().saveModule(module), valueColumnStartX);
    }

    public FEnumSettingRowWidget(EnumSetting<?> enumSetting, float outerWidth, float outerHeight, Runnable onPersist, float valueColumnStartX) {
        super(outerWidth, outerHeight, onPersist, valueColumnStartX);
        this.enumSetting = enumSetting;
    }

    public FEnumSettingRowWidget(Module module, EnumSetting<?> enumSetting, float outerWidth, float outerHeight) {
        this(module, enumSetting, outerWidth, outerHeight, 0f);
    }

    public FEnumSettingRowWidget(EnumSetting<?> enumSetting, float outerWidth, float outerHeight, Runnable onPersist) {
        this(enumSetting, outerWidth, outerHeight, onPersist, 0f);
    }

    private static float estimateTextWidthLogical(String text) {
        Minecraft client = Minecraft.getInstance();
        if (text == null) {
            return 0f;
        }
        return client.font.width(text);
    }

    @Override
    public boolean wantsPointer() {
        return true;
    }

    @Override
    public UiPointerCursor pointerCursor(float pointerX, float pointerY) {
        if (enumSetting.isLocked()) {
            return UiPointerCursor.DEFAULT;
        }
        float[] resetSquare = inlineResetSquare();
        if (SettingRowResetLayout.resetGlyphHitActive(resetSquare[0], resetSquare[1], resetSquare[2], pointerX, pointerY, enumSetting.isAtDefault())) {
            return UiPointerCursor.HAND;
        }
        float[] chip = valueChipBounds();
        if (rectContains(chip, pointerX, pointerY)) {
            return UiPointerCursor.HAND;
        }
        return UiPointerCursor.DEFAULT;
    }

    @Override
    public boolean mouseLeave(float pointerX, float pointerY) {
        hoveredChip = false;
        hoveredReset = false;
        return false;
    }

    @Override
    public boolean mouseMove(float pointerX, float pointerY) {
        float[] chip = valueChipBounds();
        hoveredChip = rectContains(chip, pointerX, pointerY);
        float[] resetSquare = inlineResetSquare();
        hoveredReset = SettingRowResetLayout.resetGlyphHitActive(resetSquare[0], resetSquare[1], resetSquare[2], pointerX, pointerY, enumSetting.isAtDefault());
        return false;
    }

    @Override
    public boolean mouseDown(float pointerX, float pointerY, int button) {
        if (button != 0) {
            return false;
        }
        float[] chip = valueChipBounds();
        boolean containsChip = rectContains(chip, pointerX, pointerY);
        if (enumSetting.isLocked()) {
            return hoveredChip || hoveredReset;
        }
        float[] resetSquare = inlineResetSquare();
        if (SettingRowResetLayout.resetGlyphHitActive(resetSquare[0], resetSquare[1], resetSquare[2], pointerX, pointerY, enumSetting.isAtDefault())) {
            return true;
        }
        return containsChip;
    }

    @Override
    public boolean click(float pointerX, float pointerY, int button) {
        if (button != 0) {
            return false;
        }
        float[] chip = valueChipBounds();
        boolean containsChip = rectContains(chip, pointerX, pointerY);
        if (enumSetting.isLocked()) {
            return hoveredChip || hoveredReset;
        }
        float[] resetSquare = inlineResetSquare();
        if (SettingRowResetLayout.resetGlyphHitActive(resetSquare[0], resetSquare[1], resetSquare[2], pointerX, pointerY, enumSetting.isAtDefault())) {
            enumSetting.resetToDefault();
            onPersist.run();
            return true;
        }
        if (containsChip) {
            enumSetting.cycleNextConstant();
            onPersist.run();
            return true;
        }
        return false;
    }

    @Override
    public void renderOverlayAfterChildren(GuiRenderer graphics, float mouseX, float mouseY, float deltaSeconds) {
        if (hoveredChip) {
            WSettingTooltip.drawEnumTooltipForCurrentValue(graphics, mouseX, mouseY, enumSetting);
            return;
        }
        if (hoveredReset) {
            WSettingTooltip.drawTooltipForSetting(graphics, mouseX, mouseY, enumSetting, hoveredReset);
        }
    }

    @Override
    protected void renderSelf(GuiRenderer graphics, float mouseX, float mouseY, float deltaSeconds) {
        boolean locked = enumSetting.isLocked();
        float innerHeight = Math.max(0f, h() - 2f * SettingsUiMetrics.SETTING_ROW_PADDING_Y);
        float padY = SettingsUiMetrics.SETTING_ROW_PADDING_Y;
        float padX = SettingsUiMetrics.SETTING_ROW_PADDING_X;
        float bodyLeft = x() + padX;
        String label = enumSetting.getTranslatedDisplayName();
        float[] chip = valueChipBounds();
        float chipH = chip[3];
        float titleRowHeight = Math.max(ModSettingsTheme.shellDesignBodyLineHeight(), chipH);
        float bodyTop = y() + padY;
        float titleOriginY = bodyTop + (innerHeight - titleRowHeight) * 0.5f;

        float labelRightEdge = chip[0] - SettingsUiMetrics.SETTING_VALUE_CONTROL_GAP;
        float labelMaxWidth = Math.max(0f, labelRightEdge - bodyLeft);
        label = TextLineLayout.ellipsize(label, labelMaxWidth, segment -> graphics.measureTextWidth(segment, false));
        float labelY = titleOriginY + Math.max(0f, (chipH - graphics.getFontHeight()) * 0.5f);
        int labelColor = locked ? graphics.theme().textMuted() : graphics.theme().textPrimary();
        graphics.drawMiniMessageText("<color:" + Colors.rgbHex(labelColor) + ">" + label + "</color>", bodyLeft, labelY, false);
        float chipCorner = Mth.clamp(graphics.theme().cardCornerRadius(), 0.5f, Math.min(chip[2], chipH) * 0.5f - 0.01f);
        int fill = graphics.theme().surface();
        int border = graphics.theme().border();
        if (locked) {
            fill = WSettingTooltip.dimColor(fill, 0.5f);
            border = WSettingTooltip.dimColor(border, 0.6f);
        }
        graphics.fillRoundedRectFrame(chip[0], chip[1], chip[2], chipH, chipCorner, border, fill, 1f, 1f, RectCornerRoundMask.ALL);
        float chipInnerTextWidth = Math.max(0f, chip[2] - 8f);
        String valueText = TextLineLayout.ellipsize(enumSetting.formatValueForDisplay(), chipInnerTextWidth, segment -> graphics.measureTextWidth(segment, false));
        float valueTextW = graphics.measureTextWidth(valueText, false);
        float valueDrawX = chip[0] + Math.max(0f, (chip[2] - valueTextW) * 0.5f);
        float valueDrawY = chip[1] + Math.max(0f, (chipH - graphics.getFontHeight()) * 0.5f);
        int valueColor = locked ? graphics.theme().textMuted() : graphics.theme().textPrimary();
        graphics.drawMiniMessageText("<color:" + Colors.rgbHex(valueColor) + ">" + valueText + "</color>", valueDrawX, valueDrawY, false);
        float[] resetSquare = inlineResetSquare();
        SettingRowResetLayout.paintGlyph(graphics, resetSquare[0], resetSquare[1], chipH, hoveredReset && !locked, enumSetting.isAtDefault());
    }

    private float[] valueChipBounds() {
        float innerHeight = Math.max(0f, h() - 2f * SettingsUiMetrics.SETTING_ROW_PADDING_Y);
        float padY = SettingsUiMetrics.SETTING_ROW_PADDING_Y;
        float padX = SettingsUiMetrics.SETTING_ROW_PADDING_X;
        float bodyLeft = x() + padX;
        float chipH = SettingsUiMetrics.SHELL_CONTROL_HEIGHT_DESIGN;
        float titleRowHeight = Math.max(ModSettingsTheme.shellDesignBodyLineHeight(), chipH);
        float bodyTop = y() + padY;
        float titleOriginY = bodyTop + (innerHeight - titleRowHeight) * 0.5f;
        float contentRight = x() + outerWidth;
        float resetLeft = SettingRowResetLayout.trailingResetLeftX(contentRight);
        float gapBeforeReset = SettingRowResetLayout.resetGapBesideControlPx();
        float chipRight = resetLeft - gapBeforeReset;
        float minChipWidth = 72f;
        float desiredChipWidth = Math.max(minChipWidth, valueChipWidthEstimate() + 16f);
        float maxChipWidth = Math.max(minChipWidth, chipRight - bodyLeft);
        float chipW = Math.min(desiredChipWidth, maxChipWidth);
        float desiredChipLeft = bodyLeft + valueColumnStartX;
        float maxChipLeft = chipRight - chipW;
        float chipLeft = Mth.clamp(desiredChipLeft, bodyLeft, maxChipLeft);
        return new float[]{chipLeft, titleOriginY, chipW, chipH};
    }

    private float valueChipWidthEstimate() {
        Enum<?>[] choices = enumSetting.enumType().getEnumConstants();
        float maxW = 0f;
        for (Enum<?> choice : choices) {
            String optionLabel = enumSetting.formatUntypedValueForDisplay(choice);
            maxW = Math.max(maxW, 8f + estimateTextWidthLogical(optionLabel));
        }
        return maxW;
    }

    private float[] inlineResetSquare() {
        float[] chip = valueChipBounds();
        float chipH = chip[3];
        float contentRight = x() + outerWidth;
        float resetLeft = SettingRowResetLayout.trailingResetLeftX(contentRight);
        float resetTop = SettingRowResetLayout.verticallyCenteredTop(chip[1], chipH);
        float box = SettingRowResetLayout.glyphBoxPx();
        return new float[]{resetLeft, resetTop, box};
    }
}
