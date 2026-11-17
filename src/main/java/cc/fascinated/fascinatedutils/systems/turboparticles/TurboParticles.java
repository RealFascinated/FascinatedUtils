package cc.fascinated.fascinatedutils.systems.turboparticles;

import cc.fascinated.fascinatedutils.common.culling.AbstractCullTask;
import cc.fascinated.fascinatedutils.common.culling.CullThreadLifecycle;
import cc.fascinated.fascinatedutils.debug.perf.core.PerfProbe;
import cc.fascinated.fascinatedutils.event.impl.ClientTickEvent;
import cc.fascinated.fascinatedutils.event.impl.lifecycle.ClientStartedEvent;
import cc.fascinated.fascinatedutils.event.impl.lifecycle.ClientStoppingEvent;
import cc.fascinated.fascinatedutils.systems.settings.Settings;
import meteordevelopment.orbit.EventHandler;

/**
 * Turbo Particles: background frustum + occlusion culling for billboard particles.
 */
public class TurboParticles {
    // Particles move every tick, but their occlusion state changes slowly, so a pass every other tick halves the cull
    // cost with at most one tick of extra latency.
    private static final int CULL_SNAPSHOT_CAPTURE_RATE_TICKS = 2;
    private static final PerfProbe.Counter PARTICLES_CONSIDERED_COUNTER = PerfProbe.INSTANCE.counter("turbo_particles.considered");
    private static final PerfProbe.Counter PARTICLES_CULLED_COUNTER = PerfProbe.INSTANCE.counter("turbo_particles.culled");

    private int tickCounter;
    private int snapshotTick = -1;
    private int captureTickCounter;
    private final CullThreadLifecycle<ParticleCullTask> cullLifecycle = new CullThreadLifecycle<>(() -> Settings.INSTANCE.getTurboParticles().isEnabled(), () -> new ParticleCullTask(AbstractCullTask.createOcclusionCulling()), "FascinatedUtils-ParticleCulling", () -> {
        snapshotTick = -1;
        captureTickCounter = 0;
    });

    /**
     * Fast gate for mixins: avoids walking {@link Settings} for every particle; refreshed each client tick.
     */
    public boolean isEnabled() {
        return cullLifecycle.isEnabled();
    }

    public ParticleCullTask getCullTask() {
        return cullLifecycle.getTask();
    }

    public boolean isCullThreadAlive() {
        return cullLifecycle.isThreadAlive();
    }

    /**
     * Claims the particle snapshot slot for this tick.
     *
     * @return true when a snapshot should be published and a pass requested this tick
     */
    public boolean claimTickSnapshot() {
        if (snapshotTick == tickCounter) {
            return false;
        }
        snapshotTick = tickCounter;
        return captureTickCounter++ % CULL_SNAPSHOT_CAPTURE_RATE_TICKS == 0;
    }

    @EventHandler
    private void alumite$onClientStarted(ClientStartedEvent event) {
        cullLifecycle.onClientStarted();
    }

    @EventHandler
    private void alumite$onClientTick(ClientTickEvent event) {
        tickCounter++;
        cullLifecycle.onClientTick();

        ParticleCullTask cullTask = cullLifecycle.getTask();
        if (cullTask != null && cullLifecycle.isEnabled()) {
            PARTICLES_CONSIDERED_COUNTER.add(cullTask.getConsideredParticleCount());
            PARTICLES_CULLED_COUNTER.add(cullTask.getCulledParticleCount());
        }
    }

    @EventHandler
    private void alumite$onClientStopping(ClientStoppingEvent event) {
        cullLifecycle.onClientStopping();
    }
}
