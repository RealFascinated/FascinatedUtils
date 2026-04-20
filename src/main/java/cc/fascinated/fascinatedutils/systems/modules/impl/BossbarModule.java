package cc.fascinated.fascinatedutils.systems.modules.impl;

import cc.fascinated.fascinatedutils.common.setting.impl.BooleanSetting;
import cc.fascinated.fascinatedutils.systems.modules.Module;
import cc.fascinated.fascinatedutils.systems.modules.ModuleCategory;
import lombok.Getter;

@Getter
public class BossbarModule extends Module {

    private final BooleanSetting hideBossHealth = BooleanSetting.builder().id("hide_boss_health").defaultValue(false).build();

    public BossbarModule() {
        super("Bossbar", ModuleCategory.MISC);
        addSetting(hideBossHealth);
    }
}
