package cc.fascinated.fascinatedutils.mixin.fogcustomizer;

import cc.fascinated.fascinatedutils.systems.modules.ModuleRegistry;
import cc.fascinated.fascinatedutils.systems.modules.impl.FogCustomizerModule;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.fog.FogData;
import net.minecraft.client.renderer.fog.environment.AtmosphericFogEnvironment;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Optional;

@Mixin(AtmosphericFogEnvironment.class)
public class AtmosphericFogModifierMixin {

    @Inject(method = "setupFog", at = @At("TAIL"))
    private void fascinatedutils$adjustAtmosphericFog(FogData fog, Camera camera, ClientLevel level, float renderDistance, DeltaTracker deltaTracker, CallbackInfo callbackInfo) {
        Optional<FogCustomizerModule> optionalModule = ModuleRegistry.INSTANCE.getModule(FogCustomizerModule.class);
        if (optionalModule.isEmpty() || !optionalModule.get().isEnabled()) {
            return;
        }
        FogCustomizerModule module = optionalModule.get();
        float multiplier;
        if (level.dimension().equals(Level.NETHER)) {
            multiplier = module.getNetherFogStrength().getValue().floatValue();
        } else if (level.dimension().equals(Level.END)) {
            multiplier = module.getEndFogStrength().getValue().floatValue();
        } else {
            multiplier = module.getAtmosphericFogStrength().getValue().floatValue();
        }
        if (multiplier <= 0f) {
            fog.environmentalStart = 1_000_000f;
            fog.environmentalEnd = 1_000_000f;
            fog.skyEnd = 1_000_000f;
            fog.cloudEnd = 1_000_000f;
            return;
        }
        fog.environmentalStart /= multiplier;
        fog.environmentalEnd /= multiplier;
        fog.skyEnd /= multiplier;
        fog.cloudEnd /= multiplier;
    }
}
