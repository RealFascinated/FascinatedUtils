package cc.fascinated.fascinatedutils.systems.modules.impl;

import cc.fascinated.fascinatedutils.common.setting.impl.BooleanSetting;
import cc.fascinated.fascinatedutils.systems.modules.Module;
import cc.fascinated.fascinatedutils.systems.modules.ModuleCategory;
import lombok.Getter;

@Getter
public class HurtcamModule extends Module {

    private final BooleanSetting cancelHurtcamAnimation = BooleanSetting.builder().id("cancel_hurtcam_animation").defaultValue(false).build();

    public HurtcamModule() {
        super("Hurtcam", ModuleCategory.MISC);

        addSetting(cancelHurtcamAnimation);
    }
}
