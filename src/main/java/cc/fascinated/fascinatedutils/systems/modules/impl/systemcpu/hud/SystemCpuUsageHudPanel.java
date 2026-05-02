package cc.fascinated.fascinatedutils.systems.modules.impl.systemcpu.hud;

import cc.fascinated.fascinatedutils.common.Colors;
import cc.fascinated.fascinatedutils.common.setting.impl.BooleanSetting;
import cc.fascinated.fascinatedutils.gui.theme.UITheme;
import cc.fascinated.fascinatedutils.gui.theme.UiColor;
import cc.fascinated.fascinatedutils.systems.modules.impl.systemcpu.SystemCpuUsageWidget;
import cc.fascinated.fascinatedutils.systems.hud.HudHostModule;
import cc.fascinated.fascinatedutils.systems.hud.MiniMessageHudPanel;
import net.minecraft.util.Mth;

import java.util.List;

public class SystemCpuUsageHudPanel extends MiniMessageHudPanel {

    private static final int CPU_COLOR_GREEN = UiColor.argb("#00e676");
    private static final int CPU_COLOR_AMBER = UiColor.argb("#ddaa33");
    private static final int CPU_COLOR_RED = UiColor.argb("#dd4444");

    private final SystemCpuUsageWidget systemCpuWidget;

    public SystemCpuUsageHudPanel(SystemCpuUsageWidget systemCpuUsageWidget) {
        super(systemCpuUsageWidget, "system_cpu", HudHostModule.UTILITY_WIDGET_MIN_WIDTH);
        this.systemCpuWidget = systemCpuUsageWidget;
    }

    @Override
    protected List<String> computeMiniMessageLines(float deltaSeconds, boolean editorMode) {
        float systemCpuPercent = systemCpuWidget.sampleSystemCpuPercent();
        if (!Float.isFinite(systemCpuPercent)) {
            return List.of("<grey>CPU N/A</grey>");
        }
        float clampedPercent = clampPercent(systemCpuPercent);
        int roundedPercent = Math.round(clampedPercent);
        BooleanSetting useCpuColorSetting = systemCpuWidget.getUseCpuColor();
        int color = useCpuColorSetting.isEnabled() ? cpuColorArgb(clampedPercent) : UITheme.COLOR_TEXT_PRIMARY;
        return List.of("<color:" + Colors.rgbHex(color) + "><white>" + roundedPercent + "% CPU");
    }

    private static float clampPercent(float cpuPercent) {
        return Math.max(0f, Math.min(100f, cpuPercent));
    }

    private static int cpuColorArgb(float cpuPercent) {
        if (cpuPercent <= 60f) {
            return CPU_COLOR_GREEN;
        }
        if (cpuPercent <= 85f) {
            float transition = smoothstep((cpuPercent - 60f) / 25f);
            return Colors.mixArgb(transition, CPU_COLOR_GREEN, CPU_COLOR_AMBER);
        }
        float transition = smoothstep((cpuPercent - 85f) / 15f);
        return Colors.mixArgb(transition, CPU_COLOR_AMBER, CPU_COLOR_RED);
    }

    private static float smoothstep(float value) {
        float clamped = Mth.clamp(value, 0f, 1f);
        return clamped * clamped * (3f - 2f * clamped);
    }
}
