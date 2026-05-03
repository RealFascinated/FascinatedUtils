package cc.fascinated.fascinatedutils.mixin.music;

import cc.fascinated.fascinatedutils.systems.modules.ModuleRegistry;
import cc.fascinated.fascinatedutils.systems.modules.impl.music.MusicModule;
import cc.fascinated.fascinatedutils.systems.modules.impl.music.feature.LegacyMusicFeature;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SoundEngine.class)
public class MusicSoundEngineMixin {

    @Inject(
            method = "play",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/resources/sounds/SoundInstance;getSound()Lnet/minecraft/client/resources/sounds/Sound;",
                    shift = At.Shift.AFTER
            ),
            cancellable = true
    )
    private void fascinatedutils$legacyMusicStreams(SoundInstance instance, CallbackInfoReturnable<SoundEngine.PlayResult> cir) {
        ModuleRegistry.INSTANCE.getModule(MusicModule.class).ifPresent(module -> {
            LegacyMusicFeature legacyMusic = module.getLegacyMusicFeature();
            if (legacyMusic.blocksNonLegacyMusicPlayback(instance)) {
                cir.setReturnValue(SoundEngine.PlayResult.NOT_STARTED);
                cir.cancel();
            }
        });
    }
}
