package cc.fascinated.fascinatedutils.mixin.blockoutline;

import cc.fascinated.fascinatedutils.systems.modules.ModuleRegistry;
import cc.fascinated.fascinatedutils.systems.modules.impl.BlockOutlineModule;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.BlockOutlineRenderState;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Optional;

@Mixin(LevelRenderer.class)
public class BlockOutlineMixin {

    @ModifyArg(
            method = "renderBlockOutline",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/LevelRenderer;renderHitOutline(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;DDDLnet/minecraft/client/renderer/state/level/BlockOutlineRenderState;IF)V",
                    ordinal = 0
            ),
            index = 6)
    private int fascinatedutils$modifyOutlineColorFirst(int color) {
        return resolveColor(color);
    }

    @ModifyArg(
            method = "renderBlockOutline",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/LevelRenderer;renderHitOutline(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;DDDLnet/minecraft/client/renderer/state/level/BlockOutlineRenderState;IF)V",
                    ordinal = 1
            ),
            index = 6)
    private int fascinatedutils$modifyOutlineColorSecond(int color) {
        return resolveColor(color);
    }

    @Inject(method = "renderBlockOutline", at = @At("TAIL"))
    private void fascinatedutils$renderFullBlock(
            MultiBufferSource.BufferSource bufferSource,
            PoseStack poseStack,
            boolean isTranslucent,
            LevelRenderState levelRenderState,
            CallbackInfo ci) {
        Optional<BlockOutlineModule> opt = ModuleRegistry.INSTANCE.getModule(BlockOutlineModule.class);
        if (opt.isEmpty() || !opt.get().isEnabled() || !opt.get().getShowBlockColor().isEnabled()) return;

        BlockOutlineRenderState outlineState = levelRenderState.blockOutlineRenderState;
        if (outlineState == null || outlineState.isTranslucent() != isTranslucent) return;

        BlockOutlineModule module = opt.get();
        Vec3 cameraPos = levelRenderState.cameraRenderState.pos;
        BlockPos blockPos = outlineState.pos();
        AABB bounds = outlineState.shape().bounds();

        float minX = (float) (bounds.minX + blockPos.getX() - cameraPos.x);
        float minY = (float) (bounds.minY + blockPos.getY() - cameraPos.y);
        float minZ = (float) (bounds.minZ + blockPos.getZ() - cameraPos.z);
        float maxX = (float) (bounds.maxX + blockPos.getX() - cameraPos.x);
        float maxY = (float) (bounds.maxY + blockPos.getY() - cameraPos.y);
        float maxZ = (float) (bounds.maxZ + blockPos.getZ() - cameraPos.z);

        int argb = module.getBlockColor().getResolvedArgb();
        int red = (argb >> 16) & 0xFF;
        int green = (argb >> 8) & 0xFF;
        int blue = argb & 0xFF;
        int alpha = (argb >> 24) & 0xFF;

        VertexConsumer consumer = bufferSource.getBuffer(RenderTypes.debugFilledBox());
        PoseStack.Pose pose = poseStack.last();

        // -Y (bottom)
        consumer.addVertex(pose, minX, minY, minZ).setColor(red, green, blue, alpha);
        consumer.addVertex(pose, maxX, minY, minZ).setColor(red, green, blue, alpha);
        consumer.addVertex(pose, maxX, minY, maxZ).setColor(red, green, blue, alpha);
        consumer.addVertex(pose, minX, minY, maxZ).setColor(red, green, blue, alpha);
        // +Y (top)
        consumer.addVertex(pose, minX, maxY, minZ).setColor(red, green, blue, alpha);
        consumer.addVertex(pose, minX, maxY, maxZ).setColor(red, green, blue, alpha);
        consumer.addVertex(pose, maxX, maxY, maxZ).setColor(red, green, blue, alpha);
        consumer.addVertex(pose, maxX, maxY, minZ).setColor(red, green, blue, alpha);
        // -Z (north)
        consumer.addVertex(pose, minX, minY, minZ).setColor(red, green, blue, alpha);
        consumer.addVertex(pose, minX, maxY, minZ).setColor(red, green, blue, alpha);
        consumer.addVertex(pose, maxX, maxY, minZ).setColor(red, green, blue, alpha);
        consumer.addVertex(pose, maxX, minY, minZ).setColor(red, green, blue, alpha);
        // +Z (south)
        consumer.addVertex(pose, minX, minY, maxZ).setColor(red, green, blue, alpha);
        consumer.addVertex(pose, maxX, minY, maxZ).setColor(red, green, blue, alpha);
        consumer.addVertex(pose, maxX, maxY, maxZ).setColor(red, green, blue, alpha);
        consumer.addVertex(pose, minX, maxY, maxZ).setColor(red, green, blue, alpha);
        // -X (west)
        consumer.addVertex(pose, minX, minY, minZ).setColor(red, green, blue, alpha);
        consumer.addVertex(pose, minX, minY, maxZ).setColor(red, green, blue, alpha);
        consumer.addVertex(pose, minX, maxY, maxZ).setColor(red, green, blue, alpha);
        consumer.addVertex(pose, minX, maxY, minZ).setColor(red, green, blue, alpha);
        // +X (east)
        consumer.addVertex(pose, maxX, minY, minZ).setColor(red, green, blue, alpha);
        consumer.addVertex(pose, maxX, maxY, minZ).setColor(red, green, blue, alpha);
        consumer.addVertex(pose, maxX, maxY, maxZ).setColor(red, green, blue, alpha);
        consumer.addVertex(pose, maxX, minY, maxZ).setColor(red, green, blue, alpha);

        bufferSource.endLastBatch();
    }

    @Unique
    private static int resolveColor(int defaultColor) {
        Optional<BlockOutlineModule> module = ModuleRegistry.INSTANCE.getModule(BlockOutlineModule.class);
        if (module.isPresent() && module.get().isEnabled() && module.get().getShowOutline().isEnabled()) {
            return module.get().getOutlineColor().getResolvedArgb();
        }
        return defaultColor;
    }
}
