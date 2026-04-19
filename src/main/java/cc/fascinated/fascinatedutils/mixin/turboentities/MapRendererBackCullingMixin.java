package cc.fascinated.fascinatedutils.mixin.turboentities;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mojang.blaze3d.vertex.PoseStack;

import cc.fascinated.fascinatedutils.client.Client;
import net.minecraft.client.renderer.MapRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.MapRenderState;

@Mixin(MapRenderer.class)
public class MapRendererBackCullingMixin {

    @Inject(method = "render*", at = @At("HEAD"), cancellable = true)
    private void fascinatedutils$cullBack(MapRenderState mapRenderState, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, boolean showOnlyFrame, int lightCoords, CallbackInfo info) {
        if (!Client.TURBO_ENTITIES.isTurboEntitiesCullEnabled()) {
            return;
        }

        if (!showOnlyFrame) {
            return;
        }

        PoseStack.Pose top = poseStack.last();
        Matrix4f matrix = top.pose();

        float mapRelX = matrix.m30();
        float mapRelY = matrix.m31();
        float mapRelZ = matrix.m32();

        Vector3f worldNormal = top.transformNormal(0f, 0f, 1f, new Vector3f());

        float dot = (mapRelX * worldNormal.x()) + (mapRelY * worldNormal.y()) + (mapRelZ * worldNormal.z());

        Client.TURBO_ENTITIES.itemFrameCounters.considered++;

        if (dot < -0.01f) {
            Client.TURBO_ENTITIES.itemFrameCounters.culled++;
            info.cancel();
        }
    }
}