package cc.fascinated.fascinatedutils.mixin.turboentities;

import cc.fascinated.fascinatedutils.client.Client;
import cc.fascinated.fascinatedutils.common.culling.Cullable;
import cc.fascinated.fascinatedutils.systems.turboentities.EntitiesCullTask;
import cc.fascinated.fascinatedutils.systems.turboentities.TurboEntities;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LevelRenderer.class)
public class LevelRendererMixin {
    // Shared by every culled entity: nothing writes to a state after extraction, and an invisible interaction draws nothing.
    @Unique
    private static final EntityRenderState ALUMITE$INVISIBLE_STATE = alumite$createInvisibleState();

    @Inject(method = "extractEntity", at = @At("HEAD"), cancellable = true)
    private void alumite$extractEntity(Entity entity, float partialTickTime, CallbackInfoReturnable<EntityRenderState> info) {
        if (!Client.TURBO_ENTITIES.isEnabled()) {
            return;
        }

        Double maxDistSq = TurboEntities.RENDER_DISTANCE_CAPS.get(entity.getType());
        if (maxDistSq != null) {
            Minecraft client = Minecraft.getInstance();
            Vec3 cameraPos = client.gameRenderer.getMainCamera().position();
            if (entity.distanceToSqr(cameraPos.x, cameraPos.y, cameraPos.z) > maxDistSq) {
                info.setReturnValue(ALUMITE$INVISIBLE_STATE);
                return;
            }
        }

        if (!(entity instanceof Cullable cullable)) {
            return;
        }

        if (cullable.alumite$isCulled()) {
            info.setReturnValue(ALUMITE$INVISIBLE_STATE);
        }
    }

    @Inject(method = "extractVisibleEntities", at = @At("HEAD"))
    private void alumite$extractVisibleEntities(Camera camera, Frustum frustum, DeltaTracker deltaTracker, LevelRenderState output, CallbackInfo info) {
        if (!Client.TURBO_ENTITIES.isEnabled()) {
            return;
        }

        TurboEntities module = Client.TURBO_ENTITIES;
        EntitiesCullTask cullTask = module.getCullTask();
        if (cullTask == null) {
            return;
        }

        if (!cullTask.handOffFrame(camera, frustum)) {
            return;
        }

        module.snapshotAndResetRenderFrameCounters();
    }

    @Unique
    private static EntityRenderState alumite$createInvisibleState() {
        EntityRenderState state = new EntityRenderState();
        state.entityType = EntityType.INTERACTION;
        state.isInvisible = true;
        return state;
    }
}
