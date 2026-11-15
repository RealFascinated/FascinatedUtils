package cc.fascinated.fascinatedutils.mixin;

import cc.fascinated.fascinatedutils.renderer.RoundedRectCornerRadiiTexture;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import net.minecraft.client.gui.render.GuiRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiRenderer.class)
public abstract class GuiRendererMixin {

    /**
     * Trims the cached corner-radius LUTs once this render pass has finished sampling them.
     *
     * @param fogBuffer the fog buffer passed to {@code GuiRenderer#render}
     * @param callbackInfo the injection callback info
     */
    @Inject(method = "render(Lcom/mojang/blaze3d/buffers/GpuBufferSlice;)V", at = @At("TAIL"))
    private void alumite$releaseRoundRectDisposableLuts(GpuBufferSlice fogBuffer, CallbackInfo callbackInfo) {
        RoundedRectCornerRadiiTexture.trimCachedLuts();
    }
}
