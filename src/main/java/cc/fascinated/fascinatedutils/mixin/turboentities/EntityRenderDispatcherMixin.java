package cc.fascinated.fascinatedutils.mixin.turboentities;

import cc.fascinated.fascinatedutils.client.Client;
import cc.fascinated.fascinatedutils.common.culling.Cullable;
import cc.fascinated.fascinatedutils.systems.turboentities.TurboEntities;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EntityRenderDispatcher.class)
public class EntityRenderDispatcherMixin {

    // Vanilla asks this once per entity before extracting it. Answering false for a culled entity skips the frustum test,
    // the section lookup, the render state extraction and the submit that would otherwise run for something that draws nothing.
    // Entities on their first tick are left alone so vanilla still seeds their previous position.
    @Inject(method = "shouldRender", at = @At("HEAD"), cancellable = true)
    private <E extends Entity> void alumite$skipCulledEntity(E entity, Frustum culler, double camX, double camY, double camZ, CallbackInfoReturnable<Boolean> info) {
        if (!Client.TURBO_ENTITIES.isEnabled() || entity.tickCount == 0) {
            return;
        }

        Double maxDistSq = TurboEntities.getRenderDistanceCapSq(entity);
        if (maxDistSq != null && entity.distanceToSqr(camX, camY, camZ) > maxDistSq) {
            info.setReturnValue(false);
            return;
        }

        if (entity instanceof Cullable cullable && cullable.alumite$isCulled()) {
            info.setReturnValue(false);
        }
    }
}
