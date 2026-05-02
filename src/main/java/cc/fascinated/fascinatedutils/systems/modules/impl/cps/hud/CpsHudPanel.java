package cc.fascinated.fascinatedutils.systems.modules.impl.cps.hud;

import cc.fascinated.fascinatedutils.systems.modules.impl.cps.CpsWidget;
import cc.fascinated.fascinatedutils.systems.hud.HudHostModule;
import cc.fascinated.fascinatedutils.systems.hud.MiniMessageHudPanel;

public class CpsHudPanel extends MiniMessageHudPanel {

    public CpsHudPanel(CpsWidget cpsWidget) {
        super(cpsWidget, "cps", HudHostModule.UTILITY_WIDGET_MIN_WIDTH);
    }
}
