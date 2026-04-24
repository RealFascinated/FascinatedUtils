package cc.fascinated.fascinatedutils.mixin.worldsize;

import cc.fascinated.fascinatedutils.systems.modules.ModuleRegistry;
import cc.fascinated.fascinatedutils.systems.modules.impl.WorldSizeModule;
import net.minecraft.client.gui.screens.worldselection.WorldSelectionList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(WorldSelectionList.class)
public class WorldSelectionListMixin {

    private static final int SIZE_EXTRA_WIDTH = 40;

    @Inject(method = "getRowWidth", at = @At("RETURN"), cancellable = true)
    private void fascinatedutils$expandRowWidthForSize(CallbackInfoReturnable<Integer> cir) {
        WorldSizeModule module = ModuleRegistry.INSTANCE.getModule(WorldSizeModule.class).orElse(null);
        if (module != null && module.isEnabled()) {
            cir.setReturnValue(cir.getReturnValue() + SIZE_EXTRA_WIDTH);
        }
    }
}
