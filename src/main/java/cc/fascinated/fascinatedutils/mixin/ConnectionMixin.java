package cc.fascinated.fascinatedutils.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import cc.fascinated.fascinatedutils.event.FascinatedEventBus;
import cc.fascinated.fascinatedutils.event.impl.packet.PacketReceiveEvent;
import cc.fascinated.fascinatedutils.event.impl.packet.PacketSendEvent;
import io.netty.channel.ChannelHandlerContext;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;

@Mixin(Connection.class)
public abstract class ConnectionMixin {

    @Inject(method = "send(Lnet/minecraft/network/protocol/Packet;)V", at = @At("HEAD"), cancellable = true)
    private void fascinatedutils$onPacketSend(Packet<?> packet, CallbackInfo callbackInfo) {
        PacketSendEvent event = new PacketSendEvent(packet);
        FascinatedEventBus.INSTANCE.postCancellable(event);
        if (event.isCancelled()) {
            callbackInfo.cancel();
        }
    }

    @Inject(method = "channelRead0(Lio/netty/channel/ChannelHandlerContext;Lnet/minecraft/network/protocol/Packet;)V", at = @At("HEAD"), cancellable = true)
    private void fascinatedutils$onPacketReceive(ChannelHandlerContext context, Packet<?> packet, CallbackInfo callbackInfo) {
        PacketReceiveEvent event = new PacketReceiveEvent(packet);
        FascinatedEventBus.INSTANCE.postCancellable(event);
        if (event.isCancelled()) {
            callbackInfo.cancel();
        }
    }
}
