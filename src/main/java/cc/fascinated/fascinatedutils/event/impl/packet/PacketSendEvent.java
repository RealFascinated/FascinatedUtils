package cc.fascinated.fascinatedutils.event.impl.packet;

import cc.fascinated.fascinatedutils.event.CancellableFascinatedEvent;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;
import net.minecraft.network.protocol.Packet;

/**
 * Fired on the main thread before a packet is sent to the server.
 *
 * <p>Cancel this event to suppress the packet.
 */
@Getter
@RequiredArgsConstructor
@Accessors(fluent = true)
public class PacketSendEvent extends CancellableFascinatedEvent {
    private final Packet<?> packet;
}
