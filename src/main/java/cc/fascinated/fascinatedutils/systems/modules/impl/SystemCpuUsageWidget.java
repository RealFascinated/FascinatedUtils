package cc.fascinated.fascinatedutils.systems.modules.impl;

import cc.fascinated.fascinatedutils.common.ColorUtils;
import cc.fascinated.fascinatedutils.common.setting.impl.BooleanSetting;
import cc.fascinated.fascinatedutils.gui.theme.UITheme;
import cc.fascinated.fascinatedutils.gui.theme.UiColor;
import cc.fascinated.fascinatedutils.systems.hud.HudMiniMessageModule;
import net.minecraft.util.Mth;

import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.lang.reflect.Method;
import java.util.List;

public class SystemCpuUsageWidget extends HudMiniMessageModule {
    private static final int CPU_COLOR_GREEN = UiColor.argb("#00e676");
    private static final int CPU_COLOR_AMBER = UiColor.argb("#ddaa33");
    private static final int CPU_COLOR_RED = UiColor.argb("#dd4444");
    private final BooleanSetting useCpuColor = BooleanSetting.builder().id("use_cpu_color").defaultValue(true).categoryDisplayKey(APPEARANCE_CATEGORY_DISPLAY_KEY).build();
    private final OperatingSystemMXBean operatingSystemBean;
    private final Method systemCpuLoadMethod;
    private final Method cpuLoadMethod;

    public SystemCpuUsageWidget() {
        super("system_cpu", "System CPU Usage", 56f);
        addSetting(useCpuColor);
        this.operatingSystemBean = ManagementFactory.getOperatingSystemMXBean();
        this.systemCpuLoadMethod = resolveMethod(operatingSystemBean, "getSystemCpuLoad");
        this.cpuLoadMethod = resolveMethod(operatingSystemBean, "getCpuLoad");
    }

    private static Method resolveMethod(OperatingSystemMXBean operatingSystemBean, String methodName) {
        try {
            return operatingSystemBean.getClass().getMethod(methodName);
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }

    private static float clampPercent(float cpuPercent) {
        return Math.max(0f, Math.min(100f, cpuPercent));
    }

    private static int cpuColorArgb(float cpuPercent) {
        if (cpuPercent <= 60f) {
            return CPU_COLOR_GREEN;
        }
        if (cpuPercent <= 85f) {
            float t = smoothstep((cpuPercent - 60f) / 25f);
            return ColorUtils.mixArgb(t, CPU_COLOR_GREEN, CPU_COLOR_AMBER);
        }
        float t = smoothstep((cpuPercent - 85f) / 15f);
        return ColorUtils.mixArgb(t, CPU_COLOR_AMBER, CPU_COLOR_RED);
    }

    private static float smoothstep(float value) {
        float clamped = Mth.clamp(value, 0f, 1f);
        return clamped * clamped * (3f - 2f * clamped);
    }

    @Override
    protected List<String> lines(float deltaSeconds) {
        float systemCpuPercent = sampleSystemCpuPercent();
        if (!Float.isFinite(systemCpuPercent)) {
            return List.of("<grey>CPU N/A</grey>");
        }
        float clampedPercent = clampPercent(systemCpuPercent);
        int roundedPercent = Math.round(clampedPercent);
        int color = useCpuColor.getValue() ? cpuColorArgb(clampedPercent) : UITheme.COLOR_TEXT_PRIMARY;
        return List.of("<color:" + ColorUtils.rgbHex(color) + "><white>" + roundedPercent + "% CPU");
    }

    private float sampleSystemCpuPercent() {
        double reflectedSystemLoad = invokeCpuLoadFraction(systemCpuLoadMethod);
        if (reflectedSystemLoad >= 0d && Double.isFinite(reflectedSystemLoad)) {
            return (float) (reflectedSystemLoad * 100d);
        }
        double reflectedGenericLoad = invokeCpuLoadFraction(cpuLoadMethod);
        if (reflectedGenericLoad >= 0d && Double.isFinite(reflectedGenericLoad)) {
            return (float) (reflectedGenericLoad * 100d);
        }
        double loadAverage = operatingSystemBean.getSystemLoadAverage();
        if (loadAverage >= 0d && Double.isFinite(loadAverage)) {
            int processorCount = Math.max(1, operatingSystemBean.getAvailableProcessors());
            return (float) ((loadAverage * 100d) / processorCount);
        }
        return Float.NaN;
    }

    private double invokeCpuLoadFraction(Method cpuLoadMethodHandle) {
        if (cpuLoadMethodHandle == null) {
            return Double.NaN;
        }
        try {
            Object value = cpuLoadMethodHandle.invoke(operatingSystemBean);
            if (value instanceof Number numberValue) {
                return numberValue.doubleValue();
            }
        } catch (ReflectiveOperationException ignored) {
            return Double.NaN;
        }
        return Double.NaN;
    }
}
