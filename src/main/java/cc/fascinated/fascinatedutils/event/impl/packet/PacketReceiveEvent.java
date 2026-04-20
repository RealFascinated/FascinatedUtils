package cc.fascinated.fascinatedutils.event.impl.packet;

import cc.fascinated.fascinatedutils.event.CancellableFascinatedEvent;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;
import net.minecraft.network.protocol.Packet;

/**
 * Fired on the Netty I/O thread when a packet is received from the server,
 * before it is dispatched to the packet listener.
 *
 * <p>Cancel this event to discard the packet entirely.
 */
@Getter
@RequiredArgsConstructor
@Accessors(fluent = true)
public class PacketReceiveEvent extends CancellableFascinatedEvent {
    private final Packet<?> packet;
}
