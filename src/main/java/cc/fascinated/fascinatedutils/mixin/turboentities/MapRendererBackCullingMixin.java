package cc.fascinated.fascinatedutils.mixin.turboentities;

import cc.fascinated.fascinatedutils.client.Client;
import cc.fascinated.fascinatedutils.settings.SettingsRegistry;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MapRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.MapRenderState;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MapRenderer.class)
public class MapRendererBackCullingMixin {

    @Inject(method = "render*", at = @At("HEAD"), cancellable = true)
    private void fascinatedutils$cullBack(MapRenderState mapRenderState, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, boolean showOnlyFrame, int lightCoords, CallbackInfo info) {
        if (!SettingsRegistry.INSTANCE.getSettings().getTurboEntities().isEnabled()) {
            return;
        }

        if (!showOnlyFrame) {
            return;
        }

        // Camera
        var mc = Minecraft.getInstance();
        var cameraEntity = mc.getCameraEntity();
        if (cameraEntity == null) {
            return;
        }

        double camX = cameraEntity.getX();
        double camY = cameraEntity.getY();
        double camZ = cameraEntity.getZ();

        // Use the pose stack's top matrix to compute world normal and center
        PoseStack.Pose top = poseStack.last();

        Vector3f worldNormal = top.transformNormal(0f, 0f, 1f, new Vector3f());
        Matrix4f mat = top.pose();
        Vector4f center = new Vector4f(64f, 64f, 0f, 1f);
        mat.transform(center);

        double centerX = center.x();
        double centerY = center.y();
        double centerZ = center.z();

        double toCamX = camX - centerX;
        double toCamY = camY - centerY;
        double toCamZ = camZ - centerZ;

        double dot = toCamX * worldNormal.x() + toCamY * worldNormal.y() + toCamZ * worldNormal.z();

        // Reuse item-frame counters to account for map quads (they are displayed in frames)
        Client.TURBO_ENTITIES.itemFrameCounters.considered++;
        if (dot < 0) {
            Client.TURBO_ENTITIES.itemFrameCounters.culled++;
            info.cancel();
        }
    }
}
