package cc.fascinated.fascinatedutils.systems.modules.impl.systemcpu.hud;

import cc.fascinated.fascinatedutils.systems.modules.impl.systemcpu.SystemCpuUsageWidget;
import cc.fascinated.fascinatedutils.systems.hud.HudHostModule;
import cc.fascinated.fascinatedutils.systems.hud.MiniMessageHudPanel;

public class SystemCpuUsageHudPanel extends MiniMessageHudPanel {

    public SystemCpuUsageHudPanel(SystemCpuUsageWidget systemCpuUsageWidget) {
        super(systemCpuUsageWidget, "system_cpu", HudHostModule.UTILITY_WIDGET_MIN_WIDTH);
    }
}
