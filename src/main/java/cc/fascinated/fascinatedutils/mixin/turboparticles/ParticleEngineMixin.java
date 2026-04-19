package cc.fascinated.fascinatedutils.mixin.turboparticles;

import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.renderer.state.level.ParticlesRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import cc.fascinated.fascinatedutils.client.Client;
import cc.fascinated.fascinatedutils.settings.SettingsRegistry;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.culling.Frustum;

@Mixin(ParticleEngine.class)
public class ParticleEngineMixin {

    @Inject(method = "extract", at = @At("HEAD"))
    private void fascinatedutils$onExtractHead(ParticlesRenderState particlesRenderState, Frustum frustum, Camera camera, float partialTickTime, CallbackInfo ci) {
        if (!SettingsRegistry.INSTANCE.getSettings().getTurboParticles().isEnabled()) {
            return;
        }

        Client.TURBO_PARTICLES.frameSnapshot(camera, frustum);
        Client.TURBO_PARTICLES.startExtractTimer();
    }

    @Inject(method = "extract", at = @At("RETURN"))
    private void fascinatedutils$onExtractReturn(ParticlesRenderState particlesRenderState, Frustum frustum, Camera camera, float partialTickTime, CallbackInfo ci) {
        if (!SettingsRegistry.INSTANCE.getSettings().getTurboParticles().isEnabled()) {
            return;
        }

        Client.TURBO_PARTICLES.stopExtractTimer();
    }
}
