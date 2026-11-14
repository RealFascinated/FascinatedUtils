package cc.fascinated.fascinatedutils.mixin.turboparticles;

import cc.fascinated.fascinatedutils.client.Client;
import cc.fascinated.fascinatedutils.systems.turboparticles.ParticleCullTask;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
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

        Client.TURBO_PARTICLES.frameSnapshot();
        ParticleCullTask cullTask = Client.TURBO_PARTICLES.getParticleCullTask();
        if (cullTask == null) {
            return;
        }

        Minecraft client = Minecraft.getInstance();
        if (client.level == null || client.player == null) {
            cullTask.setInGame(false);
            return;
        }

        cullTask.setInGame(true);
        cullTask.setCamera(camera.position());
        // Hand the cull thread a private copy; the render thread keeps mutating the live frustum.
        cullTask.setFrustum(new Frustum(frustum));

        Map<ParticleRenderType, ParticleGroup<?>> map = ((ParticleEngineAccessor) this).getParticles();
        cullTask.publishParticleCullSnapshotFromGroups(map.values());

        cullTask.requestCull();
    }
}