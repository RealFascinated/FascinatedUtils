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
    @Unique
    private static final Matrix4f ALUMITE$LAST_MATRIX = new Matrix4f();
    @Unique
    private static float alumite$lastM00 = Float.NaN;
    @Unique
    private static float alumite$lastM01;
    @Unique
    private static float alumite$lastM10;
    @Unique
    private static float alumite$lastM11;
    @Unique
    private static float alumite$lastM20;
    @Unique
    private static float alumite$lastM21;

    @Shadow
    public abstract Matrix3x2fc pose();

    @Shadow
    public abstract TextRenderable renderable();

    // Glyphs of one text share a pose, so convert it to a 4x4 once and reuse it until the pose changes.
    @Inject(method = "buildVertices", at = @At("HEAD"), cancellable = true)
    private void alumite$buildWithSharedMatrix(VertexConsumer vertexConsumer, CallbackInfo info) {
        Matrix3x2fc pose = pose();
        if (pose.m00() != alumite$lastM00 || pose.m01() != alumite$lastM01 || pose.m10() != alumite$lastM10 || pose.m11() != alumite$lastM11 || pose.m20() != alumite$lastM20 || pose.m21() != alumite$lastM21) {
            ALUMITE$LAST_MATRIX.identity().mul(pose);
            alumite$lastM00 = pose.m00();
            alumite$lastM01 = pose.m01();
            alumite$lastM10 = pose.m10();
            alumite$lastM11 = pose.m11();
            alumite$lastM20 = pose.m20();
            alumite$lastM21 = pose.m21();
        }
        renderable().render(ALUMITE$LAST_MATRIX, vertexConsumer, 15728880, true);
        info.cancel();
    }
}
