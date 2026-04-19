package cc.fascinated.fascinatedutils.systems.modules.impl;

import cc.fascinated.fascinatedutils.common.setting.impl.SliderSetting;
import cc.fascinated.fascinatedutils.systems.modules.Module;
import lombok.Getter;

@Getter
public class FogCustomizerModule extends Module {

    private final SliderSetting atmosphericFogStrength = SliderSetting.builder().id("atmospheric_fog").defaultValue(1f).minValue(0f).maxValue(1.5f).step(0.1f).categoryDisplayKey("Fog Density").build();

    public FogCustomizerModule() {
        super("Fog Customizer");
        addSetting(atmosphericFogStrength);
    }
}