package cc.fascinated.fascinatedutils.mixin.reach;

import cc.fascinated.fascinatedutils.systems.modules.ModuleRegistry;
import cc.fascinated.fascinatedutils.systems.modules.impl.ReachWidget;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.EntityHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

@Mixin(MultiPlayerGameMode.class)
public class EntityInteractionReachMixin {

    @Inject(method = "attack", at = @At("HEAD"))
    private void fascinatedutils$recordAttackReach(Player player, Entity entity, CallbackInfo ci) {
        recordReach(player, entity);
    }

    @Inject(method = "interact", at = @At("HEAD"))
    private void fascinatedutils$recordInteractReach(Player player, Entity entity, EntityHitResult hitResult, InteractionHand usedHand, CallbackInfoReturnable<InteractionResult> cir) {
        recordReach(player, entity);
    }

    private void recordReach(Player player, Entity entity) {
        Optional<ReachWidget> optionalWidget = ModuleRegistry.INSTANCE.getModule(ReachWidget.class);
        if (optionalWidget.isPresent()) {
            ReachWidget widget = optionalWidget.get();
            if (widget.isEnabled()) {
                widget.recordEntityReach(player.distanceTo(entity));
            }
        }
    }
}
