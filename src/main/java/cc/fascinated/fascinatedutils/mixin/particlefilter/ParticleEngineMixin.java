package cc.fascinated.fascinatedutils.mixin.particlefilter;

import cc.fascinated.fascinatedutils.systems.modules.ModuleRegistry;
import cc.fascinated.fascinatedutils.systems.modules.impl.ParticleFilterModule;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.core.particles.ParticleOptions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ParticleEngine.class)
public class ParticleEngineMixin {

    @Inject(method = "createParticle", at = @At("HEAD"), cancellable = true)
    private void onCreateParticle(ParticleOptions options, double x, double y, double z, double vx, double vy, double vz, CallbackInfoReturnable<Particle> cir) {
        ModuleRegistry.INSTANCE.getModule(ParticleFilterModule.class).ifPresent(module -> {
            if (!module.isEnabled()) {
                return;
            }
            if (module.isParticleBlocked(options)) {
                cir.setReturnValue(null);
            }
        });
    }
}
