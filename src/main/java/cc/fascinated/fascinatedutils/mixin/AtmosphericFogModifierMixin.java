package cc.fascinated.fascinatedutils.mixin;

import cc.fascinated.fascinatedutils.systems.modules.ModuleRegistry;
import cc.fascinated.fascinatedutils.systems.modules.impl.FogCustomizerModule;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.fog.FogData;
import net.minecraft.client.renderer.fog.environment.AtmosphericFogEnvironment;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Optional;

@Mixin(AtmosphericFogEnvironment.class)
public class AtmosphericFogModifierMixin {

    @Inject(method = "setupFog", at = @At("TAIL"))
    private void fascinatedutils$clearAtmosphericFogWhenDisabled(FogData fog, Camera camera, ClientLevel level, float renderDistance, DeltaTracker deltaTracker, CallbackInfo callbackInfo) {
        Optional<FogCustomizerModule> optionalWorldModule = ModuleRegistry.INSTANCE.getModule(FogCustomizerModule.class);
        if (optionalWorldModule.isPresent()) {
            FogCustomizerModule worldModule = optionalWorldModule.get();
            if (worldModule.isEnabled()) {
                float atmosphericFogMultiplier = worldModule.getAtmosphericFogStrength().getValue().floatValue();
                fog.environmentalStart /= atmosphericFogMultiplier;
                fog.environmentalEnd /= atmosphericFogMultiplier;
                fog.skyEnd /= atmosphericFogMultiplier;
                fog.cloudEnd /= atmosphericFogMultiplier;
            }
        }
    }
}
