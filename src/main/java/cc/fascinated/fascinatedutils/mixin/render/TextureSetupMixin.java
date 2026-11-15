package cc.fascinated.fascinatedutils.mixin.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.textures.GpuTextureView;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.render.TextureSetup;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(TextureSetup.class)
public class TextureSetupMixin {
    @Unique
    private static TextureSetup alumite$lastSetup;

    // Every glyph of a text asks for the same setup, so hand back the previous one instead of building it again.
    @Inject(method = "singleTextureWithLightmap", at = @At("HEAD"), cancellable = true)
    private static void alumite$reuseLastSetup(GpuTextureView texture, GpuSampler sampler, CallbackInfoReturnable<TextureSetup> info) {
        TextureSetup last = alumite$lastSetup;
        if (last != null && last.texure0() == texture && last.sampler0() == sampler && last.texure2() == Minecraft.getInstance().gameRenderer.lightmap() && last.sampler2() == RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR)) {
            info.setReturnValue(last);
        }
    }

    @Inject(method = "singleTextureWithLightmap", at = @At("RETURN"))
    private static void alumite$rememberSetup(GpuTextureView texture, GpuSampler sampler, CallbackInfoReturnable<TextureSetup> info) {
        alumite$lastSetup = info.getReturnValue();
    }
}
