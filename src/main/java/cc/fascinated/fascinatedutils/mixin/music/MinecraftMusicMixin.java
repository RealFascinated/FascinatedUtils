package cc.fascinated.fascinatedutils.mixin.music;

import cc.fascinated.fascinatedutils.systems.modules.ModuleRegistry;
import cc.fascinated.fascinatedutils.systems.modules.impl.music.MusicModule;
import cc.fascinated.fascinatedutils.systems.modules.impl.music.feature.LegacyMusicFeature;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.sounds.Music;
import net.minecraft.sounds.Musics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Minecraft.class)
public class MinecraftMusicMixin {

    @WrapOperation(
            method = "updateLevelInEngines(Lnet/minecraft/client/multiplayer/ClientLevel;Z)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/sounds/SoundManager;stop()V")
    )
    private void fascinatedutils$continueMusicAcrossLevelEngineTeardown(
            SoundManager soundManager,
            Operation<Void> original,
            ClientLevel newLevel,
            boolean stopSounds
    ) {
        ModuleRegistry.INSTANCE.getModule(MusicModule.class).ifPresentOrElse(
                module -> module.applyUpdateLevelEnginesSoundStop(soundManager, newLevel, () -> original.call(soundManager)),
                () -> original.call(soundManager));
    }

    @Inject(method = "getSituationalMusic", at = @At("RETURN"), cancellable = true)
    private void fascinatedutils$legacyMusicChannels(CallbackInfoReturnable<Music> cir) {
        ModuleRegistry.INSTANCE.getModule(MusicModule.class).ifPresent(module -> {
            LegacyMusicFeature legacyMusic = module.getLegacyMusicFeature();
            if (!legacyMusic.isFilterActive()) {
                return;
            }
            Music music = cir.getReturnValue();
            if (music == null) {
                return;
            }
            if (legacyMusic.isVanillaGlobalMusicChannel(music.sound())) {
                return;
            }
            cir.setReturnValue(Musics.GAME);
        });
    }
}
