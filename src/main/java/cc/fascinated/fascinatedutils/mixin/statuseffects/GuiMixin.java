package cc.fascinated.fascinatedutils.mixin.statuseffects;

import cc.fascinated.fascinatedutils.systems.modules.ModuleRegistry;
import cc.fascinated.fascinatedutils.systems.modules.impl.statuseffects.StatusEffectsModule;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Optional;

@Mixin(Gui.class)
public class GuiMixin {

    @Inject(method = "extractEffects", at = @At(value = "HEAD"), cancellable = true)
    private void fascinatedutils$hideStatusEffects(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker, CallbackInfo callbackInfo) {
        Optional<StatusEffectsModule> statusEffectsOptional = ModuleRegistry.INSTANCE.getModule(StatusEffectsModule.class);
        if (statusEffectsOptional.isEmpty()) {
            return;
        }
        StatusEffectsModule renderingModule = statusEffectsOptional.get();
        if (renderingModule.isEnabled()) {
            callbackInfo.cancel();
        }
    }
}