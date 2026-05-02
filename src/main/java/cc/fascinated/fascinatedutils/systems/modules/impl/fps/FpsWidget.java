package cc.fascinated.fascinatedutils.systems.modules.impl.fps;

import cc.fascinated.fascinatedutils.common.setting.impl.BooleanSetting;
import cc.fascinated.fascinatedutils.systems.modules.impl.fps.hud.FpsHudPanel;
import cc.fascinated.fascinatedutils.systems.hud.HudDefaults;
import cc.fascinated.fascinatedutils.systems.hud.HudHostModule;
import cc.fascinated.fascinatedutils.systems.hud.MiniMessageHudChrome;

import lombok.Getter;

@Getter
public class FpsWidget extends HudHostModule {

    private final BooleanSetting showOnePercentLows = BooleanSetting.builder().id("show_one_percent_lows")
            .defaultValue(false)
            .categoryDisplayKey(APPEARANCE_CATEGORY_DISPLAY_KEY)
            .build();

    private final BooleanSetting showPointOnePercentLows = BooleanSetting.builder().id("show_point_one_percent_lows")
            .defaultValue(false)
            .categoryDisplayKey(APPEARANCE_CATEGORY_DISPLAY_KEY)
            .build();

    public FpsWidget() {
        super("fps", "FPS", defaults());
        MiniMessageHudChrome.register(this);
        addSetting(showOnePercentLows);
        addSetting(showPointOnePercentLows);
        registerHudPanel(new FpsHudPanel(this));
    }

    private static HudDefaults defaults() {
        return HudDefaults.builder().build();
    }
}
