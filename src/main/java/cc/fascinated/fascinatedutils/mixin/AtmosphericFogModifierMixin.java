package cc.fascinated.fascinatedutils.mixin;

import cc.fascinated.fascinatedutils.systems.modules.ModuleRegistry;
import cc.fascinated.fascinatedutils.systems.modules.impl.WorldModule;
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
        Optional<WorldModule> optionalWorldModule = ModuleRegistry.INSTANCE.getModule(WorldModule.class);
        if (optionalWorldModule.isPresent()) {
            WorldModule worldModule = optionalWorldModule.get();
            if (worldModule.isEnabled() && worldModule.getRenderFog().isDisabled()) {
                float noFogDistance = Float.MAX_VALUE;
                fog.environmentalStart = noFogDistance;
                fog.environmentalEnd = noFogDistance;
                fog.skyEnd = noFogDistance;
                fog.cloudEnd = noFogDistance;
            }
        }
    }
}
