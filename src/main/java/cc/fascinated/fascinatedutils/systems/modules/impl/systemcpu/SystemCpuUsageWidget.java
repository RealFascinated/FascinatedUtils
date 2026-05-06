package cc.fascinated.fascinatedutils.systems.modules.impl.systemcpu;

import cc.fascinated.fascinatedutils.common.setting.impl.BooleanSetting;
import cc.fascinated.fascinatedutils.common.setting.impl.ColorSetting;
import cc.fascinated.fascinatedutils.common.setting.impl.SliderSetting;
import cc.fascinated.fascinatedutils.systems.hud.HudDefaults;
import cc.fascinated.fascinatedutils.systems.hud.HudHostModule;
import cc.fascinated.fascinatedutils.systems.hud.HudWidgetAppearanceBuilders;
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

    private final BooleanSetting showBackground = HudWidgetAppearanceBuilders.showBackground().build();
    private final BooleanSetting roundedCorners = HudWidgetAppearanceBuilders.roundedCorners().build();
    private final SliderSetting roundingRadius = HudWidgetAppearanceBuilders.roundingRadius().build();
    private final BooleanSetting showBorder = HudWidgetAppearanceBuilders.showBorder().build();
    private final SliderSetting borderThickness = HudWidgetAppearanceBuilders.borderThickness().build();
    private final ColorSetting backgroundColor = HudWidgetAppearanceBuilders.backgroundColor().build();
    private final ColorSetting borderColor = HudWidgetAppearanceBuilders.borderColor().build();
    private final BooleanSetting removeMinimumWidth = HudWidgetAppearanceBuilders.removeMinimumWidth().build();
    private final SliderSetting padding = HudWidgetAppearanceBuilders.padding().build();
    private final BooleanSetting textShadow = HudWidgetAppearanceBuilders.textShadow().build();

    public SystemCpuUsageWidget() {
        super("system_cpu", "System CPU Usage", HudDefaults.builder().build());
        addSetting(showBackground);
        addSetting(roundedCorners);
        addSetting(showBorder);
        addSetting(roundingRadius);
        addSetting(borderThickness);
        addSetting(backgroundColor);
        addSetting(borderColor);
        showBackground.addSubSetting(backgroundColor);
        roundedCorners.addSubSetting(roundingRadius);
        showBorder.addSubSetting(borderThickness);
        showBorder.addSubSetting(borderColor);
        addSetting(removeMinimumWidth);
        addSetting(padding);
        addSetting(textShadow);
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
