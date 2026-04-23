package cc.fascinated.fascinatedutils.mixin.zoom;

import cc.fascinated.fascinatedutils.event.FascinatedEventBus;
import cc.fascinated.fascinatedutils.event.impl.render.ClientFovEvent;
import net.minecraft.client.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Camera.class)
public class CameraMixin {

    @Inject(method = "calculateFov", at = @At("RETURN"), cancellable = true)
    private void fascinatedutils$postClientFov(float partialTicks, CallbackInfoReturnable<Float> cir) {
        Camera camera = (Camera) (Object) this;
        float vanillaFov = cir.getReturnValue();
        ClientFovEvent event = new ClientFovEvent(camera, partialTicks, true, vanillaFov);
        FascinatedEventBus.INSTANCE.post(event);
        cir.setReturnValue(event.fovDegrees());
    }
}
