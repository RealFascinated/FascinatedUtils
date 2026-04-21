package cc.fascinated.fascinatedutils.systems.modules.impl;

import cc.fascinated.fascinatedutils.common.color.SettingColor;
import cc.fascinated.fascinatedutils.common.setting.impl.BooleanSetting;
import cc.fascinated.fascinatedutils.common.setting.impl.ColorSetting;
import cc.fascinated.fascinatedutils.systems.modules.Module;
import cc.fascinated.fascinatedutils.systems.modules.ModuleCategory;
import lombok.Getter;

@Getter
public class BlockOutlineModule extends Module {

    private final ColorSetting outlineColor = ColorSetting.builder()
            .id("outline_color")
            .defaultValue(new SettingColor(255, 255, 255, 255))
            .build();

    private final BooleanSetting showOutline = BooleanSetting.builder()
            .id("show_outline")
            .defaultValue(true)
            .build();

    private final ColorSetting blockColor = ColorSetting.builder()
            .id("block_color")
            .defaultValue(new SettingColor(255, 255, 255, 80))
            .build();

    private final BooleanSetting showBlockColor = BooleanSetting.builder()
            .id("show_block_color")
            .defaultValue(false)
            .build();

    public BlockOutlineModule() {
        super("Block Outline", ModuleCategory.GENERAL);
        showOutline.addSubSetting(outlineColor);
        showBlockColor.addSubSetting(blockColor);
        addSetting(showOutline);
        addSetting(showBlockColor);
    }
}
