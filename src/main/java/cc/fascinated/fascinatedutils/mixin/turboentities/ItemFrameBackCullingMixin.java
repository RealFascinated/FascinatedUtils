package cc.fascinated.fascinatedutils.mixin.turboentities;

import cc.fascinated.fascinatedutils.client.Client;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.ItemFrameRenderer;
import net.minecraft.client.renderer.entity.state.ItemFrameRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemFrameRenderer.class)
public class ItemFrameBackCullingMixin {
    @Unique
    private static final double FACING_EPSILON = 0.0;

    @Inject(method = "submit*", at = @At("HEAD"), cancellable = true)
    private void alumite$cullBack(ItemFrameRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState camera, CallbackInfo info) {
        if (!Client.TURBO_ENTITIES.isEnabled()) {
            return;
        }

        Direction direction = state.direction;
        double toCameraX = camera.pos.x - state.x;
        double toCameraY = camera.pos.y - state.y;
        double toCameraZ = camera.pos.z - state.z;
        double dot = toCameraX * direction.getStepX() + toCameraY * direction.getStepY() + toCameraZ * direction.getStepZ();

        if (Client.TURBO_ENTITIES.itemFrameCounters.cullIfFacingAway(dot, FACING_EPSILON)) {
            info.cancel();
        }
    }
}
