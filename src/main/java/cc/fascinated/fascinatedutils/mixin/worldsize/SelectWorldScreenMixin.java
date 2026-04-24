package cc.fascinated.fascinatedutils.mixin.worldsize;

import cc.fascinated.fascinatedutils.systems.modules.ModuleRegistry;
import cc.fascinated.fascinatedutils.systems.modules.impl.WorldSizeModule;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SelectWorldScreen.class)
public class SelectWorldScreenMixin {

    @Inject(method = "init", at = @At("TAIL"))
    private void fascinatedutils$refreshWorldSizes(CallbackInfo ci) {
        ModuleRegistry.INSTANCE.getModule(WorldSizeModule.class).ifPresent(WorldSizeModule::refreshSizes);
    }
}
