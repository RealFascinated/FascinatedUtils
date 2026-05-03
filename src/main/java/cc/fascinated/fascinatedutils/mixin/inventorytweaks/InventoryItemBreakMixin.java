package cc.fascinated.fascinatedutils.mixin.inventorytweaks;

import cc.fascinated.fascinatedutils.systems.modules.ModuleRegistry;
import cc.fascinated.fascinatedutils.systems.modules.impl.InventoryTweaksModule;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Inventory.class)
public abstract class InventoryItemBreakMixin {

    @Inject(method = "setItem", at = @At("HEAD"))
    private void fascinatedutils$onSetItem(int slot, ItemStack newStack, CallbackInfo ci) {
        Inventory inventory = (Inventory) (Object) this;
        if (!(inventory.player instanceof LocalPlayer localPlayer)) return;
        if (slot != inventory.getSelectedSlot()) return;

        ItemStack current = inventory.getItem(slot);
        if (current.isEmpty() || !newStack.isEmpty()) return;

        ModuleRegistry.INSTANCE.getModule(InventoryTweaksModule.class)
                .ifPresent(module -> module.getAutoSwapToolFeature().onToolBroke(localPlayer, current));
    }
}
