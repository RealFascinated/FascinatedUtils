package cc.fascinated.fascinatedutils.mixin.particlefilter;

import cc.fascinated.fascinatedutils.debug.perf.core.PerfProbe;
import cc.fascinated.fascinatedutils.systems.modules.ModuleRegistry;
import cc.fascinated.fascinatedutils.systems.modules.impl.particlefilter.ParticleFilterModule;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientLevel.class)
public class ClientLevelParticleMixin {

    @Unique
    private static final PerfProbe.Section ALUMITE$BLOCK_EFFECT = PerfProbe.INSTANCE.section("particlefilter.block_effect");
    @Unique
    private ParticleFilterModule alumite$particleFilter;

    @Inject(method = "addDestroyBlockEffect", at = @At("HEAD"), cancellable = true)
    private void alumite$onAddDestroyBlockEffect(BlockPos pos, BlockState state, CallbackInfo ci) {
        long start = ALUMITE$BLOCK_EFFECT.begin();
        try {
            alumite$cancelIfParticleDisabled(ParticleTypes.BLOCK_CRUMBLE, ci);
        } finally {
            ALUMITE$BLOCK_EFFECT.end(start);
        }
    }

    @Inject(method = "addBreakingBlockEffect", at = @At("HEAD"), cancellable = true)
    private void alumite$onAddBreakingBlockEffect(BlockPos pos, Direction direction, CallbackInfo ci) {
        long start = ALUMITE$BLOCK_EFFECT.begin();
        try {
            alumite$cancelIfParticleDisabled(ParticleTypes.BLOCK, ci);
        } finally {
            ALUMITE$BLOCK_EFFECT.end(start);
        }
    }

    @Unique
    private void alumite$cancelIfParticleDisabled(ParticleType<?> particleType, CallbackInfo ci) {
        if (alumite$particleFilter == null) {
            alumite$particleFilter = ModuleRegistry.INSTANCE.getModule(ParticleFilterModule.class).orElse(null);
        }
        if (alumite$particleFilter == null || !alumite$particleFilter.isEnabled()) {
            return;
        }
        if (alumite$particleFilter.isParticleTypeBlocked(particleType)) {
            ci.cancel();
        }
    }
}
