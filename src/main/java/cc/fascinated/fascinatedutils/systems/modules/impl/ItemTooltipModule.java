package cc.fascinated.fascinatedutils.systems.modules.impl;

import cc.fascinated.fascinatedutils.common.setting.impl.BooleanSetting;
import cc.fascinated.fascinatedutils.systems.modules.Module;
import cc.fascinated.fascinatedutils.systems.modules.ModuleCategory;
import lombok.Getter;

@Getter
public class ItemTooltipModule extends Module {

    private final BooleanSetting gapAboveInfo = BooleanSetting.builder()
            .id("gap_above_info")
            .defaultValue(true)
            .build();

    private final BooleanSetting showItemSize = BooleanSetting.builder()
            .id("show_item_size")
            .defaultValue(true)
            .build();

    public ItemTooltipModule() {
        super("Item Tooltip", ModuleCategory.GENERAL);
        addSetting(gapAboveInfo);
        addSetting(showItemSize);
    }
}
