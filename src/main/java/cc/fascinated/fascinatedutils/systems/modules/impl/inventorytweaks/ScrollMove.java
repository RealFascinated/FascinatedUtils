package cc.fascinated.fascinatedutils.systems.modules.impl.inventorytweaks;

import cc.fascinated.fascinatedutils.systems.modules.Feature;
import cc.fascinated.fascinatedutils.systems.modules.impl.InventoryTweaksModule;
import net.minecraft.client.Minecraft;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class ScrollMove extends Feature<InventoryTweaksModule> {

    public ScrollMove(InventoryTweaksModule module) {
        super(module);
    }

    public boolean isEnabled() {
        return getModule().isEnabled() && getModule().getScrollMove().isEnabled();
    }

    /**
     * Moves items for {@code ticks} scroll steps. Negative ticks push the hovered item to the
     * other inventory; positive ticks pull a matching item from the other inventory.
     *
     * @param ticks       accumulated scroll ticks (negative = push, positive = pull)
     * @param hoveredSlot the slot currently under the cursor
     * @param menu        the active container menu
     * @param clicker     callback to perform slot clicks on the screen
     */
    public void scroll(int ticks, Slot hoveredSlot, AbstractContainerMenu menu, SlotClickAction clicker) {
        boolean push = ticks < 0;
        int count = Math.abs(ticks);
        for (int i = 0; i < count; i++) {
            if (push) {
                if (!hoveredSlot.hasItem()) break;
                moveOne(hoveredSlot, true, menu, clicker);
            } else {
                if (!hoveredSlot.hasItem()) break;
                boolean hoveredInPlayerInv = hoveredSlot.container == Minecraft.getInstance().player.getInventory();
                Slot source = findMatchingSource(!hoveredInPlayerInv, hoveredSlot.getItem(), menu);
                if (source == null) break;
                moveOne(source, true, menu, clicker);
            }
        }
    }

    private void moveOne(Slot source, boolean toOtherSide, AbstractContainerMenu menu, SlotClickAction clicker) {
        ItemStack sourceStack = source.getItem();
        if (sourceStack.isEmpty()) return;

        boolean sourceInPlayerInv = source.container == Minecraft.getInstance().player.getInventory();
        boolean targetInPlayerInv = toOtherSide != sourceInPlayerInv;

        Slot target = findTarget(targetInPlayerInv, sourceStack, menu);
        if (target == null) return;

        int sourceId = menu.slots.indexOf(source);
        int targetId = menu.slots.indexOf(target);

        clicker.click(source, sourceId, 0, ContainerInput.PICKUP);
        clicker.click(target, targetId, 1, ContainerInput.PICKUP);
        if (!Minecraft.getInstance().player.containerMenu.getCarried().isEmpty()) {
            clicker.click(source, sourceId, 0, ContainerInput.PICKUP);
        }
    }

    private Slot findTarget(boolean inPlayerInv, ItemStack item, AbstractContainerMenu menu) {
        Slot emptyFallback = null;
        for (Slot slot : menu.slots) {
            boolean slotInPlayerInv = slot.container == Minecraft.getInstance().player.getInventory();
            if (slotInPlayerInv != inPlayerInv) continue;
            if (slot.hasItem()) {
                ItemStack existing = slot.getItem();
                if (ItemStack.isSameItemSameComponents(existing, item)
                        && existing.getCount() < slot.getMaxStackSize(existing)) {
                    return slot;
                }
            } else if (emptyFallback == null && slot.mayPlace(item)) {
                emptyFallback = slot;
            }
        }
        return emptyFallback;
    }

    private Slot findMatchingSource(boolean inPlayerInv, ItemStack filter, AbstractContainerMenu menu) {
        for (Slot slot : menu.slots) {
            boolean slotInPlayerInv = slot.container == Minecraft.getInstance().player.getInventory();
            if (slotInPlayerInv != inPlayerInv) continue;
            if (slot.hasItem() && ItemStack.isSameItemSameComponents(slot.getItem(), filter)) {
                return slot;
            }
        }
        return null;
    }
}
