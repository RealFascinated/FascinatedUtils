package cc.fascinated.fascinatedutils.systems.modules.impl;

import cc.fascinated.fascinatedutils.common.setting.impl.SliderSetting;
import cc.fascinated.fascinatedutils.systems.modules.Module;
import cc.fascinated.fascinatedutils.systems.modules.ModuleCategory;
import lombok.Getter;

@Getter
public class FullbrightModule extends Module {

    private final SliderSetting brightness = SliderSetting.builder().id("brightness").defaultValue(1f).minValue(0.1f).maxValue(1f).step(0.1f).build();

    public FullbrightModule() {
        super("Fullbright", ModuleCategory.GENERAL);
        addSetting(brightness);
    }
}
