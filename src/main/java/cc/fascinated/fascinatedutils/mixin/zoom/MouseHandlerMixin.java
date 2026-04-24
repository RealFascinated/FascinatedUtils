package cc.fascinated.fascinatedutils.mixin.zoom;

import cc.fascinated.fascinatedutils.systems.modules.Module;
import cc.fascinated.fascinatedutils.systems.modules.ModuleRegistry;
import cc.fascinated.fascinatedutils.systems.modules.impl.ZoomModule;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(MouseHandler.class)
public abstract class MouseHandlerMixin {

    @ModifyArgs(method = "turnPlayer", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;turn(DD)V"))
    private void fascinatedutils$scaleMouseLookWhileZoomed(Args args) {
        float factor = ModuleRegistry.INSTANCE.getModule(ZoomModule.class).filter(Module::isEnabled).map(ZoomModule::zoomMouseLookScale).orElse(1f);
        if (factor >= 1f - 1e-4f) {
            return;
        }
        args.set(0, (Double) args.get(0) * factor);
        args.set(1, (Double) args.get(1) * factor);
    }
}
