package cc.fascinated.fascinatedutils.mixin;

import cc.fascinated.fascinatedutils.settings.SettingsRegistry;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin {

    @Inject(method = "shouldShowName(Lnet/minecraft/world/entity/LivingEntity;D)Z", at = @At("HEAD"), cancellable = true)
    private void fascinatedutils$respectShowSelfNameplate(LivingEntity entity, double distanceToCameraSq, CallbackInfoReturnable<Boolean> cir) {
        if (!(entity instanceof LocalPlayer)) {
            return;
        }
        cir.setReturnValue(SettingsRegistry.INSTANCE.getSettings().getShowSelfNameplate().isEnabled());
    }
}
