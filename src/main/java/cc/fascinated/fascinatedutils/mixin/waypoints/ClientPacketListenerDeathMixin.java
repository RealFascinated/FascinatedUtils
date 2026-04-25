package cc.fascinated.fascinatedutils.mixin.waypoints;

import cc.fascinated.fascinatedutils.event.FascinatedEventBus;
import cc.fascinated.fascinatedutils.event.impl.PlayerDeathEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundPlayerCombatKillPacket;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public class ClientPacketListenerDeathMixin {

    @Inject(method = "handlePlayerCombatKill", at = @At("HEAD"))
    private void fascinatedutils$onPlayerCombatKill(ClientboundPlayerCombatKillPacket packet, CallbackInfo ci) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return;
        }
        Entity entity = minecraft.level.getEntity(packet.playerId());
        if (entity == minecraft.player) {
            FascinatedEventBus.INSTANCE.post(new PlayerDeathEvent(minecraft.player));
        }
    }
}
