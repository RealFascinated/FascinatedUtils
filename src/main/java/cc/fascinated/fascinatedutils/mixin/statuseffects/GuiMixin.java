package cc.fascinated.fascinatedutils.mixin.statuseffects;

import cc.fascinated.fascinatedutils.debug.perf.core.PerfProbe;
import cc.fascinated.fascinatedutils.systems.modules.ModuleRegistry;
import cc.fascinated.fascinatedutils.systems.modules.impl.statuseffects.StatusEffectsModule;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
public class GuiMixin {

    @Unique
    private static final PerfProbe.Section ALUMITE$RENDER = PerfProbe.INSTANCE.section("statuseffects.render");

    @Inject(method = "extractEffects", at = @At(value = "HEAD"), cancellable = true)
    private void alumite$hideStatusEffects(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker, CallbackInfo callbackInfo) {
        long start = ALUMITE$RENDER.begin();
        try {
            if (ModuleRegistry.INSTANCE.isEnabled(StatusEffectsModule.class)) {
                callbackInfo.cancel();
            }
        } finally {
            ALUMITE$RENDER.end(start);
        }
    }
}