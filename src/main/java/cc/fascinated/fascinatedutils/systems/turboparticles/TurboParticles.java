package cc.fascinated.fascinatedutils.systems.turboparticles;

import cc.fascinated.fascinatedutils.common.culling.CullCounters;
import cc.fascinated.fascinatedutils.common.culling.OcclusionProvider;
import cc.fascinated.fascinatedutils.event.impl.ClientTickEvent;
import cc.fascinated.fascinatedutils.event.impl.lifecycle.ClientStartedEvent;
import cc.fascinated.fascinatedutils.event.impl.lifecycle.ClientStoppingEvent;
import cc.fascinated.fascinatedutils.settings.SettingsRegistry;
import com.logisticscraft.occlusionculling.OcclusionCullingInstance;
import com.logisticscraft.occlusionculling.cache.ArrayOcclusionCache;
import lombok.Getter;
import meteordevelopment.orbit.EventHandler;

/**
 * Turbo Particles: background frustum + occlusion culling for billboard particles.
 */
public class TurboParticles {

    public final CullCounters particleCounters = new CullCounters();

    @Getter
    private ParticleCullTask particleCullTask;
    private Thread particleCullThread;

    private volatile boolean previousEnabledState = false;

    private volatile boolean enabledMirror = false;

    /**
     * Fast gate for mixins: avoids walking {@link SettingsRegistry} for every particle; refreshed each client tick.
     */
    public boolean isEnabled() {
        return enabledMirror;
    }

    public void frameSnapshot() {
        particleCounters.snapshotAndReset();
    }

    public void incrementConsidered(boolean wasCulled) {
        particleCounters.increment(wasCulled);
    }

    @EventHandler
    private void alumite$onClientStarted(ClientStartedEvent event) {
        previousEnabledState = SettingsRegistry.INSTANCE.getSettings().getTurboParticles().isEnabled();
        enabledMirror = previousEnabledState;
        if (previousEnabledState) {
            startParticleCullThread();
        }
    }

    @EventHandler
    private void alumite$onClientTick(ClientTickEvent event) {
        boolean enabled = SettingsRegistry.INSTANCE.getSettings().getTurboParticles().isEnabled();
        enabledMirror = enabled;
        if (enabled == previousEnabledState) {
            return;
        }

        previousEnabledState = enabled;
        if (enabled) {
            startParticleCullThread();
        }
        else {
            stopParticleCullThread();
        }
    }

    @EventHandler
    private void alumite$onClientStopping(ClientStoppingEvent event) {
        enabledMirror = false;
        stopParticleCullThread();
    }

    private void startParticleCullThread() {
        if (particleCullThread != null && particleCullThread.isAlive()) {
            return;
        }
        OcclusionCullingInstance occlusion = new OcclusionCullingInstance(128, new OcclusionProvider(), new ArrayOcclusionCache(128), 0.0);
        particleCullTask = new ParticleCullTask(occlusion);
        particleCullThread = new Thread(particleCullTask, "FascinatedUtils-ParticleCulling");
        particleCullThread.setDaemon(true);
        particleCullThread.start();
    }

    private void stopParticleCullThread() {
        if (particleCullTask != null) {
            particleCullTask.stop();
        }
        particleCullThread = null;
        particleCullTask = null;
    }
}
