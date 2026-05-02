package cc.fascinated.fascinatedutils.systems.modules.impl.tps.hud;

import cc.fascinated.fascinatedutils.systems.modules.impl.tps.TpsWidget;
import cc.fascinated.fascinatedutils.systems.hud.MiniMessageHudPanel;

public class TpsHudPanel extends MiniMessageHudPanel {

    public TpsHudPanel(TpsWidget tpsWidget) {
        super(tpsWidget, "tps", tpsWidget.tpsHudBaseMinWidth());
    }
}
