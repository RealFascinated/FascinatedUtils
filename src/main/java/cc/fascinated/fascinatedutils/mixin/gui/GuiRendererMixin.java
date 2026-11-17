package cc.fascinated.fascinatedutils.mixin.gui;

import cc.fascinated.fascinatedutils.mixininterface.GuiItemRenderStateAlpha;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.gui.render.GuiRenderer;
import net.minecraft.client.renderer.state.gui.GuiItemRenderState;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(GuiRenderer.class)
public abstract class GuiRendererMixin {

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
