package cc.fascinated.fascinatedutils.systems.modules.impl;

import cc.fascinated.fascinatedutils.common.setting.impl.BooleanSetting;
import cc.fascinated.fascinatedutils.common.setting.impl.SliderSetting;
import cc.fascinated.fascinatedutils.systems.modules.Module;
import cc.fascinated.fascinatedutils.systems.modules.ModuleCategory;
import lombok.Getter;

@Getter
public class MovementModule extends Module {

    private final BooleanSetting enableFlightSpeedModifier = BooleanSetting.builder().id("enable_flight_speed_modifier").defaultValue(false).categoryDisplayKey("Flight Speed").build();

    private final SliderSetting flightSpeedModifier = SliderSetting.builder().id("flight_speed_modifier").defaultValue(2f).minValue(2f).maxValue(10f).step(1f).categoryDisplayKey("Flight Speed").build();

    public MovementModule() {
        super("Movement", ModuleCategory.MISC);
        addSetting(enableFlightSpeedModifier);
        addSetting(flightSpeedModifier);
    }
}
