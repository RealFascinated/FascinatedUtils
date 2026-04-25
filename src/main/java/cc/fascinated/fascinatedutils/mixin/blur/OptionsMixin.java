package cc.fascinated.fascinatedutils.mixin.blur;

import cc.fascinated.fascinatedutils.systems.modules.ModuleRegistry;
import cc.fascinated.fascinatedutils.systems.modules.impl.BlurModule;
import net.minecraft.client.Options;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Options.class)
public abstract class OptionsMixin {

    @Inject(method = "getMenuBackgroundBlurriness", at = @At("HEAD"), cancellable = true)
    private void fascinatedutils$scaleBlurByProgress(CallbackInfoReturnable<Integer> cir) {
        BlurModule module = ModuleRegistry.INSTANCE.getModule(BlurModule.class).orElse(null);
        if (module != null && module.isEnabled()) {
            cir.setReturnValue(Math.round(module.getBlurStrength().getValue().floatValue() * module.getProgress()));
        }
    }
}
