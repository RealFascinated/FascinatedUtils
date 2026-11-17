package cc.fascinated.fascinatedutils.debug.perf.tests.particle;

import cc.fascinated.fascinatedutils.debug.perf.core.PerfTest;
import cc.fascinated.fascinatedutils.debug.perf.coverage.PerfCoverage;
import cc.fascinated.fascinatedutils.systems.modules.ModuleRegistry;
import cc.fascinated.fascinatedutils.systems.modules.impl.particlefilter.ParticleFilterModule;
import net.minecraft.core.particles.ParticleTypes;

import java.util.List;

/**
 * Runs the real particle filter check that executes for every particle the game creates.
 */
public class ParticleFilterPerfTest extends PerfTest {
    private ParticleFilterModule module;
    private boolean lastResult;

    public ParticleFilterPerfTest() {
        super("particlefilter.is_blocked", "ParticleFilterModule.isParticleBlocked for a late-registered particle type", 50, 20_000);
    }

    @Override
    public void setup() {
        module = ModuleRegistry.INSTANCE.getModule(ParticleFilterModule.class).orElseThrow();
    }

    @Override
    public List<String> covers() {
        return List.of(PerfCoverage.module(ParticleFilterModule.class));
    }

    @Override
    public void runOnce() {
        lastResult = module.isParticleBlocked(ParticleTypes.WHITE_SMOKE);
    }
}
