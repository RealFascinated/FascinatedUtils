package cc.fascinated.fascinatedutils.systems.modules.impl.days;

import cc.fascinated.fascinatedutils.systems.hud.HudDefaults;
import cc.fascinated.fascinatedutils.systems.hud.HudHostModule;
import cc.fascinated.fascinatedutils.systems.modules.impl.days.hud.DaysHudPanel;

public class DaysCounterWidget extends HudHostModule {


    public DaysCounterWidget() {
        super("days_counter", "Days Counter", HudDefaults.builder().build());
        addAppearanceSettings(true);
        registerHudPanel(new DaysHudPanel(this));
    }
}
