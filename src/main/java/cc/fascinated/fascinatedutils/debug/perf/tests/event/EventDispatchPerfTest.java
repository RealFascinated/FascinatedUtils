package cc.fascinated.fascinatedutils.debug.perf.tests.event;

import cc.fascinated.fascinatedutils.debug.perf.core.PerfTest;
import cc.fascinated.fascinatedutils.debug.perf.coverage.PerfCoverage;
import cc.fascinated.fascinatedutils.event.FascinatedEventBus;
import cc.fascinated.fascinatedutils.event.impl.chat.ChatMessageEvent;
import cc.fascinated.fascinatedutils.event.impl.packet.PacketReceiveEvent;
import cc.fascinated.fascinatedutils.systems.modules.impl.hypixel.feature.AutoGG;
import cc.fascinated.fascinatedutils.systems.modules.impl.mcutils.McUtilsModule;
import net.minecraft.client.multiplayer.chat.GuiMessageSource;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.ClientboundKeepAlivePacket;

import java.util.List;

/**
 * One received packet and one chat line through the event bus, as ConnectionMixin and ChatComponentMixin post them,
 * without any network or chat-screen side effects.
 */
public class EventDispatchPerfTest extends PerfTest {
    private static final long KEEP_ALIVE_ID = 42L;

    private final ClientboundKeepAlivePacket packet = new ClientboundKeepAlivePacket(KEEP_ALIVE_ID);
    private final Component chatLine = Component.literal("perf chat line from another player");
    private boolean lastCancelled;

    public EventDispatchPerfTest() {
        super("event.packet_chat_dispatch", "PacketReceiveEvent plus ChatMessageEvent through the event bus", 50, 20_000);
    }

    @Override
    public void runOnce() {
        PacketReceiveEvent packetEvent = new PacketReceiveEvent(packet);
        FascinatedEventBus.INSTANCE.postCancellable(packetEvent);
        ChatMessageEvent chatEvent = new ChatMessageEvent(chatLine, GuiMessageSource.SYSTEM_SERVER);
        FascinatedEventBus.INSTANCE.postCancellable(chatEvent);
        lastCancelled = packetEvent.isCancelled() | chatEvent.isCancelled();
    }

    @Override
    public List<String> covers() {
        return List.of(PerfCoverage.listener(McUtilsModule.class), PerfCoverage.listener(AutoGG.class));
    }
}
