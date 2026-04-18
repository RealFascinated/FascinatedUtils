package cc.fascinated.fascinatedutils.mixin.turboentities;

import cc.fascinated.fascinatedutils.client.Client;
import cc.fascinated.fascinatedutils.settings.SettingsRegistry;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.PaintingRenderer;
import net.minecraft.client.renderer.entity.state.PaintingRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PaintingRenderer.class)
public class PaintingBackCullingMixin {

    @Inject(method = "submit*", at = @At("HEAD"), cancellable = true)
    private void fascinatedutils$cullBack(PaintingRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState camera, CallbackInfo info) {
        if (!SettingsRegistry.INSTANCE.getSettings().getTurboEntities().isEnabled()) {
            return;
        }

        Client.TURBO_ENTITIES.paintingCounters.considered++;

        Direction direction = state.direction;
        double toCameraX = camera.pos.x - state.x;
        double toCameraY = camera.pos.y - state.y;
        double toCameraZ = camera.pos.z - state.z;
        double dot = toCameraX * direction.getStepX() + toCameraY * direction.getStepY() + toCameraZ * direction.getStepZ();

        if (dot < 0) {
            Client.TURBO_ENTITIES.paintingCounters.culled++;
            info.cancel();
        }
    }
}
