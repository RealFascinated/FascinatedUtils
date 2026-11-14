package cc.fascinated.fascinatedutils.mixin.perf;

import cc.fascinated.fascinatedutils.debug.perf.core.PerfProbe;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Times the real vanilla frame so perf sessions measure the whole client, not only mod code.
 */
@Mixin(GameRenderer.class)
public class GameRendererPerfMixin {
    @Unique
    private static final PerfProbe.Section ALUMITE$FRAME_CPU = PerfProbe.INSTANCE.section("frame.cpu");
    @Unique
    private static final PerfProbe.Section ALUMITE$FRAME_EXTRACT = PerfProbe.INSTANCE.section("frame.extract");
    @Unique
    private static final PerfProbe.Section ALUMITE$LEVEL_RENDER = PerfProbe.INSTANCE.section("level.render");
    @Unique
    private long alumite$frameStart;
    @Unique
    private long alumite$frameAllocStart;
    @Unique
    private long alumite$extractStart;
    @Unique
    private long alumite$levelStart;

    @Inject(method = "render", at = @At("HEAD"))
    private void alumite$frameBegin(DeltaTracker deltaTracker, boolean advanceGameTime, CallbackInfo callbackInfo) {
        PerfProbe.INSTANCE.markFrame();
        alumite$frameStart = PerfProbe.INSTANCE.begin();
        alumite$frameAllocStart = PerfProbe.INSTANCE.beginAllocation();
    }

    @Inject(method = "render", at = @At("RETURN"))
    private void alumite$frameEnd(DeltaTracker deltaTracker, boolean advanceGameTime, CallbackInfo callbackInfo) {
        ALUMITE$FRAME_CPU.end(alumite$frameStart);
        ALUMITE$FRAME_CPU.endAllocation(alumite$frameAllocStart);
    }

    @Inject(method = "extract", at = @At("HEAD"))
    private void alumite$extractBegin(DeltaTracker deltaTracker, boolean advanceGameTime, CallbackInfo callbackInfo) {
        alumite$extractStart = PerfProbe.INSTANCE.begin();
    }

    @Inject(method = "extract", at = @At("RETURN"))
    private void alumite$extractEnd(DeltaTracker deltaTracker, boolean advanceGameTime, CallbackInfo callbackInfo) {
        ALUMITE$FRAME_EXTRACT.end(alumite$extractStart);
    }

    @Inject(method = "renderLevel", at = @At("HEAD"))
    private void alumite$levelBegin(DeltaTracker deltaTracker, CallbackInfo callbackInfo) {
        alumite$levelStart = PerfProbe.INSTANCE.begin();
    }

    @Inject(method = "renderLevel", at = @At("RETURN"))
    private void alumite$levelEnd(DeltaTracker deltaTracker, CallbackInfo callbackInfo) {
        ALUMITE$LEVEL_RENDER.end(alumite$levelStart);
    }
}
