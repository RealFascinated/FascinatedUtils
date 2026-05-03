package cc.fascinated.fascinatedutils.mixin.music;

import cc.fascinated.fascinatedutils.systems.modules.ModuleRegistry;
import cc.fascinated.fascinatedutils.systems.modules.impl.music.MusicModule;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.MusicManager;
import net.minecraft.client.sounds.SoundManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Consumer;

@Mixin(MusicManager.class)
public class MusicManagerMixin {

    @Shadow
    private SoundInstance currentMusic;

    @Inject(method = "tick", at = @At("HEAD"))
    private void fascinatedutils$clearDisconnectRetainWhenInWorld(CallbackInfo callbackInfo) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }
        withMusicModule(module -> {
            if (module.isDisconnectMusicRetainActive()) {
                module.clearDisconnectMusicRetain();
            }
        });
    }

    @Inject(method = "stopPlaying()V", at = @At("HEAD"), cancellable = true)
    private void fascinatedutils$retainMusicSkipStopPlaying(CallbackInfo callbackInfo) {
        Minecraft minecraft = Minecraft.getInstance();
        withMusicModule(module -> {
            if (!module.shouldSuppressStoppingCurrentMusicTrack(minecraft)) {
                return;
            }
            SoundInstance currentTrack = this.currentMusic;
            if (currentTrack != null && minecraft.getSoundManager().isActive(currentTrack)) {
                callbackInfo.cancel();
            }
        });
    }

    @WrapOperation(
            method = "tick",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/sounds/SoundManager;stop(Lnet/minecraft/client/resources/sounds/SoundInstance;)V")
    )
    private void fascinatedutils$retainBackgroundMusicDuringDisconnect(
            SoundManager soundManager,
            SoundInstance soundInstance,
            Operation<Void> original
    ) {
        Minecraft minecraft = Minecraft.getInstance();
        ModuleRegistry.INSTANCE.getModule(MusicModule.class).ifPresentOrElse(module -> {
            if (!module.shouldSuppressStoppingCurrentMusicTrack(minecraft)) {
                original.call(soundManager, soundInstance);
            }
        }, () -> original.call(soundManager, soundInstance));
    }

    private static void withMusicModule(Consumer<MusicModule> consumer) {
        ModuleRegistry.INSTANCE.getModule(MusicModule.class).ifPresent(consumer);
    }
}
