package cc.fascinated.fascinatedutils.systems.modules.impl.itemtooltip;

import cc.fascinated.fascinatedutils.common.setting.impl.BooleanSetting;
import cc.fascinated.fascinatedutils.systems.modules.Module;
import cc.fascinated.fascinatedutils.systems.modules.ModuleCategory;
import cc.fascinated.fascinatedutils.common.util.ByteUtils;
import cc.fascinated.fascinatedutils.common.item.ItemStackSizeCalculator;
import lombok.Getter;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.List;

@Getter
public class ItemTooltipModule extends Module {

    private final BooleanSetting gapAboveInfo = BooleanSetting.builder().id("gap_above_info").defaultValue(true).build();

    private final BooleanSetting showItemSize = BooleanSetting.builder().id("show_item_size").defaultValue(true).build();

    public ItemTooltipModule() {
        super("Item Tooltip", ModuleCategory.GENERAL);
        addSetting(gapAboveInfo);
        addSetting(showItemSize);
    }

    /**
     * Builds the item size tooltip lines for a stack: an optional blank gap followed by the size label.
     *
     * @param stack the stack being described
     * @return the lines to append, empty when item size is disabled or the stack has no measurable size
     */
    public List<Component> itemSizeLines(ItemStack stack) {
        if (!showItemSize.isEnabled()) {
            return List.of();
        }
        long byteSize = ItemStackSizeCalculator.INSTANCE.getStackSize(stack);
        if (byteSize <= 0) {
            return List.of();
        }
        Component sizeLine = Component.literal(I18n.get("alumite.module.itemtooltip.item_size", ByteUtils.formatBytes(byteSize, 2))).withStyle(style -> style.withColor(0xAAAAAA));
        if (gapAboveInfo.isEnabled()) {
            return List.of(Component.empty(), sizeLine);
        }
        return List.of(sizeLine);
    }
}
