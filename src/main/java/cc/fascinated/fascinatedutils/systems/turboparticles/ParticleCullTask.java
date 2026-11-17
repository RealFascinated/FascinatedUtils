package cc.fascinated.fascinatedutils.systems.turboparticles;

import cc.fascinated.fascinatedutils.common.culling.AbstractCullTask;
import cc.fascinated.fascinatedutils.common.culling.CullDistances;
import cc.fascinated.fascinatedutils.common.culling.CullSnapshotBuffers;
import cc.fascinated.fascinatedutils.common.culling.Cullable;
import cc.fascinated.fascinatedutils.debug.perf.core.PerfProbe;
import com.logisticscraft.occlusionculling.OcclusionCullingInstance;
import lombok.Getter;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleGroup;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class ParticleCullTask extends AbstractCullTask {
    private static final int SLEEP_DELAY = 20;
    private static final long FRUSTUM_COPY_INTERVAL_NANOS = 20_000_000L;
    private static final PerfProbe.Section PUBLISH_SECTION = PerfProbe.INSTANCE.section("turbo_particles.publish");
    private static final PerfProbe.Section CULL_PASS_SECTION = PerfProbe.INSTANCE.section("turbo_particles.cull_pass");

    private final CullSnapshotBuffers<Particle> particleSnapshots = new CullSnapshotBuffers<>();
    @Getter
    private volatile int consideredParticleCount = 0;
    @Getter
    private volatile int culledParticleCount = 0;

    public ParticleCullTask(OcclusionCullingInstance culling) {
        super(culling, SLEEP_DELAY, FRUSTUM_COPY_INTERVAL_NANOS, CULL_PASS_SECTION);
    }

    @Override
    protected void resetCulledFlags() {
        uncullPublished(particleSnapshots);
    }

    /**
     * Drops the published snapshot so its particles, and the level they reference, can be collected. Called from the
     * main thread when there is no level to cull for.
     */
    public void clearSnapshots() {
        particleSnapshots.clear();
        consideredParticleCount = 0;
        culledParticleCount = 0;
    }

    @Override
    protected long newestSnapshotPublishNanos() {
        return particleSnapshots.getPublishedNanos();
    }

    /**
     * To be called from the Main Thread (typically in a mixin or client tick).
     * Copies particles currently active in the ParticleManager.
     */
    private void collectCullable(Iterable<? extends Particle> particles, ArrayList<Particle> target) {
        for (Particle particle : particles) {
            if (particle instanceof SingleQuadParticle && particle instanceof Cullable) {
                target.add(particle);
            }
        }
    }

    @Override
    protected void cullSnapshots() {
        List<Particle> particles = particleSnapshots.acquire();
        try {
            if (!particles.isEmpty()) {
                resetOcclusionCache();
                cullParticles(particles);
            }
        } finally {
            particleSnapshots.release();
        }
    }

    /**
     * Gathers every particle from the engine's render groups and publishes them as the next cull snapshot.
     * Called from the main thread before particle extraction.
     *
     * @param groups the particle engine's render groups
     */
    public void publishParticleCullSnapshotFromGroups(Collection<ParticleGroup<?>> groups) {
        long publishStart = PUBLISH_SECTION.begin();
        ArrayList<Particle> snapshot = particleSnapshots.beginPublish();
        for (ParticleGroup<?> group : groups) {
            try {
                collectCullable(group.getAll(), snapshot);
            } catch (Exception exception) {
                logFailureOnce("Particle group", exception);
            }
        }
        particleSnapshots.publish(snapshot);
        PUBLISH_SECTION.end(publishStart);
    }

    private void cullParticles(List<Particle> snapshot) {
        Vec3 camera = getLastCameraPos();
        double cameraX = camera.x;
        double cameraY = camera.y;
        double cameraZ = camera.z;
        CullTally tally = cullSnapshot(snapshot, "Particle", (particle, cullable) -> {
            AABB bounds = particle.getBoundingBox();
            double centerX = (bounds.minX + bounds.maxX) * 0.5;
            double centerY = (bounds.minY + bounds.maxY) * 0.5;
            double centerZ = (bounds.minZ + bounds.maxZ) * 0.5;
            double offsetX = centerX - cameraX;
            double offsetY = centerY - cameraY;
            double offsetZ = centerZ - cameraZ;
            if (offsetX * offsetX + offsetY * offsetY + offsetZ * offsetZ > CullDistances.PARTICLE_FORCE_CULL_DISTANCE_SQ) {
                cullable.alumite$setCulled(true);
                return true;
            }

            // Pad by the rendered quad size so large particles are not hidden while part of the quad is on screen.
            double halfSize = Math.max(0.1, ((SingleQuadParticle) particle).getQuadSize(1.0F));
            cullable.alumite$setCulled(!isVisible(centerX - halfSize, centerY - halfSize, centerZ - halfSize, centerX + halfSize, centerY + halfSize, centerZ + halfSize));
            return true;
        });
        consideredParticleCount = tally.considered();
        culledParticleCount = tally.culled();
    }
}
