package cc.fascinated.fascinatedutils.mixin;

import cc.fascinated.fascinatedutils.common.PlayerUtils;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Abilities;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(LocalPlayer.class)
public abstract class ClientPlayerEntityMixin {
    @Redirect(method = "aiStep", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Abilities;getFlyingSpeed()F"))
    private float fascinatedutils$scaleVerticalCreativeFlight(Abilities abilities) {
        float base = abilities.getFlyingSpeed();
        Player self = (Player) (Object) this;
        return PlayerUtils.scaleFlyingSpeed(self, base);
    }
}
