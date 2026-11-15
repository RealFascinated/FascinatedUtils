package cc.fascinated.fascinatedutils.mixin.blockoutline;

import cc.fascinated.fascinatedutils.debug.perf.core.PerfProbe;
import cc.fascinated.fascinatedutils.renderer.FascinatedWorldRenderTypes;
import cc.fascinated.fascinatedutils.renderer.WorldBoxFaces;
import cc.fascinated.fascinatedutils.systems.modules.ModuleRegistry;
import cc.fascinated.fascinatedutils.systems.modules.impl.BlockOutlineModule;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
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

    @Unique
    private static final PerfProbe.Section ALUMITE$RENDER = PerfProbe.INSTANCE.section("blockoutline.render");

    @Unique
    private static int alumite$resolveColor(int defaultColor) {
        Optional<BlockOutlineModule> module = ModuleRegistry.INSTANCE.getModule(BlockOutlineModule.class);
        if (module.isPresent() && module.get().isEnabled() && module.get().getShowOutline().isEnabled()) {
            return module.get().getOutlineColor().getResolvedArgb();
        }
        return defaultColor;
    }

    @ModifyArg(method = "renderBlockOutline", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/LevelRenderer;renderHitOutline(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;DDDLnet/minecraft/client/renderer/state/level/BlockOutlineRenderState;IF)V", ordinal = 0), index = 6)
    private int alumite$modifyOutlineColorFirst(int color) {
        return alumite$resolveColor(color);
    }

    @ModifyArg(method = "renderBlockOutline", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/LevelRenderer;renderHitOutline(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;DDDLnet/minecraft/client/renderer/state/level/BlockOutlineRenderState;IF)V", ordinal = 1), index = 6)
    private int alumite$modifyOutlineColorSecond(int color) {
        return alumite$resolveColor(color);
    }

    @Inject(method = "renderBlockOutline", at = @At("TAIL"))
    private void alumite$renderFullBlock(MultiBufferSource.BufferSource bufferSource, PoseStack poseStack, boolean isTranslucent, LevelRenderState levelRenderState, CallbackInfo ci) {
        long start = ALUMITE$RENDER.begin();
        try {
            Optional<BlockOutlineModule> opt = ModuleRegistry.INSTANCE.getModule(BlockOutlineModule.class);
            if (opt.isEmpty() || !opt.get().isEnabled() || !opt.get().getShowBlockColor().isEnabled()) {
                return;
            }

            BlockOutlineRenderState outlineState = levelRenderState.blockOutlineRenderState;
            if (outlineState == null || outlineState.isTranslucent() != isTranslucent) {
                return;
            }

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

            VertexConsumer consumer = bufferSource.getBuffer(FascinatedWorldRenderTypes.WORLD_BEAM);
            PoseStack.Pose pose = poseStack.last();
            WorldBoxFaces.emitBoxFaces(consumer, pose, minX, minY, minZ, maxX, maxY, maxZ, argb, true);

            bufferSource.endBatch(FascinatedWorldRenderTypes.WORLD_BEAM);
        } finally {
            ALUMITE$RENDER.end(start);
        }
    }
}
