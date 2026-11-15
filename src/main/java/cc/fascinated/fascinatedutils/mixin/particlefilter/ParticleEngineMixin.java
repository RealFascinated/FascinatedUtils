package cc.fascinated.fascinatedutils.mixin.particlefilter;

import cc.fascinated.fascinatedutils.systems.modules.ModuleRegistry;
import cc.fascinated.fascinatedutils.systems.modules.impl.ParticleFilterModule;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.core.particles.ParticleOptions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ParticleEngine.class)
public class ParticleEngineMixin {

    @Unique
    private ParticleFilterModule alumite$particleFilter;

    @Inject(method = "createParticle", at = @At("HEAD"), cancellable = true)
    private void alumite$onCreateParticle(ParticleOptions options, double x, double y, double z, double vx, double vy, double vz, CallbackInfoReturnable<Particle> cir) {
        if (alumite$particleFilter == null) {
            alumite$particleFilter = ModuleRegistry.INSTANCE.getModule(ParticleFilterModule.class).orElse(null);
        }
        if (alumite$particleFilter == null || !alumite$particleFilter.isEnabled()) {
            return;
        }
        if (alumite$particleFilter.isParticleBlocked(options)) {
            cir.setReturnValue(null);
        }
    }
}
