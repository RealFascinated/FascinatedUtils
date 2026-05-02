package cc.fascinated.fascinatedutils.systems.modules.impl.cps;

import cc.fascinated.fascinatedutils.common.setting.impl.BooleanSetting;
import cc.fascinated.fascinatedutils.systems.modules.impl.cps.hud.CpsHudPanel;
import cc.fascinated.fascinatedutils.systems.hud.HudDefaults;
import cc.fascinated.fascinatedutils.systems.hud.HudHostModule;
import cc.fascinated.fascinatedutils.systems.hud.MiniMessageHudChrome;
import lombok.Getter;

@Getter
public class CpsWidget extends HudHostModule {
    private final BooleanSetting splitMouseButtons = BooleanSetting.builder().id("split_mouse_buttons")

            .defaultValue(false).categoryDisplayKey(APPEARANCE_CATEGORY_DISPLAY_KEY).build();

    public CpsWidget() {
        super("cps", "CPS", HudDefaults.builder().build());
        MiniMessageHudChrome.register(this);
        addSetting(splitMouseButtons);
        registerHudPanel(new CpsHudPanel(this));
    }
}
