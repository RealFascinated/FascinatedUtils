package cc.fascinated.fascinatedutils.mixin.hurtcam;

import cc.fascinated.fascinatedutils.mixininterface.IGameRenderer;
import cc.fascinated.fascinatedutils.systems.modules.ModuleRegistry;
import cc.fascinated.fascinatedutils.systems.modules.impl.HurtcamModule;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Optional;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin implements IGameRenderer {

    @Inject(method = "bobHurt", at = @At("HEAD"), cancellable = true, require = 0)
    private void fascinatedutils$cancelHurtcamTilt(CameraRenderState cameraState, PoseStack poseStack, CallbackInfo callbackInfo) {
        Optional<HurtcamModule> module = ModuleRegistry.INSTANCE.getModule(HurtcamModule.class);
        if (module.isPresent()) {
            HurtcamModule hurtcamModule = module.get();
            if (hurtcamModule.isEnabled() && hurtcamModule.getCancelHurtcamAnimation().isEnabled()) {
                callbackInfo.cancel();
            }
        }
    }
}
