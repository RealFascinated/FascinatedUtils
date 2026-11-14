package cc.fascinated.fascinatedutils.debug.perf.tests.event;

import cc.fascinated.fascinatedutils.client.Client;
import cc.fascinated.fascinatedutils.common.color.RainbowColors;
import cc.fascinated.fascinatedutils.debug.perf.core.PerfTest;
import cc.fascinated.fascinatedutils.debug.perf.coverage.PerfCoverage;
import cc.fascinated.fascinatedutils.systems.modules.impl.FreelookModule;
import cc.fascinated.fascinatedutils.systems.modules.impl.VibrancyModule;
import cc.fascinated.fascinatedutils.systems.modules.impl.ZoomModule;
import cc.fascinated.fascinatedutils.systems.modules.impl.reach.ReachWidget;
import cc.fascinated.fascinatedutils.systems.modules.impl.speed.SpeedWidget;
import cc.fascinated.fascinatedutils.systems.modules.impl.tps.TpsWidget;
import cc.fascinated.fascinatedutils.systems.turboentities.TurboEntities;
import cc.fascinated.fascinatedutils.systems.turboparticles.TurboParticles;
import net.minecraft.client.Minecraft;

import java.util.List;

/**
 * The mod's real end-of-client-tick work in the idle world: keybind callbacks, then the {@code ClientTickEvent} to
 * every listener, with every module enabled and a player present.
 */
public class ClientTickDispatchPerfTest extends PerfTest {

    public ClientTickDispatchPerfTest() {
        super("client.tick_dispatch", "Client.dispatchEndClientTick in the idle world: keybinds plus every tick listener");
    }

    @Override
    public void runOnce() {
        Client.dispatchEndClientTick(Minecraft.getInstance());
    }

    @Override
    public List<String> sceneIds() {
        return List.of("idle");
    }

    @Override
    public List<String> covers() {
        return List.of(PerfCoverage.listener(RainbowColors.class), PerfCoverage.listener(FreelookModule.class), PerfCoverage.listener(ReachWidget.class), PerfCoverage.listener(SpeedWidget.class), PerfCoverage.listener(TpsWidget.class), PerfCoverage.listener(VibrancyModule.class), PerfCoverage.listener(ZoomModule.class), PerfCoverage.listener(TurboEntities.class), PerfCoverage.listener(TurboParticles.class));
    }
}
