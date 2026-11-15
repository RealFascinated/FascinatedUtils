package cc.fascinated.fascinatedutils.mixin.render;

import java.util.Arrays;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.BlockAndLightGetter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Particle.class)
public class ParticleLightCoordsMixin {
    // Packed light coordinates seen this frame, indexed by the packed block position. -1 marks a free slot because
    // vanilla never returns a negative light coordinate.
    @Unique
    private static final int ALUMITE_LIGHT_CACHE_SIZE = 256;
    @Unique
    private static final long[] alumite$lightKeys = new long[ALUMITE_LIGHT_CACHE_SIZE];
    @Unique
    private static final int[] alumite$lightValues = new int[ALUMITE_LIGHT_CACHE_SIZE];
    @Unique
    private static final BlockPos.MutableBlockPos alumite$blockPos = new BlockPos.MutableBlockPos();
    @Unique
    private static BlockAndLightGetter alumite$cachedLevel;
    @Unique
    private static long alumite$cachedFrame = Long.MIN_VALUE;

    // A fresh block position per particle per frame is only read by the chunk test and the light lookup right after it,
    // so a single scratch position is enough and nothing leaks it.
    @Redirect(method = "getLightCoords", at = @At(value = "INVOKE", target = "Lnet/minecraft/core/BlockPos;containing(DDD)Lnet/minecraft/core/BlockPos;"))
    private static BlockPos alumite$reuseBlockPos(double particleX, double particleY, double particleZ) {
        return alumite$blockPos.set(BlockPos.asLong(Mth.floor(particleX), Mth.floor(particleY), Mth.floor(particleZ)));
    }

    // Hundreds of particles share a block, and light only changes between frames, so resolve the block state and both
    // light layers once per block position instead of once per particle.
    @Redirect(method = "getLightCoords", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/LevelRenderer;getLightCoords(Lnet/minecraft/world/level/BlockAndLightGetter;Lnet/minecraft/core/BlockPos;)I"))
    private static int alumite$cachedLightCoords(BlockAndLightGetter level, BlockPos pos) {
        long frame = Minecraft.getInstance().getFrameTimeNs();
        if (frame != alumite$cachedFrame || level != alumite$cachedLevel) {
            alumite$cachedFrame = frame;
            alumite$cachedLevel = level;
            Arrays.fill(alumite$lightValues, -1);
        }

        long key = pos.asLong();
        int slot = (int) (key ^ (key >>> 32)) & (ALUMITE_LIGHT_CACHE_SIZE - 1);
        int cached = alumite$lightValues[slot];
        if (cached >= 0 && alumite$lightKeys[slot] == key) {
            return cached;
        }

        int light = LevelRenderer.getLightCoords(level, pos);
        alumite$lightKeys[slot] = key;
        alumite$lightValues[slot] = light;
        return light;
    }
}
