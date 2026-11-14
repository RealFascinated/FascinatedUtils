package cc.fascinated.fascinatedutils.debug.perf.tests;

import cc.fascinated.fascinatedutils.debug.perf.PerfTest;
import cc.fascinated.fascinatedutils.event.FascinatedEventBus;
import cc.fascinated.fascinatedutils.event.impl.ClientTickEvent;
import net.minecraft.client.Minecraft;

/**
 * Posts a {@link ClientTickEvent}, so the cost is every mod listener that runs each client tick.
 */
public class ClientTickDispatchPerfTest extends PerfTest {

    public ClientTickDispatchPerfTest() {
        super("event.client_tick_dispatch", "One ClientTickEvent through the event bus to every mod listener");
    }

    @Override
    public void runOnce() {
        FascinatedEventBus.INSTANCE.post(new ClientTickEvent(Minecraft.getInstance()));
    }
}
