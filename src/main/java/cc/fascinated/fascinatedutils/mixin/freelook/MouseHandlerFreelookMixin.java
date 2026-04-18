package cc.fascinated.fascinatedutils.mixin.freelook;

import cc.fascinated.fascinatedutils.systems.modules.Module;
import cc.fascinated.fascinatedutils.systems.modules.ModuleRegistry;
import cc.fascinated.fascinatedutils.systems.modules.impl.FreelookModule;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(MouseHandler.class)
public abstract class MouseHandlerFreelookMixin {

    @ModifyArgs(method = "turnPlayer", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;turn(DD)V"))
    private void fascinatedutils$redirectTurnForFreelook(Args args) {
        FreelookModule module = ModuleRegistry.INSTANCE.getModule(FreelookModule.class).filter(Module::isEnabled).filter(FreelookModule::isFreelookActive).orElse(null);
        if (module == null) {
            return;
        }
        double dx = args.get(0);
        double dy = args.get(1);
        module.addMouseDelta(dx, dy);
        args.set(0, 0.0);
        args.set(1, 0.0);
    }
}
