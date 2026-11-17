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
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractSignRenderer.class)
public class SignBackCullingMixin {
    @Unique
    private static final double FACING_EPSILON = 0.0;

    // Sign text is submitted on the render thread only, so one scratch vector avoids an allocation per sign face.
    @Unique
    private static final Vector4f NORMAL_SCRATCH = new Vector4f();

    @Inject(method = "submitSignText", at = @At("HEAD"), cancellable = true)
    private void alumite$cullSignText(SignRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, SignText signText, CallbackInfo info) {
        if (!Client.TURBO_ENTITIES.isEnabled()) {
            return;
        }

        Transformation faceTransform = signText == state.frontText ? state.transformations.frontText() : state.transformations.backText();

        Vector4f normal = NORMAL_SCRATCH.set(0f, 0f, 1f, 0f);
        faceTransform.getMatrix().transform(normal);

        Vec3 cameraPos = Minecraft.getInstance().gameRenderer.getMainCamera().position();
        double toCameraX = cameraPos.x - state.blockPos.getX() - 0.5;
        double toCameraY = cameraPos.y - state.blockPos.getY() - 0.5;
        double toCameraZ = cameraPos.z - state.blockPos.getZ() - 0.5;

        double dot = toCameraX * normal.x() + toCameraY * normal.y() + toCameraZ * normal.z();
        if (Client.TURBO_ENTITIES.signCounters.cullIfFacingAway(dot, FACING_EPSILON)) {
            info.cancel();
        }
    }
}
