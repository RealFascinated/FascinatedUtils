package cc.fascinated.fascinatedutils.mixin;

import cc.fascinated.fascinatedutils.renderer.MeshRenderer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import net.minecraft.client.gui.render.GuiRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiRenderer.class)
public abstract class GuiRendererMixin {

    /**
     * Per-LUT {@link net.minecraft.client.renderer.texture.DynamicTexture} instances are uploaded while queueing mesh
     * work; release only after this pass has sampled them. Preparation runs earlier for all simple elements, so
     * mutating one shared LUT during {@code prepareSimpleElement} cannot match per-draw sampling.
     */
    @Inject(method = "render(Lcom/mojang/blaze3d/buffers/GpuBufferSlice;)V", at = @At("TAIL"))
    private void fascinatedutils$releaseRoundRectDisposableLuts(GpuBufferSlice fogBuffer, CallbackInfo callbackInfo) {
        MeshRenderer.INSTANCE.releaseDisposableRadiiLutsAfterGuiRenderPass();
    }
}
