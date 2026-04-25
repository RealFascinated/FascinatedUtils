package cc.fascinated.fascinatedutils.mixin.blur;

import cc.fascinated.fascinatedutils.systems.modules.ModuleRegistry;
import cc.fascinated.fascinatedutils.systems.modules.impl.BlurModule;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Screen.class)
public abstract class ScreenMixin {

    @Inject(method = "extractBlurredBackground", at = @At("HEAD"))
    private void fascinatedutils$onBlurBackground(GuiGraphicsExtractor graphics, CallbackInfo ci) {
        BlurModule module = ModuleRegistry.INSTANCE.getModule(BlurModule.class).orElse(null);
        if (module == null || !module.isEnabled()) {
            return;
        }
        if (module.isDisabledScreen(((Screen) (Object) this).getClass().getName())) {
            return;
        }
        module.onBlurDetected();
    }

    @Inject(method = "extractTransparentBackground", at = @At("HEAD"))
    private void fascinatedutils$onTransparentBackground(GuiGraphicsExtractor graphics, CallbackInfo ci) {
        BlurModule module = ModuleRegistry.INSTANCE.getModule(BlurModule.class).orElse(null);
        if (module == null || !module.isEnabled()) {
            return;
        }
        if (module.isDisabledScreen(((Screen) (Object) this).getClass().getName())) {
            return;
        }
        module.onBlurDetected();
        if (!module.isBlurApplied() && Math.round(module.getBlurStrength().getValue().floatValue() * module.getProgress()) >= 1) {
            graphics.blurBeforeThisStratum();
            module.markBlurApplied();
        }
    }
}
