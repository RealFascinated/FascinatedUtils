package cc.fascinated.fascinatedutils.mixin.turboentities;

import cc.fascinated.fascinatedutils.client.Client;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Transformation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.AbstractSignRenderer;
import net.minecraft.client.renderer.blockentity.state.SignRenderState;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractSignRenderer.class)
public class SignBackCullingMixin {

    @Inject(method = "submitSignText", at = @At("HEAD"), cancellable = true)
    private void fascinatedutils$cullSignText(SignRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, SignText signText, CallbackInfo info) {
        if (!Client.TURBO_ENTITIES.isTurboEntitiesCullEnabled()) {
            return;
        }

        // Pick the transformation for whichever face this text belongs to
        Transformation faceTransform = signText == state.frontText
                ? state.transformations.frontText()
                : state.transformations.backText();

        // Derive face normal: local +Z transformed into world space
        Vector4f normal = new Vector4f(0f, 0f, 1f, 0f);
        Matrix4f matrix = faceTransform.getMatrixCopy();
        matrix.transform(normal);

        // Vector from sign center to camera
        Vec3 cameraPos = Minecraft.getInstance().gameRenderer.getMainCamera().position();
        double toCameraX = cameraPos.x - state.blockPos.getX() - 0.5;
        double toCameraY = cameraPos.y - state.blockPos.getY() - 0.5;
        double toCameraZ = cameraPos.z - state.blockPos.getZ() - 0.5;

        double dot = toCameraX * normal.x() + toCameraY * normal.y() + toCameraZ * normal.z();
        Client.TURBO_ENTITIES.signCounters.considered++;
        if (dot < 0) {
            Client.TURBO_ENTITIES.signCounters.culled++;
            info.cancel();
        }
    }
}
