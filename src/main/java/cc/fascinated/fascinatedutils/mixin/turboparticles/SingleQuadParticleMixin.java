package cc.fascinated.fascinatedutils.mixin.turboparticles;

import cc.fascinated.fascinatedutils.client.Client;
import cc.fascinated.fascinatedutils.common.culling.Cullable;
import cc.fascinated.fascinatedutils.settings.SettingsRegistry;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.SingleQuadParticle;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Skips extract for particles marked culled by the background particle cull task.
 */
@Mixin(SingleQuadParticle.class)
public abstract class SingleQuadParticleMixin {

    @Inject(method = "extract", at = @At("HEAD"), cancellable = true)
    private void fascinatedutils$earlyCull(CallbackInfo ci) {
        if (!SettingsRegistry.INSTANCE.getSettings().getTurboParticles().isEnabled()) {
            return;
        }

        Particle particle = (Particle) (Object) this;
        if (!(particle instanceof Cullable cullable)) {
            return;
        }

        if (cullable.fascinatedutils$isCulled()) {
            Client.TURBO_PARTICLES.incrementConsidered(true);
            ci.cancel();
            return;
        }

        Client.TURBO_PARTICLES.incrementConsidered(false);
    }
}
