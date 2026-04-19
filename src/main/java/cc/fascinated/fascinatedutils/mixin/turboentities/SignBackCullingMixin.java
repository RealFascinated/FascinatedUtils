package cc.fascinated.fascinatedutils.mixin.turboentities;

import cc.fascinated.fascinatedutils.client.Client;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Transformation;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.AbstractSignRenderer;
import net.minecraft.client.renderer.blockentity.state.SignRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractSignRenderer.class)
public class SignBackCullingMixin {

    @Inject(method = "submit*", at = @At("HEAD"), cancellable = true)
    private void fascinatedutils$cullBack(SignRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState camera, CallbackInfo info) {
        if (!Client.TURBO_ENTITIES.isTurboEntitiesCullEnabled()) {
            return;
        }

        // Compute vector from sign center to camera
        double toCameraX = camera.pos.x - state.blockPos.getX() - 0.5;
        double toCameraY = camera.pos.y - state.blockPos.getY() - 0.5;
        double toCameraZ = camera.pos.z - state.blockPos.getZ() - 0.5;

        // Derive sign forward vector from the frontText transformation
        Transformation t = state.transformations.frontText();
        // transformation's matrix maps local sign space to world; forward is local +Z (0,0,1)
        Vector4f v = new Vector4f(0f, 0f, 1f, 0f);
        Matrix4f m = t.getMatrixCopy();
        m.transform(v);
        double fx = v.x();
        double fy = v.y();
        double fz = v.z();

        double dot = toCameraX * fx + toCameraY * fy + toCameraZ * fz;
        Client.TURBO_ENTITIES.signCounters.considered++;
        if (dot < 0) {
            Client.TURBO_ENTITIES.signCounters.culled++;
            info.cancel();
        }
    }
}
