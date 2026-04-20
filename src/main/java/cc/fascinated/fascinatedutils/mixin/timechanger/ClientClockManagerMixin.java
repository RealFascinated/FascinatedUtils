package cc.fascinated.fascinatedutils.mixin.timechanger;

import cc.fascinated.fascinatedutils.systems.modules.ModuleRegistry;
import cc.fascinated.fascinatedutils.systems.modules.impl.TimeChangerModule;
import net.minecraft.client.ClientClockManager;
import net.minecraft.core.Holder;
import net.minecraft.world.clock.WorldClock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientClockManager.class)
public class ClientClockManagerMixin {

    @Shadow
    private long lastTickGameTime;

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void onTick(long gameTime, CallbackInfo ci) {
        ModuleRegistry.INSTANCE.getModule(TimeChangerModule.class).ifPresent(timeChangerModule -> {
            if (timeChangerModule.isEnabled()) {
                this.lastTickGameTime = gameTime;
                ci.cancel();
            }
        });
    }

    @Inject(method = "getTotalTicks", at = @At("HEAD"), cancellable = true)
    private void onGetTotalTicks(Holder<WorldClock> definition, CallbackInfoReturnable<Long> cir) {
        ModuleRegistry.INSTANCE.getModule(TimeChangerModule.class).ifPresent(timeChangerModule -> {
            if (timeChangerModule.isEnabled()) {
                cir.setReturnValue(timeChangerModule.getWorldTime().getValue().longValue());
            }
        });
    }
}
