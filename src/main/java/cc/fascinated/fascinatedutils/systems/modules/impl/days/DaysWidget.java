package cc.fascinated.fascinatedutils.systems.modules.impl.days;

import cc.fascinated.fascinatedutils.systems.modules.impl.days.hud.DaysHudPanel;
import cc.fascinated.fascinatedutils.systems.hud.HudDefaults;
import cc.fascinated.fascinatedutils.systems.hud.HudHostModule;
import cc.fascinated.fascinatedutils.systems.hud.MiniMessageHudChrome;

public class DaysWidget extends HudHostModule {

    public DaysWidget() {
        super("days", "Days", HudDefaults.builder().build());
        MiniMessageHudChrome.register(this);
        registerHudPanel(new DaysHudPanel(this));
    }
}
