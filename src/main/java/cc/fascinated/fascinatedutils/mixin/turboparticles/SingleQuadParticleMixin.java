package cc.fascinated.fascinatedutils.mixin.turboparticles;

import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.SingleQuadParticle;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import cc.fascinated.fascinatedutils.client.Client;
import cc.fascinated.fascinatedutils.settings.SettingsRegistry;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.world.phys.AABB;

/**
 * Conservative early-out for single-quad particles. Opt-in only; non-invasive.
 */
@Mixin(SingleQuadParticle.class)
public abstract class SingleQuadParticleMixin {

    @Inject(method = "extract", at = @At("HEAD"), cancellable = true)
    private void fascinatedutils$earlyCull(CallbackInfo ci) {
        if (!SettingsRegistry.INSTANCE.getSettings().getTurboParticles().isEnabled()) {
            return;
        }
        if (!Client.TURBO_PARTICLES.isEnabled()) {
            return;
        }

        // Visibility check (frustum + small AABB), conservative like TurboEntities' frustum step
        Frustum fr = Client.TURBO_PARTICLES.getFrustum();
        if (fr != null) {
            AABB particleBB = ((Particle) (Object) this).getBoundingBox();
            double px = (particleBB.minX + particleBB.maxX) / 2.0;
            double py = particleBB.minY;
            double pz = (particleBB.minZ + particleBB.maxZ) / 2.0;
            AABB box = new AABB(px - 0.125, py - 0.125, pz - 0.125, px + 0.125, py + 0.125, pz + 0.125);
            if (!fr.isVisible(box)) {
                Client.TURBO_PARTICLES.incrementConsidered(true);
                ci.cancel();
                return;
            }

            // Per-particle occlusion check using the turbo particles occlusion instance
            try {
                if (!Client.TURBO_PARTICLES.isVisible(box)) {
                    Client.TURBO_PARTICLES.incrementConsidered(true);
                    ci.cancel();
                    return;
                }
            } catch (Exception ignored) {
            }
        }

        Client.TURBO_PARTICLES.incrementConsidered(false);
    }
}
