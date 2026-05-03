package cc.fascinated.fascinatedutils.systems.modules.impl.systemcpu;

import cc.fascinated.fascinatedutils.common.setting.impl.BooleanSetting;
import cc.fascinated.fascinatedutils.systems.hud.HudDefaults;
import cc.fascinated.fascinatedutils.systems.hud.HudHostModule;
import cc.fascinated.fascinatedutils.systems.hud.MiniMessageHudChrome;
import cc.fascinated.fascinatedutils.systems.modules.impl.systemcpu.hud.SystemCpuUsageHudPanel;
import lombok.Getter;

import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.lang.reflect.Method;

@Getter
public class SystemCpuUsageWidget extends HudHostModule {
    private final BooleanSetting useCpuColor = BooleanSetting.builder().id("use_cpu_color").defaultValue(true).categoryDisplayKey(APPEARANCE_CATEGORY_DISPLAY_KEY).build();
    private final OperatingSystemMXBean operatingSystemBean;
    private final Method systemCpuLoadMethod;
    private final Method cpuLoadMethod;

    public SystemCpuUsageWidget() {
        super("system_cpu", "System CPU Usage", HudDefaults.builder().build());
        MiniMessageHudChrome.register(this);
        addSetting(useCpuColor);
        this.operatingSystemBean = ManagementFactory.getOperatingSystemMXBean();
        this.systemCpuLoadMethod = resolveMethod(operatingSystemBean, "getSystemCpuLoad");
        this.cpuLoadMethod = resolveMethod(operatingSystemBean, "getCpuLoad");
        registerHudPanel(new SystemCpuUsageHudPanel(this));
    }

    private static Method resolveMethod(OperatingSystemMXBean operatingSystemBeanHandle, String methodName) {
        try {
            return operatingSystemBeanHandle.getClass().getMethod(methodName);
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }

    double invokeCpuLoadFraction(Method cpuLoadMethodHandle) {
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

    public float sampleSystemCpuPercent() {
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
}
