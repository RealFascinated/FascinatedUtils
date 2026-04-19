package cc.fascinated.fascinatedutils.turboparticles;

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
public class TurboParticlesManager {

    public final CullCounters particleCounters = new CullCounters();

    @Getter
    private ParticleCullTask particleCullTask;
    private Thread particleCullThread;

    private volatile boolean previousEnabledState = false;

    public void frameSnapshot() {
        particleCounters.snapshotAndReset();
    }

    public void incrementConsidered(boolean wasCulled) {
        particleCounters.increment(wasCulled);
    }

    @EventHandler
    private void fascinatedutils$onClientStarted(ClientStartedEvent event) {
        previousEnabledState = SettingsRegistry.INSTANCE.getSettings().getTurboParticles().isEnabled();
        if (previousEnabledState) {
            startParticleCullThread();
        }
    }

    @EventHandler
    private void fascinatedutils$onClientTick(ClientTickEvent event) {
        boolean enabled = SettingsRegistry.INSTANCE.getSettings().getTurboParticles().isEnabled();
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
    private void fascinatedutils$onClientStopping(ClientStoppingEvent event) {
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
