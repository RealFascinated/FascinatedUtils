package cc.fascinated.fascinatedutils.mixin.soundfilter;

import cc.fascinated.fascinatedutils.common.setting.impl.BooleanSetting;
import cc.fascinated.fascinatedutils.debug.perf.core.PerfProbe;
import cc.fascinated.fascinatedutils.systems.modules.ModuleRegistry;
import cc.fascinated.fascinatedutils.systems.modules.impl.soundfilter.SoundFilterModule;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SoundEngine.class)
public class SoundEngineMixin {

    @Unique
    private static final PerfProbe.Section ALUMITE$PLAY = PerfProbe.INSTANCE.section("soundfilter.play");

    @Unique
    private SoundFilterModule alumite$soundFilter;

    @Inject(method = "play", at = @At("HEAD"), cancellable = true)
    private void alumite$onPlay(SoundInstance instance, CallbackInfoReturnable<SoundEngine.PlayResult> cir) {
        if (alumite$soundFilter == null) {
            alumite$soundFilter = ModuleRegistry.INSTANCE.getModule(SoundFilterModule.class).orElse(null);
        }
        if (alumite$soundFilter == null || !alumite$soundFilter.isEnabled()) {
            return;
        }
        long start = ALUMITE$PLAY.begin();
        try {
            BooleanSetting toggle = alumite$soundFilter.getTogglesById().get(instance.getIdentifier().toString());
            if (toggle != null && !toggle.getValue()) {
                cir.setReturnValue(SoundEngine.PlayResult.NOT_STARTED);
            }
        } finally {
            ALUMITE$PLAY.end(start);
        }
    }
}
