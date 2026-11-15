package cc.fascinated.fascinatedutils.mixin.render;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.SubmitNodeCollection;
import net.minecraft.client.renderer.block.BlockStateModelSet;
import net.minecraft.client.renderer.feature.BlockFeatureRenderer;
import net.minecraft.client.renderer.state.OptionsRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BlockFeatureRenderer.class)
public class BlockFeatureRendererMixin {

    // Runs four times a frame (solid and translucent, world and held item). With no moving blocks it only builds a
    // block renderer, which Sodium swaps for a context holding a fresh hash map, so skip it when there is nothing to draw.
    @Inject(method = "renderMovingBlockSubmits", at = @At("HEAD"), cancellable = true)
    private void alumite$skipWhenNoMovingBlocks(SubmitNodeCollection nodeCollection, MultiBufferSource.BufferSource bufferSource, BlockStateModelSet blockStateModelSet, OptionsRenderState optionsState, boolean translucent, CallbackInfo info) {
        if (nodeCollection.getMovingBlockSubmits().isEmpty()) {
            info.cancel();
        }
    }
}
