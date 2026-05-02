package cc.fascinated.fascinatedutils.systems.modules.impl.speed.hud;

import cc.fascinated.fascinatedutils.common.setting.impl.BooleanSetting;
import cc.fascinated.fascinatedutils.systems.modules.impl.speed.SpeedWidget;
import cc.fascinated.fascinatedutils.systems.hud.HudHostModule;
import cc.fascinated.fascinatedutils.systems.hud.MiniMessageHudPanel;

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
            return List.of("<grey>Speed N/A</grey>");
        }
        List<String> lines = new ArrayList<>();
        lines.add(String.format(Locale.ENGLISH, "%.2f b/s", currentSpeed));
        BooleanSetting showPeak = speedWidget.getShowPeak();
        if (showPeak.isEnabled() && speedWidget.getBufferCount() > 0) {
            lines.add(String.format(Locale.ENGLISH, "<grey>Peak:</grey> %.2f", speedWidget.sampledPeakSpeed()));
        }
        BooleanSetting showAverage = speedWidget.getShowAverage();
        if (showAverage.isEnabled() && speedWidget.getBufferCount() > 0) {
            lines.add(String.format(Locale.ENGLISH, "<grey>Avg:</grey> %.2f", speedWidget.sampledAverageSpeed()));
        }
        return lines;
    }
}
