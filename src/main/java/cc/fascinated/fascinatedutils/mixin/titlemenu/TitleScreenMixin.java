package cc.fascinated.fascinatedutils.mixin.titlemenu;

import cc.fascinated.fascinatedutils.systems.titlescreen.TitleScreenAddon;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.input.MouseButtonEvent;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(TitleScreen.class)
public class TitleScreenMixin {

    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void alumite$drawTitleMenuOverlay(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        TitleScreenAddon.INSTANCE.render(graphics);
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void alumite$onTitleMenuButtonClicked(MouseButtonEvent event, boolean doubled, CallbackInfoReturnable<Boolean> cir) {
        if (TitleScreenAddon.INSTANCE.mouseClicked(event)) {
            cir.setReturnValue(true);
        }
    }
}
