package cc.fascinated.fascinatedutils.mixin.particlefilter;

import cc.fascinated.fascinatedutils.systems.modules.ModuleRegistry;
import cc.fascinated.fascinatedutils.systems.modules.impl.ParticleFilterModule;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientLevel.class)
public class ClientLevelParticleMixin {

    @Inject(method = "addDestroyBlockEffect", at = @At("HEAD"), cancellable = true)
    private void onAddDestroyBlockEffect(BlockPos pos, BlockState state, CallbackInfo ci) {
        ModuleRegistry.INSTANCE.getModule(ParticleFilterModule.class).ifPresent(module -> {
            if (!module.isEnabled()) {
                return;
            }
            module.getParticleToggles().stream()
                    .filter(toggle -> toggle.getNameProvider().get().equals("minecraft:block_crumble"))
                    .findFirst()
                    .ifPresent(toggle -> {
                        if (!toggle.getValue()) {
                            ci.cancel();
                        }
                    });
        });
    }

    @Inject(method = "addBreakingBlockEffect", at = @At("HEAD"), cancellable = true)
    private void onAddBreakingBlockEffect(BlockPos pos, Direction direction, CallbackInfo ci) {
        ModuleRegistry.INSTANCE.getModule(ParticleFilterModule.class).ifPresent(module -> {
            if (!module.isEnabled()) {
                return;
            }
            module.getParticleToggles().stream()
                    .filter(toggle -> toggle.getNameProvider().get().equals("minecraft:block"))
                    .findFirst()
                    .ifPresent(toggle -> {
                        if (!toggle.getValue()) {
                            ci.cancel();
                        }
                    });
        });
    }
}
