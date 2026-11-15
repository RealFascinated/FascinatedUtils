package cc.fascinated.fascinatedutils.mixin.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.geom.ModelPart;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.Map;

@Mixin(ModelPart.class)
public abstract class ModelPartChildrenMixin {
    @Shadow
    private List<ModelPart.Cube> cubes;
    @Shadow
    private Map<String, ModelPart> children;
    @Shadow
    public boolean visible;
    @Shadow
    public boolean skipDraw;
    @Unique
    private ModelPart[] alumite$childArray;

    @Shadow
    public abstract void translateAndRotate(PoseStack poseStack);

    // Vanilla builds a fresh iterator for every part's cubes and children, for every entity, every frame. The baked
    // lists and maps never change, so index the cubes and walk a cached child array instead.
    @Inject(method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;III)V", at = @At("HEAD"), cancellable = true)
    private void alumite$renderWithCachedChildren(PoseStack poseStack, VertexConsumer buffer, int lightCoords, int overlayCoords, int color, CallbackInfo info) {
        info.cancel();
        if (!this.visible || this.cubes.isEmpty() && this.children.isEmpty()) {
            return;
        }

        ModelPart[] childArray = this.alumite$childArray;
        if (childArray == null) {
            childArray = this.children.values().toArray(new ModelPart[0]);
            this.alumite$childArray = childArray;
        }

        poseStack.pushPose();
        this.translateAndRotate(poseStack);
        if (!this.skipDraw) {
            PoseStack.Pose pose = poseStack.last();
            for (int cubeIndex = 0; cubeIndex < this.cubes.size(); cubeIndex++) {
                this.cubes.get(cubeIndex).compile(pose, buffer, lightCoords, overlayCoords, color);
            }
        }
        for (ModelPart child : childArray) {
            child.render(poseStack, buffer, lightCoords, overlayCoords, color);
        }
        poseStack.popPose();
    }
}
