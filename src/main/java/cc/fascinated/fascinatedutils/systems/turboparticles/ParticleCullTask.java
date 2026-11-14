package cc.fascinated.fascinatedutils.systems.turboparticles;

import cc.fascinated.fascinatedutils.client.Client;
import cc.fascinated.fascinatedutils.common.culling.Cullable;
import cc.fascinated.fascinatedutils.debug.perf.core.PerfProbe;
import cc.fascinated.fascinatedutils.systems.turboentities.CullSnapshotBuffers;
import com.logisticscraft.occlusionculling.OcclusionCullingInstance;
import com.logisticscraft.occlusionculling.util.Vec3d;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleGroup;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Getter
public class ParticleCullTask implements Runnable {
    private static final int SLEEP_DELAY = 10;
    private static final double PARTICLE_FORCE_CULL_DISTANCE_SQ = 64.0 * 64.0;
    private static final PerfProbe.Section PUBLISH_SECTION = PerfProbe.INSTANCE.section("turbo_particles.publish");
    private static final PerfProbe.Section CULL_PASS_SECTION = PerfProbe.INSTANCE.section("turbo_particles.cull_pass");

    private final OcclusionCullingInstance culling;

    private final Vec3d aabbMin = new Vec3d(0, 0, 0);
    private final Vec3d aabbMax = new Vec3d(0, 0, 0);
    private final Vec3d cameraVec3d = new Vec3d(0, 0, 0);
    private final CullSnapshotBuffers<Particle> particleSnapshots = new CullSnapshotBuffers<>();
    private volatile boolean running = true;
    private volatile boolean requestCull = false;
    @Setter
    private volatile boolean inGame = false;
    @Setter
    private volatile Vec3 camera = Vec3.ZERO;
    @Setter
    private volatile Frustum frustum = null;

    private volatile int consideredParticleCount = 0;
    private volatile int culledParticleCount = 0;
    private volatile long lastPassNanos = 0L;

    private Vec3 lastCameraPos = Vec3.ZERO;

    public ParticleCullTask(OcclusionCullingInstance culling) {
        this.culling = culling;
    }

    private static AABB cullingBoxForParticle(Particle particle) {
        AABB box = particle.getBoundingBox();
        // Create a tighter or standard culling box centered on the particle
        double centerX = (box.minX + box.maxX) * 0.5;
        double centerY = (box.minY + box.maxY) * 0.5;
        double centerZ = (box.minZ + box.maxZ) * 0.5;
        return new AABB(centerX - 0.1, centerY - 0.1, centerZ - 0.1, centerX + 0.1, centerY + 0.1, centerZ + 0.1);
    }

    @Override
    public void run() {
        while (running && Minecraft.getInstance().isRunning()) {
            try {
                Thread.sleep(SLEEP_DELAY);
                if (!inGame) {
                    continue;
                }

                Vec3 cameraSnap = camera;
                if (requestCull || !cameraSnap.equals(lastCameraPos)) {
                    requestCull = false;
                    cullPass(cameraSnap);
                }
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception exception) {
                // A failed pass must not end the thread, or every particle keeps its last culled flag forever.
                Client.LOG.warn("Particle cull pass failed: {}", exception.toString());
            }
        }
    }

    /**
     * To be called from the Main Thread (typically in a mixin or client tick).
     * Copies particles currently active in the ParticleManager.
     */
    private void collectCullable(Iterable<? extends Particle> particles, ArrayList<Particle> target) {
        for (Particle particle : particles) {
            // Only add particles that we can actually cull
            if (particle instanceof SingleQuadParticle && particle instanceof Cullable) {
                target.add(particle);
            }
        }
    }

    /**
     * Runs one occlusion and frustum pass over the published particle snapshot.
     *
     * @param cameraPosition the camera position to cull from
     */
    public void cullPass(Vec3 cameraPosition) {
        lastCameraPos = cameraPosition;
        cameraVec3d.set(cameraPosition.x, cameraPosition.y, cameraPosition.z);

        long passStart = System.nanoTime();
        culling.resetCache();

        List<Particle> particles = particleSnapshots.acquire();
        try {
            cullParticles(particles);
        } finally {
            particleSnapshots.release();
        }

        lastPassNanos = System.nanoTime() - passStart;
        CULL_PASS_SECTION.record(lastPassNanos);
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
            } catch (Exception ignored) {
            }
        }
        particleSnapshots.publish(snapshot);
        PUBLISH_SECTION.end(publishStart);
    }

    public void requestCull() {
        this.requestCull = true;
    }

    public void stop() {
        this.running = false;
    }

    private void cullParticles(List<Particle> snapshot) {
        int considered = 0;
        int culled = 0;

        for (Particle particle : snapshot) {
            if (!(particle instanceof Cullable cullable)) {
                continue;
            }

            considered++;

            try {
                // 1. Distance Culling (Fastest)
                double distSq = particle.getBoundingBox().getCenter().distanceToSqr(lastCameraPos);
                if (distSq > PARTICLE_FORCE_CULL_DISTANCE_SQ) {
                    cullable.alumite$setCulled(true);
                }
                else {
                    // 2. Frustum and Occlusion Culling
                    AABB box = cullingBoxForParticle(particle);
                    cullable.alumite$setCulled(!isVisible(box));
                }

                if (cullable.alumite$isCulled()) {
                    culled++;
                }
            } catch (Exception ignored) {
                // Particles can be volatile; ignore occasional errors during culling pass
            }
        }

        consideredParticleCount = considered;
        culledParticleCount = culled;
    }

    private boolean isVisible(AABB box) {
        if (box == null) {
            return true;
        }

        // Frustum Check
        if (frustum != null && !frustum.isVisible(box)) {
            return false;
        }

        // Occlusion Check
        aabbMin.set(box.minX, box.minY, box.minZ);
        aabbMax.set(box.maxX, box.maxY, box.maxZ);
        return culling.isAABBVisible(aabbMin, aabbMax, cameraVec3d);
    }
}