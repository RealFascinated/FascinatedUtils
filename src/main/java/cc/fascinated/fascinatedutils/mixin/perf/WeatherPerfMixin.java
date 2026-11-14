package cc.fascinated.fascinatedutils.mixin.perf;

import cc.fascinated.fascinatedutils.debug.perf.core.PerfProbe;
import net.minecraft.client.renderer.WeatherEffectRenderer;
import net.minecraft.client.renderer.state.level.WeatherRenderState;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Times the vanilla weather extraction so rain scenes can show what the weather cache saves.
 */
@Mixin(WeatherEffectRenderer.class)
public class WeatherPerfMixin {
    @Unique
    private static final PerfProbe.Section ALUMITE$WEATHER_EXTRACT = PerfProbe.INSTANCE.section("weather.extract");
    @Unique
    private long alumite$weatherStart;

    @Inject(method = "extractRenderState", at = @At("HEAD"))
    private void alumite$weatherBegin(Level level, int ticks, float partialTicks, Vec3 cameraPosition, WeatherRenderState renderState, CallbackInfo callbackInfo) {
        alumite$weatherStart = PerfProbe.INSTANCE.begin();
    }

    @Inject(method = "extractRenderState", at = @At("RETURN"))
    private void alumite$weatherEnd(Level level, int ticks, float partialTicks, Vec3 cameraPosition, WeatherRenderState renderState, CallbackInfo callbackInfo) {
        ALUMITE$WEATHER_EXTRACT.end(alumite$weatherStart);
    }
}
