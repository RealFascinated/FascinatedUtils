package cc.fascinated.fascinatedutils.mixin.turboparticles;

import cc.fascinated.fascinatedutils.client.Client;
import cc.fascinated.fascinatedutils.common.culling.Cullable;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.SingleQuadParticle;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SingleQuadParticle.class)
public abstract class SingleQuadParticleMixin {

    @Inject(method = "extract", at = @At("HEAD"), cancellable = true)
    private void alumite$earlyCull(CallbackInfo ci) {
        if (!Client.TURBO_PARTICLES.isEnabled()) {
            return;
        }

        Particle particle = (Particle) (Object) this;
        if (particle instanceof Cullable cullable && cullable.alumite$isCulled()) {
            ci.cancel();
        }
    }
}
