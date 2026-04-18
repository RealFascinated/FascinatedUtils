package cc.fascinated.fascinatedutils.gui.widgets.settings;

import cc.fascinated.fascinatedutils.common.ColorUtils;
import cc.fascinated.fascinatedutils.common.setting.Setting;
import cc.fascinated.fascinatedutils.common.setting.impl.EnumSetting;
import cc.fascinated.fascinatedutils.gui.renderer.GuiRenderer;
import cc.fascinated.fascinatedutils.gui.widgets.WTooltip;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public class WSettingTooltip {

    public static int dimColor(int color, float keepAmount) {
        return ColorUtils.mixArgb(Math.max(0f, Math.min(1f, keepAmount)), 0xFF000000, color);
    }

    public static String lockReasonText(Setting<?> setting) {
        String reason = setting.getLockedReasonText();
        return reason == null ? Component.translatable("fascinatedutils.setting.ui.lock_reason.default").getString() : reason;
    }

    public static void drawTooltipForSetting(GuiRenderer graphics, float mouseX, float mouseY, Setting<?> setting) {
        drawTooltipForSetting(graphics, mouseX, mouseY, setting, false);
    }

    public static void drawTooltipForSetting(GuiRenderer graphics, float mouseX, float mouseY, Setting<?> setting, boolean hoveredResetButton) {
        if (hoveredResetButton) {
            WTooltip.draw(graphics, mouseX, mouseY, Component.translatable("fascinatedutils.setting.ui.reset.tooltip").getString());
            return;
        }
        String text = setting.isLocked() ? lockReasonText(setting) : setting.getTooltipDescriptionText();
        if (text == null || text.isBlank()) {
            return;
        }
        WTooltip.draw(graphics, mouseX, mouseY, text);
    }

    public static void drawEnumTooltipForCurrentValue(GuiRenderer graphics, float mouseX, float mouseY, EnumSetting<?> setting) {
        Enum<?>[] constants = setting.enumType().getEnumConstants();
        if (constants.length == 0) {
            return;
        }

        Enum<?> currentValue = setting.getValue();
        List<String> lines = new ArrayList<>(constants.length);
        int currentLineIndex = -1;
        for (int lineIndex = 0; lineIndex < constants.length; lineIndex++) {
            Enum<?> constant = constants[lineIndex];
            lines.add(setting.formatUntypedValueForDisplay(constant));
            if (constant == currentValue) {
                currentLineIndex = lineIndex;
            }
        }

        WTooltip.draw(graphics, mouseX, mouseY, lines, currentLineIndex);
    }
}
