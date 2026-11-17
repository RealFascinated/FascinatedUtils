package cc.fascinated.fascinatedutils.mixin.turboparticles;

import cc.fascinated.fascinatedutils.client.Client;
import cc.fascinated.fascinatedutils.systems.turboparticles.ParticleCullTask;
import net.minecraft.client.Camera;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.particle.ParticleGroup;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.state.level.ParticlesRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;

@Mixin(ParticleEngine.class)
public class ParticleEngineMixin {

    @Inject(method = "extract", at = @At("HEAD"))
    private void alumite$onExtractHead(ParticlesRenderState particlesRenderState, Frustum frustum, Camera camera, float partialTickTime, CallbackInfo ci) {
        if (!Client.TURBO_PARTICLES.isEnabled()) {
            return;
        }

        ParticleCullTask cullTask = Client.TURBO_PARTICLES.getCullTask();
        if (cullTask == null) {
            return;
        }

        if (!cullTask.handOffFrame(camera, frustum)) {
            cullTask.clearSnapshots();
            return;
        }

        // Particles only move on client ticks, so a snapshot is published and a pass requested on the cadence set by
        // the module instead of every frame.
        if (Client.TURBO_PARTICLES.claimTickSnapshot()) {
            Map<ParticleRenderType, ParticleGroup<?>> map = ((ParticleEngineAccessor) this).getParticles();
            cullTask.publishParticleCullSnapshotFromGroups(map.values());
            cullTask.requestCull();
        }
    }
}