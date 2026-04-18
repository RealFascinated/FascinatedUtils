package cc.fascinated.fascinatedutils.mixin;

import cc.fascinated.fascinatedutils.common.PlayerUtils;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public abstract class PlayerEntityMixin {

    @Inject(method = "getFlyingSpeed", at = @At("RETURN"), cancellable = true)
    private void fascinatedutils$modifyCreativeSprintFlightSpeed(CallbackInfoReturnable<Float> cir) {
        Player self = (Player) (Object) this;
        float vanilla = cir.getReturnValue();
        float scaled = PlayerUtils.scaleFlyingSpeed(self, vanilla);
        if (scaled != vanilla) {
            cir.setReturnValue(scaled);
        }
    }
}
