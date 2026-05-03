package cc.fascinated.fascinatedutils.systems.modules.impl;

import cc.fascinated.fascinatedutils.common.setting.impl.SliderSetting;
import cc.fascinated.fascinatedutils.systems.modules.Module;
import cc.fascinated.fascinatedutils.systems.modules.ModuleCategory;
import lombok.Getter;

@Getter
public class FogCustomizerModule extends Module {

    private static final String FOG_DENSITY_CATEGORY_KEY = "fascinatedutils.setting.category.fog_density";

    private final SliderSetting atmosphericFogStrength = SliderSetting.builder().id("atmospheric_fog").defaultValue(1f).minValue(0f).maxValue(2f).step(0.1f).categoryDisplayKey(FOG_DENSITY_CATEGORY_KEY).build();
    private final SliderSetting waterFogStrength = SliderSetting.builder().id("water_fog").defaultValue(1f).minValue(0f).maxValue(2f).step(0.1f).categoryDisplayKey(FOG_DENSITY_CATEGORY_KEY).build();
    private final SliderSetting netherFogStrength = SliderSetting.builder().id("nether_fog").defaultValue(1f).minValue(0f).maxValue(2f).step(0.1f).categoryDisplayKey(FOG_DENSITY_CATEGORY_KEY).build();
    private final SliderSetting endFogStrength = SliderSetting.builder().id("end_fog").defaultValue(1f).minValue(0f).maxValue(2f).step(0.1f).categoryDisplayKey(FOG_DENSITY_CATEGORY_KEY).build();

    public FogCustomizerModule() {
        super("Fog Customizer", ModuleCategory.GENERAL);
        addSetting(atmosphericFogStrength);
        addSetting(waterFogStrength);
        addSetting(netherFogStrength);
        addSetting(endFogStrength);
    }
}