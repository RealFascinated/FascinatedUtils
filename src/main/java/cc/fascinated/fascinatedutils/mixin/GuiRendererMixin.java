package cc.fascinated.fascinatedutils.mixin;

import cc.fascinated.fascinatedutils.mixininterface.GuiItemRenderStateAlpha;
import cc.fascinated.fascinatedutils.renderer.RoundedRectCornerRadiiTexture;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import net.minecraft.client.gui.render.GuiRenderer;
import net.minecraft.client.renderer.state.gui.GuiItemRenderState;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
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

    /**
     * Replaces the hardcoded white tint on item atlas blits with the alpha captured at submit time, so item icons fade
     * with the rest of the GUI. The blit pipeline is premultiplied, so every channel is scaled by the alpha.
     *
     * @param color     the vanilla tint (always opaque white)
     * @param itemState the item being blitted
     * @return the premultiplied tint for the item's alpha
     */
    @ModifyArg(method = "submitBlitFromItemAtlas", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/state/gui/BlitRenderState;<init>(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/client/gui/render/TextureSetup;Lorg/joml/Matrix3x2f;IIIIFFFFILnet/minecraft/client/gui/navigation/ScreenRectangle;Lnet/minecraft/client/gui/navigation/ScreenRectangle;)V"), index = 11)
    private int alumite$fadeItemAtlasBlit(int color, @Local(argsOnly = true) GuiItemRenderState itemState) {
        float alpha = ((GuiItemRenderStateAlpha) (Object) itemState).alumite$getAlpha();
        if (alpha >= 1f) {
            return color;
        }
        int channel = Mth.clamp(Mth.floor(alpha * 255f), 0, 255);
        return (channel << 24) | (channel << 16) | (channel << 8) | channel;
    }
}
