package cc.fascinated.fascinatedutils.systems.modules.impl.reach.hud;

import cc.fascinated.fascinatedutils.systems.modules.impl.reach.ReachWidget;
import cc.fascinated.fascinatedutils.systems.hud.HudHostModule;
import cc.fascinated.fascinatedutils.systems.hud.MiniMessageHudPanel;

public class ReachHudPanel extends MiniMessageHudPanel {

    public ReachHudPanel(ReachWidget reachWidget) {
        super(reachWidget, "reach", HudHostModule.UTILITY_WIDGET_MIN_WIDTH);
    }
}
