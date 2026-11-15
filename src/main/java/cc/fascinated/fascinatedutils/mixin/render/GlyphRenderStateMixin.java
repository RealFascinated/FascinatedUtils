package cc.fascinated.fascinatedutils.mixin.render;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.gui.font.TextRenderable;
import net.minecraft.client.renderer.state.gui.GlyphRenderState;
import org.joml.Matrix3x2fc;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GlyphRenderState.class)
public abstract class GlyphRenderStateMixin {
    // The glyph cache is per thread so concurrent buildVertices calls never share a scratch matrix.
    @Unique
    private static final ThreadLocal<Matrix4f> ALUMITE$LAST_MATRIX = ThreadLocal.withInitial(Matrix4f::new);
    // Last pose as m00, m01, m10, m11, m20, m21; NaN in the first slot marks an empty cache.
    @Unique
    private static final ThreadLocal<float[]> ALUMITE$LAST_POSE = ThreadLocal.withInitial(() -> new float[]{Float.NaN, 0f, 0f, 0f, 0f, 0f});

    @Shadow
    public abstract Matrix3x2fc pose();

    @Shadow
    public abstract TextRenderable renderable();

    // Glyphs of one text share a pose, so convert it to a 4x4 once and reuse it until the pose changes.
    @Inject(method = "buildVertices", at = @At("HEAD"), cancellable = true)
    private void alumite$buildWithSharedMatrix(VertexConsumer vertexConsumer, CallbackInfo info) {
        Matrix3x2fc pose = pose();
        Matrix4f matrix = ALUMITE$LAST_MATRIX.get();
        float[] lastPose = ALUMITE$LAST_POSE.get();
        if (pose.m00() != lastPose[0] || pose.m01() != lastPose[1] || pose.m10() != lastPose[2] || pose.m11() != lastPose[3] || pose.m20() != lastPose[4] || pose.m21() != lastPose[5]) {
            matrix.identity().mul(pose);
            lastPose[0] = pose.m00();
            lastPose[1] = pose.m01();
            lastPose[2] = pose.m10();
            lastPose[3] = pose.m11();
            lastPose[4] = pose.m20();
            lastPose[5] = pose.m21();
        }
        renderable().render(matrix, vertexConsumer, 15728880, true);
        info.cancel();
    }
}
