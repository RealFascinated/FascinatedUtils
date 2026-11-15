package cc.fascinated.fascinatedutils.mixin.itemtooltip;

import cc.fascinated.fascinatedutils.systems.modules.ModuleRegistry;
import cc.fascinated.fascinatedutils.systems.modules.impl.ItemTooltipModule;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;

@Mixin(ItemStack.class)
public class ItemStackTooltipMixin {

    @Inject(method = "getTooltipLines", at = @At("RETURN"), cancellable = true)
    private void alumite$appendItemTooltip(CallbackInfoReturnable<List<Component>> cir) {
        ItemTooltipModule module = ModuleRegistry.INSTANCE.getModule(ItemTooltipModule.class).orElse(null);
        if (module == null || !module.isEnabled()) {
            return;
        }

        ItemStack stack = (ItemStack) (Object) this;

        List<Component> sizeLines = module.itemSizeLines(stack);
        if (!sizeLines.isEmpty()) {
            List<Component> mutable = new ArrayList<>(cir.getReturnValue());
            mutable.addAll(sizeLines);
            cir.setReturnValue(mutable);
        }
    }
}
