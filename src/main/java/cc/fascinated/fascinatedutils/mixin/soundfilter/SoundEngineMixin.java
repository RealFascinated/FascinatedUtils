package cc.fascinated.fascinatedutils.mixin.soundfilter;

import cc.fascinated.fascinatedutils.systems.modules.ModuleRegistry;
import cc.fascinated.fascinatedutils.systems.modules.impl.SoundFilterModule;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SoundEngine.class)
public class SoundEngineMixin {

    @Inject(method = "play", at = @At("HEAD"), cancellable = true)
    private void onPlay(SoundInstance instance, CallbackInfoReturnable<SoundEngine.PlayResult> cir) {
        ModuleRegistry.INSTANCE.getModule(SoundFilterModule.class).ifPresent(module -> {
            String id = instance.getIdentifier().getNamespace() + ":" + instance.getIdentifier().getPath();
            module.getSoundToggles().stream()
                    .filter(toggle -> toggle.getNameProvider().get().equals(id))
                    .findFirst()
                    .ifPresent(toggle -> {
                        if (!toggle.getValue()) {
                            cir.setReturnValue(SoundEngine.PlayResult.NOT_STARTED);
                        }
                    });
        });
    }
}