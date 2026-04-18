package cc.fascinated.fascinatedutils.systems.modules.impl;

import cc.fascinated.fascinatedutils.common.setting.impl.BooleanSetting;
import cc.fascinated.fascinatedutils.systems.modules.Module;
import lombok.Getter;

@Getter
public class WorldModule extends Module {

    private final BooleanSetting renderFog = BooleanSetting.builder().id("render_fog").defaultValue(true).categoryDisplayKey("World").build();

    public WorldModule() {
        super("World");
        addSetting(renderFog);
    }
}