package cc.fascinated.fascinatedutils.mixin.turboentities;

import cc.fascinated.fascinatedutils.client.Client;
import cc.fascinated.fascinatedutils.common.culling.Cullable;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer.CrumblingOverlay;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockEntityRenderDispatcher.class)
public abstract class BlockEntityRenderDispatcherMixin {

    @Shadow
    public abstract <E extends BlockEntity> BlockEntityRenderer<? super E, ?> getRenderer(E blockEntity);

    @Inject(method = "tryExtractRenderState", at = @At("HEAD"), cancellable = true)
    private void fascinatedutils$tryExtractRenderState(BlockEntity blockEntity, float partialTicks, CrumblingOverlay breakProgress, CallbackInfoReturnable<BlockEntityRenderState> info) {
        if (!Client.TURBO_ENTITIES.isTurboEntitiesCullEnabled()) {
            return;
        }

        BlockEntityRenderer<?, ?> blockEntityRenderer = getRenderer(blockEntity);
        if (blockEntityRenderer == null) {
            return;
        }

        if (!(blockEntity instanceof Cullable cullable)) {
            return;
        }

        if (cullable.fascinatedutils$isCulled()) {
            if (blockEntityRenderer.shouldRenderOffScreen()) {
                return;
            }
            info.setReturnValue(null);
            return;
        }

        if (cullable.fascinatedutils$isForcedVisible()) {
            return;
        }

        cullable.fascinatedutils$setOutOfCamera(false);
    }
}
