package cc.fascinated.fascinatedutils.systems.modules.impl.ping.hud;

import cc.fascinated.fascinatedutils.systems.modules.impl.ping.PingWidget;
import cc.fascinated.fascinatedutils.systems.hud.HudHostModule;
import cc.fascinated.fascinatedutils.systems.hud.MiniMessageHudPanel;

public class PingHudPanel extends MiniMessageHudPanel {

    public PingHudPanel(PingWidget pingWidget) {
        super(pingWidget, "ping", HudHostModule.UTILITY_WIDGET_MIN_WIDTH);
    }
}
