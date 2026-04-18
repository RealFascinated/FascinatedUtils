package cc.fascinated.fascinatedutils.systems.modules.impl;

import cc.fascinated.fascinatedutils.common.NumberUtils;
import cc.fascinated.fascinatedutils.common.setting.impl.BooleanSetting;
import cc.fascinated.fascinatedutils.common.setting.impl.EnumSetting;
import cc.fascinated.fascinatedutils.common.setting.impl.SliderSetting;
import cc.fascinated.fascinatedutils.systems.modules.Module;
import lombok.Getter;

@Getter
public class TabModule extends Module {

    private final EnumSetting<PingMode> pingMode = EnumSetting.<PingMode>builder().id("ping_mode").defaultValue(PingMode.BARS).build();
    private final BooleanSetting coloredPing = BooleanSetting.builder().id("colored_ping").defaultValue(true).build();
    private final SliderSetting maxPlayerSlots = SliderSetting.builder().id("max_player_slots").defaultValue(80f).minValue(80f).maxValue(180f).step(20f).valueFormatter((slots) -> NumberUtils.formatNumber(slots, 0) + " slots").build();

    public TabModule() {
        super("Tab");
        addSetting(pingMode);
        addSetting(coloredPing);
        addSetting(maxPlayerSlots);
    }

    public enum PingMode {
        BARS, MILLISECONDS, NONE
    }
}
