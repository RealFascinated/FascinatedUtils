package cc.fascinated.fascinatedutils.systems.modules.impl.ping.hud;

import cc.fascinated.fascinatedutils.common.Colors;
import cc.fascinated.fascinatedutils.common.PingColors;
import cc.fascinated.fascinatedutils.common.setting.impl.BooleanSetting;
import cc.fascinated.fascinatedutils.systems.modules.impl.ping.PingWidget;
import cc.fascinated.fascinatedutils.systems.hud.HudHostModule;
import cc.fascinated.fascinatedutils.systems.hud.MiniMessageHudPanel;

import java.util.List;
import java.util.concurrent.TimeUnit;

public class PingHudPanel extends MiniMessageHudPanel {

    private static final long UPDATE_INTERVAL_NANOS = TimeUnit.MILLISECONDS.toNanos(1_000L);

    private final PingWidget pingWidget;

    public PingHudPanel(PingWidget pingWidget) {
        super(pingWidget, "ping", HudHostModule.UTILITY_WIDGET_MIN_WIDTH);
        this.pingWidget = pingWidget;
    }

    @Override
    protected long miniMessageLineUpdateIntervalNanos() {
        return UPDATE_INTERVAL_NANOS;
    }

    @Override
    protected List<String> computeMiniMessageLines(float deltaSeconds, boolean editorMode) {
        int pingMs = pingWidget.resolvedPingMs();
        if (pingMs <= 0) {
            return List.of("<yellow>... <white>ms");
        }

        BooleanSetting usePingColor = pingWidget.getUsePingColor();
        int color = usePingColor.isEnabled() ? PingColors.getPingColor(pingMs) : 0xFFFFFFFF;
        return List.of("<color:" + Colors.rgbHex(color) + ">" + pingMs + "</color> ms");
    }
}
