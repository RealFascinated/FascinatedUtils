package cc.fascinated.fascinatedutils.mixin;

import cc.fascinated.fascinatedutils.systems.modules.ModuleRegistry;
import cc.fascinated.fascinatedutils.systems.modules.impl.FogCustomizerModule;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.fog.FogData;
import net.minecraft.client.renderer.fog.environment.WaterFogEnvironment;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Optional;

@Mixin(WaterFogEnvironment.class)
public class WaterFogModifierMixin {

    @Inject(method = "setupFog", at = @At("TAIL"))
    private void fascinatedutils$adjustWaterFog(FogData fog, Camera camera, ClientLevel level, float renderDistance, DeltaTracker deltaTracker, CallbackInfo callbackInfo) {
        Optional<FogCustomizerModule> optionalModule = ModuleRegistry.INSTANCE.getModule(FogCustomizerModule.class);
        if (optionalModule.isEmpty() || !optionalModule.get().isEnabled()) {
            return;
        }
        float multiplier = optionalModule.get().getWaterFogStrength().getValue().floatValue();
        if (multiplier <= 0f) {
            fog.environmentalStart = 1_000_000f;
            fog.environmentalEnd = 1_000_000f;
            return;
        }
        fog.environmentalStart /= multiplier;
        fog.environmentalEnd /= multiplier;
    }
}
