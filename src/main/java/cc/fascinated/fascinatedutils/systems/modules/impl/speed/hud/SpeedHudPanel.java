package cc.fascinated.fascinatedutils.systems.modules.impl.speed.hud;

import cc.fascinated.fascinatedutils.systems.modules.impl.speed.SpeedWidget;
import cc.fascinated.fascinatedutils.systems.hud.HudHostModule;
import cc.fascinated.fascinatedutils.systems.hud.MiniMessageHudPanel;

public class SpeedHudPanel extends MiniMessageHudPanel {

    public SpeedHudPanel(SpeedWidget speedWidget) {
        super(speedWidget, "speed", HudHostModule.UTILITY_WIDGET_MIN_WIDTH);
    }
}
