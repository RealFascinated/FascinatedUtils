package cc.fascinated.fascinatedutils.systems.modules.impl.inventorytweaks;

import cc.fascinated.fascinatedutils.systems.modules.Feature;
import cc.fascinated.fascinatedutils.systems.modules.impl.InventoryTweaksModule;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;

import java.util.Set;

public class QuickMove extends Feature<InventoryTweaksModule> {

    public QuickMove(InventoryTweaksModule module) {
        super(module);
    }

    public boolean isEnabled() {
        return getModule().isEnabled() && getModule().getQuickMove().isEnabled();
    }

    /**
     * Records a slot as visited during a shift-drag and fires a {@code QUICK_MOVE} click if
     * the slot has not been visited yet in this drag session.
     *
     * @param slot         the slot the drag cursor entered
     * @param slotId       index of the slot in the container menu
     * @param visitedSlots set of already-visited slot indices for this drag
     * @param clicker      callback to perform the actual slot click
     * @return {@code true} if the slot was newly visited and a click was sent
     */
    public boolean tryVisitSlot(Slot slot, int slotId, Set<Integer> visitedSlots, SlotClickAction clicker) {
        if (!visitedSlots.add(slotId)) return false;
        clicker.click(slot, slotId, 0, ContainerInput.QUICK_MOVE);
        return true;
    }
}
