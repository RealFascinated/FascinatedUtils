package cc.fascinated.fascinatedutils.systems.modules.impl;

import cc.fascinated.fascinatedutils.common.setting.impl.SliderSetting;
import cc.fascinated.fascinatedutils.systems.modules.Module;
import cc.fascinated.fascinatedutils.systems.modules.ModuleCategory;
import lombok.Getter;

@Getter
public class FogCustomizerModule extends Module {

    private final SliderSetting atmosphericFogStrength = SliderSetting.builder().id("atmospheric_fog").defaultValue(1f).minValue(0f).maxValue(2f).step(0.1f).categoryDisplayKey("Fog Density").build();
    private final SliderSetting waterFogStrength = SliderSetting.builder().id("water_fog").defaultValue(1f).minValue(0f).maxValue(2f).step(0.1f).categoryDisplayKey("Fog Density").build();
    private final SliderSetting netherFogStrength = SliderSetting.builder().id("nether_fog").defaultValue(1f).minValue(0f).maxValue(2f).step(0.1f).categoryDisplayKey("Fog Density").build();
    private final SliderSetting endFogStrength = SliderSetting.builder().id("end_fog").defaultValue(1f).minValue(0f).maxValue(2f).step(0.1f).categoryDisplayKey("Fog Density").build();

    public FogCustomizerModule() {
        super("Fog Customizer", ModuleCategory.GENERAL);
        addSetting(atmosphericFogStrength);
        addSetting(waterFogStrength);
        addSetting(netherFogStrength);
        addSetting(endFogStrength);
    }
}