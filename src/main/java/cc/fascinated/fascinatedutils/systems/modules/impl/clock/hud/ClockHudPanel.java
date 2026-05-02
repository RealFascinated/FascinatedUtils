package cc.fascinated.fascinatedutils.systems.modules.impl.clock.hud;

import cc.fascinated.fascinatedutils.systems.modules.impl.clock.ClockWidget;
import cc.fascinated.fascinatedutils.systems.hud.MiniMessageHudPanel;

public class ClockHudPanel extends MiniMessageHudPanel {

    public ClockHudPanel(ClockWidget clockWidget) {
        super(clockWidget, "clock", 0f);
    }
}
