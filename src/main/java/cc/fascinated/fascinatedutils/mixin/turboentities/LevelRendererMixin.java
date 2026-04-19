package cc.fascinated.fascinatedutils.mixin.turboentities;

import cc.fascinated.fascinatedutils.client.Client;
import cc.fascinated.fascinatedutils.common.culling.Cullable;
import cc.fascinated.fascinatedutils.turboentities.EntitiesCullTask;
import cc.fascinated.fascinatedutils.turboentities.TurboEntities;
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
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LevelRenderer.class)
public class LevelRendererMixin {

    @Inject(method = "extractEntity", at = @At("HEAD"), cancellable = true)
    private void fascinatedutils$extractEntity(Entity entity, float partialTickTime, CallbackInfoReturnable<EntityRenderState> info) {
        if (!Client.TURBO_ENTITIES.isTurboEntitiesCullEnabled()) {
            return;
        }

        // Distance cap check — skip extracting render state entirely for far-away low-value entities
        Double maxDistSq = TurboEntities.RENDER_DISTANCE_CAPS.get(entity.getType());
        if (maxDistSq != null) {
            Minecraft client = Minecraft.getInstance();
            Vec3 cameraPos = client.gameRenderer.getMainCamera().position();
            if (entity.distanceToSqr(cameraPos.x, cameraPos.y, cameraPos.z) > maxDistSq) {
                EntityRenderState skipped = new EntityRenderState();
                skipped.entityType = EntityType.INTERACTION;
                skipped.isInvisible = true;
                info.setReturnValue(skipped);
                return;
            }
        }

        if (!(entity instanceof Cullable cullable)) {
            return;
        }

        if (cullable.fascinatedutils$isCulled()) {
            // Return an invisible state to skip rendering
            EntityRenderState state = new EntityRenderState();
            state.entityType = EntityType.INTERACTION;
            state.isInvisible = true;
            info.setReturnValue(state);
            return;
        }

        cullable.fascinatedutils$setOutOfCamera(false);
    }

    @Inject(method = "extractVisibleEntities", at = @At("HEAD"))
    private void fascinatedutils$extractVisibleEntities(Camera camera, Frustum frustum, DeltaTracker deltaTracker, LevelRenderState output, CallbackInfo info) {
        if (!Client.TURBO_ENTITIES.isTurboEntitiesCullEnabled()) {
            return;
        }

        TurboEntities module = Client.TURBO_ENTITIES;
        EntitiesCullTask cullTask = module.getCullTask();
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
        cullTask.setFrustum(frustum);

        module.snapshotAndResetRenderFrameCounters();

        cullTask.requestCull();
    }
}
