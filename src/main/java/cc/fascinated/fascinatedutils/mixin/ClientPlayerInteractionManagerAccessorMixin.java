package cc.fascinated.fascinatedutils.mixin;

import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(MultiPlayerGameMode.class)
public interface ClientPlayerInteractionManagerAccessorMixin {

    @Accessor("destroyBlockPos")
    BlockPos fascinatedutils$getCurrentBreakingPos();

    @Accessor("destroyProgress")
    float fascinatedutils$getCurrentBreakingProgress();
}
