package cc.fascinated.fascinatedutils.mixin.perf;

import cc.fascinated.fascinatedutils.debug.perf.core.PerfProbe;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Times the real vanilla client tick, which includes every tick mixin the mod adds.
 */
@Mixin(Minecraft.class)
public class MinecraftPerfMixin {
    @Unique
    private static final PerfProbe.Section ALUMITE$TICK_FULL = PerfProbe.INSTANCE.section("tick.full");
    @Unique
    private long alumite$tickStart;
    @Unique
    private long alumite$tickAllocStart;

    @Inject(method = "tick", at = @At("HEAD"))
    private void alumite$tickBegin(CallbackInfo callbackInfo) {
        alumite$tickStart = PerfProbe.INSTANCE.begin();
        alumite$tickAllocStart = PerfProbe.INSTANCE.beginAllocation();
    }

    @Inject(method = "tick", at = @At("RETURN"))
    private void alumite$tickEnd(CallbackInfo callbackInfo) {
        ALUMITE$TICK_FULL.end(alumite$tickStart);
        ALUMITE$TICK_FULL.endAllocation(alumite$tickAllocStart);
    }
}
