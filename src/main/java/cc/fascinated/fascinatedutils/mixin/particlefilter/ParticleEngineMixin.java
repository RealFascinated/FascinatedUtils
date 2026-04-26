package cc.fascinated.fascinatedutils.mixin.particlefilter;

import cc.fascinated.fascinatedutils.systems.modules.ModuleRegistry;
import cc.fascinated.fascinatedutils.systems.modules.impl.ParticleFilterModule;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
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
            Identifier key = BuiltInRegistries.PARTICLE_TYPE.getKey(options.getType());
            if (key == null) {
                return;
            }
            String id = key.getNamespace() + ":" + key.getPath();
            module.getParticleToggles().stream()
                    .filter(toggle -> toggle.getNameProvider().get().equals(id))
                    .findFirst()
                    .ifPresent(toggle -> {
                        if (!toggle.getValue()) {
                            cir.setReturnValue(null);
                        }
                    });
        });
    }
}
