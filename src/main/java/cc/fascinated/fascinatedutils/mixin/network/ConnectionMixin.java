package cc.fascinated.fascinatedutils.mixin.network;

import cc.fascinated.fascinatedutils.debug.perf.core.PerfProbe;
import cc.fascinated.fascinatedutils.event.FascinatedEventBus;
import cc.fascinated.fascinatedutils.event.impl.packet.PacketReceiveEvent;
import cc.fascinated.fascinatedutils.event.impl.packet.PacketSendEvent;
import io.netty.channel.ChannelHandlerContext;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Connection.class)
public abstract class ConnectionMixin {
    @Unique
    private static final PerfProbe.Section ALUMITE$PACKET_SEND = PerfProbe.INSTANCE.section("packet.send");
    @Unique
    private static final PerfProbe.Section ALUMITE$PACKET_RECEIVE = PerfProbe.INSTANCE.section("packet.receive");
    @Unique
    private static final PerfProbe.Counter ALUMITE$PACKETS_SENT = PerfProbe.INSTANCE.counter("packets.sent");
    @Unique
    private static final PerfProbe.Counter ALUMITE$PACKETS_RECEIVED = PerfProbe.INSTANCE.counter("packets.received");

    @Inject(method = "send(Lnet/minecraft/network/protocol/Packet;)V", at = @At("HEAD"), cancellable = true)
    private void alumite$onPacketSend(Packet<?> packet, CallbackInfo callbackInfo) {
        ALUMITE$PACKETS_SENT.add(1);
        long token = ALUMITE$PACKET_SEND.begin();
        try {
            PacketSendEvent event = new PacketSendEvent(packet);
            FascinatedEventBus.INSTANCE.postCancellable(event);
            if (event.isCancelled()) {
                callbackInfo.cancel();
            }
        } finally {
            ALUMITE$PACKET_SEND.end(token);
        }
    }

    @Inject(method = "channelRead0(Lio/netty/channel/ChannelHandlerContext;Lnet/minecraft/network/protocol/Packet;)V", at = @At("HEAD"), cancellable = true)
    private void alumite$onPacketReceive(ChannelHandlerContext ctx, Packet<?> packet, CallbackInfo callbackInfo) {
        ALUMITE$PACKETS_RECEIVED.add(1);
        long token = ALUMITE$PACKET_RECEIVE.begin();
        try {
            PacketReceiveEvent event = new PacketReceiveEvent(packet);
            FascinatedEventBus.INSTANCE.postCancellable(event);
            if (event.isCancelled()) {
                callbackInfo.cancel();
            }
        } finally {
            ALUMITE$PACKET_RECEIVE.end(token);
        }
    }
}
