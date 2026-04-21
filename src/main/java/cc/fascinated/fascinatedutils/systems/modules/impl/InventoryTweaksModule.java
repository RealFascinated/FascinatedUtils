package cc.fascinated.fascinatedutils.systems.modules.impl;

import cc.fascinated.fascinatedutils.common.setting.impl.BooleanSetting;
import cc.fascinated.fascinatedutils.systems.modules.Module;
import cc.fascinated.fascinatedutils.systems.modules.ModuleCategory;
import lombok.Getter;

@Getter
public class InventoryTweaksModule extends Module {

    private final BooleanSetting quickMove = BooleanSetting.builder().id("quick_move").defaultValue(true).build();
    private final BooleanSetting scrollMove = BooleanSetting.builder().id("scroll_move").defaultValue(true).build();

    public InventoryTweaksModule() {
        super("Inventory Tweaks", ModuleCategory.GENERAL);

        addSetting(quickMove);
        addSetting(scrollMove);
    }
}
