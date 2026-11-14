package cc.fascinated.fascinatedutils.debug.perf.tests.text;

import cc.fascinated.fascinatedutils.debug.perf.core.PerfTest;
import cc.fascinated.fascinatedutils.debug.perf.coverage.PerfCoverage;
import cc.fascinated.fascinatedutils.systems.modules.impl.ItemTooltipModule;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * ItemStack.getTooltipLines for the enchanted sword the inventory scene gives, with the item tooltip module's size
 * line (encoded size, cached per stack) appended by its mixin.
 */
public class ItemTooltipPerfTest extends PerfTest {
    private ItemStack stack;
    private Item.TooltipContext tooltipContext;
    private List<Component> lastResult;

    public ItemTooltipPerfTest() {
        super("itemtooltip.lines", "getTooltipLines on an enchanted sword, including the size-cache mixin", 50, 5_000);
    }

    @Override
    public void setup() {
        Minecraft minecraft = Minecraft.getInstance();
        ItemStack held = minecraft.player.getInventory().getItem(0);
        stack = held.isEmpty() ? new ItemStack(Items.DIAMOND_SWORD) : held.copy();
        tooltipContext = Item.TooltipContext.of(minecraft.level);
    }

    @Override
    public void runOnce() {
        lastResult = stack.getTooltipLines(tooltipContext, Minecraft.getInstance().player, TooltipFlag.NORMAL);
    }

    @Override
    public List<String> sceneIds() {
        return List.of("inventory");
    }

    @Override
    public List<String> covers() {
        return List.of(PerfCoverage.mixin("itemtooltip.ItemStackTooltipMixin"), PerfCoverage.module(ItemTooltipModule.class), PerfCoverage.common("LRUCache"), PerfCoverage.common("ByteUtils"));
    }
}
