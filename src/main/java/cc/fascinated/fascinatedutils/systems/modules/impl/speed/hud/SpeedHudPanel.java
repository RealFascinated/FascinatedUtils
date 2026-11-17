package cc.fascinated.fascinatedutils.systems.modules.impl.speed.hud;

import cc.fascinated.fascinatedutils.common.setting.impl.BooleanSetting;
import cc.fascinated.fascinatedutils.systems.hud.HudHostModule;
import cc.fascinated.fascinatedutils.systems.hud.panel.MiniMessageHudPanel;
import cc.fascinated.fascinatedutils.systems.modules.impl.speed.SpeedWidget;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class SpeedHudPanel extends MiniMessageHudPanel {

    private final SpeedWidget speedWidget;

    public SpeedHudPanel(SpeedWidget speedWidget) {
        super(speedWidget, "speed", HudHostModule.UTILITY_WIDGET_MIN_WIDTH);
        this.speedWidget = speedWidget;
    }

    @Override
    protected List<String> computeMiniMessageLines(float deltaSeconds, boolean editorMode) {
        float currentSpeed = speedWidget.getCurrentSpeed();
        if (!Float.isFinite(currentSpeed)) {
            return List.of("<grey>" + hudText("unavailable") + "</grey>");
        }
        List<String> lines = new ArrayList<>();
        lines.add(String.format(Locale.ENGLISH, "%.2f %s", currentSpeed, hudText("unit")));
        BooleanSetting showPeak = speedWidget.getShowPeak();
        if (showPeak.isEnabled() && speedWidget.getBufferCount() > 0) {
            lines.add(String.format(Locale.ENGLISH, "<grey>%s</grey> %.2f", hudText("peak"), speedWidget.sampledPeakSpeed()));
        }
        BooleanSetting showAverage = speedWidget.getShowAverage();
        if (showAverage.isEnabled() && speedWidget.getBufferCount() > 0) {
            lines.add(String.format(Locale.ENGLISH, "<grey>%s</grey> %.2f", hudText("average"), speedWidget.sampledAverageSpeed()));
        }
        return lines;
    }
}
