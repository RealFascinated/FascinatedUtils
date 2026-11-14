package cc.fascinated.fascinatedutils.mixin.turboweather;

import cc.fascinated.fascinatedutils.client.Client;
import cc.fascinated.fascinatedutils.systems.turboweather.TurboWeather;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.WeatherEffectRenderer;
import net.minecraft.client.renderer.state.level.WeatherRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndLightGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WeatherEffectRenderer.class)
public class WeatherEffectRendererMixin {
    @Inject(method = "extractRenderState", at = @At("HEAD"))
    private void alumite$beginWeatherFrame(Level level, int ticks, float partialTicks, Vec3 cameraPosition, WeatherRenderState renderState, CallbackInfo callbackInfo) {
        Client.TURBO_WEATHER.beginFrame(level);
    }

    @WrapOperation(method = "extractRenderState", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;getHeight(Lnet/minecraft/world/level/levelgen/Heightmap$Types;II)I"))
    private int alumite$cachedHeight(Level level, Heightmap.Types types, int x, int z, Operation<Integer> original) {
        TurboWeather weather = Client.TURBO_WEATHER;
        if (!weather.isEnabled()) {
            return original.call(level, types, x, z);
        }
        TurboWeather.ColumnTable table = weather.heights();
        if (table.find(x, z, BlockPos.asLong(x, 0, z))) {
            return table.found();
        }
        return table.store(original.call(level, types, x, z));
    }

    @WrapOperation(method = "extractRenderState", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/WeatherEffectRenderer;getPrecipitationAt(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/biome/Biome$Precipitation;"))
    private Biome.Precipitation alumite$cachedPrecipitation(WeatherEffectRenderer instance, Level level, BlockPos pos, Operation<Biome.Precipitation> original) {
        TurboWeather weather = Client.TURBO_WEATHER;
        if (!weather.isEnabled()) {
            return original.call(instance, level, pos);
        }
        TurboWeather.ColumnTable table = weather.precipitations();
        if (table.find(pos.getX(), pos.getZ(), pos.asLong())) {
            return Biome.Precipitation.values()[table.found()];
        }
        Biome.Precipitation precipitation = original.call(instance, level, pos);
        table.store(precipitation.ordinal());
        return precipitation;
    }

    @WrapOperation(method = "extractRenderState", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/LevelRenderer;getLightCoords(Lnet/minecraft/world/level/BlockAndLightGetter;Lnet/minecraft/core/BlockPos;)I"))
    private int alumite$cachedLight(BlockAndLightGetter getter, BlockPos pos, Operation<Integer> original) {
        TurboWeather weather = Client.TURBO_WEATHER;
        if (!weather.isEnabled()) {
            return original.call(getter, pos);
        }
        TurboWeather.ColumnTable table = weather.lights();
        if (table.find(pos.getX(), pos.getZ(), pos.asLong())) {
            return table.found();
        }
        return table.store(original.call(getter, pos));
    }
}
