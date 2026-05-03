package cc.fascinated.fascinatedutils.systems.modules.impl;

import cc.fascinated.fascinatedutils.common.setting.impl.BooleanSetting;
import cc.fascinated.fascinatedutils.systems.modules.Module;
import cc.fascinated.fascinatedutils.systems.modules.ModuleCategory;
import cc.fascinated.fascinatedutils.systems.modules.impl.inventorytweaks.AutoSwapTool;
import cc.fascinated.fascinatedutils.systems.modules.impl.inventorytweaks.QuickMove;
import cc.fascinated.fascinatedutils.systems.modules.impl.inventorytweaks.ScrollMove;
import lombok.Getter;

@Getter
public class InventoryTweaksModule extends Module {

    private final BooleanSetting quickMove = BooleanSetting.builder().id("quick_move").defaultValue(true).build();
    private final BooleanSetting scrollMove = BooleanSetting.builder().id("scroll_move").defaultValue(true).build();
    private final BooleanSetting autoSwapNextTool = BooleanSetting.builder().id("auto_swap_next_tool").defaultValue(true).build();

    private final AutoSwapTool autoSwapToolFeature;
    private final QuickMove quickMoveFeature;
    private final ScrollMove scrollMoveFeature;

    public InventoryTweaksModule() {
        super("Inventory Tweaks", ModuleCategory.GENERAL);

        addSetting(quickMove);
        addSetting(scrollMove);
        addSetting(autoSwapNextTool);

        autoSwapToolFeature = new AutoSwapTool(this);
        quickMoveFeature = new QuickMove(this);
        scrollMoveFeature = new ScrollMove(this);
    }
}
