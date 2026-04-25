package cc.fascinated.fascinatedutils.mixin.blur;

import cc.fascinated.fascinatedutils.systems.modules.ModuleRegistry;
import cc.fascinated.fascinatedutils.systems.modules.impl.BlurModule;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {

    @Inject(method = "extract", at = @At("HEAD"))
    private void fascinatedutils$blurFrameBegin(DeltaTracker deltaTracker, boolean advanceGameTime, CallbackInfo ci) {
        BlurModule module = ModuleRegistry.INSTANCE.getModule(BlurModule.class).orElse(null);
        if (module != null) {
            module.beginFrame();
        }
    }

    @Inject(method = "extract", at = @At("TAIL"))
    private void fascinatedutils$blurFrameEnd(DeltaTracker deltaTracker, boolean advanceGameTime, CallbackInfo ci) {
        BlurModule module = ModuleRegistry.INSTANCE.getModule(BlurModule.class).orElse(null);
        if (module != null) {
            module.advanceAnimation(deltaTracker.getRealtimeDeltaTicks() / 20f);
        }
    }
}
