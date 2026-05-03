package cc.fascinated.fascinatedutils.systems.modules.impl.inventorytweaks;

import cc.fascinated.fascinatedutils.mixin.InventorySelectedAccessorMixin;
import cc.fascinated.fascinatedutils.systems.modules.Feature;
import cc.fascinated.fascinatedutils.systems.modules.impl.InventoryTweaksModule;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public class AutoSwapTool extends Feature<InventoryTweaksModule> {

    private static final List<TagKey<Item>> TOOL_TAGS = List.of(
            ItemTags.PICKAXES,
            ItemTags.AXES,
            ItemTags.SWORDS,
            ItemTags.SHOVELS,
            ItemTags.HOES
    );

    public AutoSwapTool(InventoryTweaksModule module) {
        super(module);
    }

    /**
     * Called by the mixin when a main-hand item stack reaches zero durability.
     *
     * @param player      the local player holding the broken tool
     * @param brokenTool  the now-empty item stack that just broke
     */
    public void onToolBroke(LocalPlayer player, ItemStack brokenTool) {
        if (getModule().getAutoSwapNextTool().isDisabled() || !getModule().isEnabled()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.screen != null) return;

        Inventory inventory = player.getInventory();
        InventorySelectedAccessorMixin accessor = (InventorySelectedAccessorMixin) inventory;
        int currentSlot = accessor.fascinatedutils$getSelected();

        for (int slot = 0; slot < 36; slot++) {
            if (slot == currentSlot) continue;
            ItemStack candidate = inventory.getItem(slot);
            if (candidate.isEmpty() || !isSameTool(candidate, brokenTool)) continue;

            if (slot < Inventory.getSelectionSize()) {
                accessor.fascinatedutils$setSelected(slot);
                player.connection.send(new ServerboundSetCarriedItemPacket(slot));
            } else {
                if (mc.gameMode != null) {
                    mc.gameMode.handleContainerInput(
                            player.inventoryMenu.containerId,
                            slot,
                            currentSlot,
                            ContainerInput.SWAP,
                            player);
                }
            }
            return;
        }
    }

    private static boolean isSameTool(ItemStack candidate, ItemStack brokenTool) {
        if (candidate.is(brokenTool.getItem())) {
            return true;
        }
        for (TagKey<Item> tag : TOOL_TAGS) {
            if (candidate.is(tag) && brokenTool.is(tag)) {
                return true;
            }
        }
        return false;
    }
}
