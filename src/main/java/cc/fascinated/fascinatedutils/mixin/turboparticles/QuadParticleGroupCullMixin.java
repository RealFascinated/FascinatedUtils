package cc.fascinated.fascinatedutils.mixin.turboparticles;

import cc.fascinated.fascinatedutils.client.Client;
import cc.fascinated.fascinatedutils.systems.turboparticles.CulledParticleIterator;
import net.minecraft.client.particle.QuadParticleGroup;
import net.minecraft.client.particle.SingleQuadParticle;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import java.util.Iterator;
import java.util.Queue;

@Mixin(QuadParticleGroup.class)
public class QuadParticleGroupCullMixin {

    // The group frustum-tests every particle before extract can reject a culled one, so filter them out of the loop
    // instead: a culled particle then costs neither the point test nor the extract call.
    @WrapOperation(method = "extractRenderState", at = @At(value = "INVOKE", target = "Ljava/util/Queue;iterator()Ljava/util/Iterator;"))
    private Iterator<SingleQuadParticle> alumite$skipCulledParticles(Queue<SingleQuadParticle> particles, Operation<Iterator<SingleQuadParticle>> original) {
        Iterator<SingleQuadParticle> iterator = original.call(particles);
        if (!Client.TURBO_PARTICLES.isEnabled()) {
            return iterator;
        }
        return new CulledParticleIterator(iterator);
    }
}
