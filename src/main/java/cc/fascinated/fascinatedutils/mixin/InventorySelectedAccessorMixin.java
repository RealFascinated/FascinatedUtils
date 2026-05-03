package cc.fascinated.fascinatedutils.mixin;

import net.minecraft.world.entity.player.Inventory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Inventory.class)
public interface InventorySelectedAccessorMixin {

    @Accessor("selected")
    int fascinatedutils$getSelected();

    @Accessor("selected")
    void fascinatedutils$setSelected(int selected);
}
