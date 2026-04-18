package cc.fascinated.fascinatedutils.systems.modules.impl;

import cc.fascinated.fascinatedutils.common.setting.impl.SliderSetting;
import cc.fascinated.fascinatedutils.systems.modules.Module;
import lombok.Getter;

@Getter
public class TitlesModule extends Module {
    private final SliderSetting scaleTitleAndSubtitle = SliderSetting.builder().id("scale_title_and_subtitle").defaultValue(1f).step(0.1f).minValue(0.1f).maxValue(1f).build();

    public TitlesModule() {
        super("Titles");
        addSetting(scaleTitleAndSubtitle);
    }
}
