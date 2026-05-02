package cc.fascinated.fascinatedutils.systems.modules.impl.fps.hud;

import cc.fascinated.fascinatedutils.systems.modules.impl.fps.FpsWidget;
import cc.fascinated.fascinatedutils.systems.hud.HudHostModule;
import cc.fascinated.fascinatedutils.systems.hud.MiniMessageHudPanel;

public class FpsHudPanel extends MiniMessageHudPanel {

    public FpsHudPanel(FpsWidget fpsWidget) {
        super(fpsWidget, "fps", HudHostModule.UTILITY_WIDGET_MIN_WIDTH);
    }
}
