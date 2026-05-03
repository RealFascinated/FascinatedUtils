package cc.fascinated.fascinatedutils.systems.modules.impl.inventorytweaks;

import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;

@FunctionalInterface
public interface SlotClickAction {

    void click(Slot slot, int slotId, int mouseButton, ContainerInput type);
}
