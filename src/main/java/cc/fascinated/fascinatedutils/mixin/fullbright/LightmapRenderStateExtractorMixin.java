package cc.fascinated.fascinatedutils.mixin.fullbright;

import cc.fascinated.fascinatedutils.systems.modules.ModuleRegistry;
import cc.fascinated.fascinatedutils.systems.modules.impl.FullbrightModule;
import net.minecraft.client.renderer.LightmapRenderStateExtractor;
import net.minecraft.client.renderer.state.LightmapRenderState;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LightmapRenderStateExtractor.class)
public class LightmapRenderStateExtractorMixin {

    @Shadow
    private boolean needsUpdate;

    @Unique
    private boolean previousFullbright = false;

    @Inject(method = "extract", at = @At("HEAD"))
    private void onExtractHead(LightmapRenderState renderState, float partialTicks, CallbackInfo ci) {
        boolean enabled = ModuleRegistry.INSTANCE.getModule(FullbrightModule.class)
                .map(FullbrightModule::isEnabled)
                .orElse(false);

        if (!enabled && previousFullbright) {
            // Force vanilla logic to run this frame by pretending update is needed
            this.needsUpdate = true;
        }
    }

    @Inject(method = "extract", at = @At("RETURN"))
    private void onExtractReturn(LightmapRenderState renderState, float partialTicks, CallbackInfo ci) {
        boolean enabled = ModuleRegistry.INSTANCE.getModule(FullbrightModule.class)
                .map(FullbrightModule::isEnabled)
                .orElse(false);

        if (enabled) {
            renderState.ambientColor = new Vector3f(1.0F, 1.0F, 1.0F);
            renderState.darknessEffectScale = 0.0F;
            renderState.needsUpdate = true;
        }

        previousFullbright = enabled;
    }
}