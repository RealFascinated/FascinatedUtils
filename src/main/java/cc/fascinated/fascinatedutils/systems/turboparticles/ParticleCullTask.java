package cc.fascinated.fascinatedutils.systems.turboparticles;

import cc.fascinated.fascinatedutils.common.culling.Cullable;
import com.logisticscraft.occlusionculling.OcclusionCullingInstance;
import com.logisticscraft.occlusionculling.util.Vec3d;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

@Getter
public class ParticleCullTask implements Runnable {
    private static final int SLEEP_DELAY = 10;
    private static final int PARTICLE_BUFFER_CAPACITY = 8192;
    private static final double PARTICLE_FORCE_CULL_DISTANCE_SQ = 64.0 * 64.0;

    private final OcclusionCullingInstance culling;

    private final Vec3d aabbMin = new Vec3d(0, 0, 0);
    private final Vec3d aabbMax = new Vec3d(0, 0, 0);
    private final Vec3d cameraVec3d = new Vec3d(0, 0, 0);
    private final ArrayList<Particle> particlePublishScratch = new ArrayList<>(PARTICLE_BUFFER_CAPACITY);
    private volatile boolean running = true;
    private volatile boolean requestCull = false;
    @Setter
    private volatile boolean inGame = false;
    @Setter
    private volatile Vec3 camera = Vec3.ZERO;
    /**
     * Immutable hand-off from the main thread.
     * The cull thread only reads this reference once per pass.
     */
    private volatile List<Particle> particleCullSnapshot = List.of();

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
                    lastCameraPos = cameraSnap;
                    cameraVec3d.set(cameraSnap.x, cameraSnap.y, cameraSnap.z);

                    long passStart = System.nanoTime();
                    culling.resetCache();

                    cullParticles();

                    lastPassNanos = System.nanoTime() - passStart;
                }
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    /**
     * To be called from the Main Thread (typically in a mixin or client tick).
     * Copies particles currently active in the ParticleManager.
     */
    public void publishParticleCullSnapshot(Iterable<Particle> particles) {
        particlePublishScratch.clear();
        for (Particle particle : particles) {
            if (particle == null) {
                continue;
            }
            // Only add particles that we can actually cull
            if (particle instanceof SingleQuadParticle && particle instanceof Cullable) {
                particlePublishScratch.add(particle);
            }
        }
        this.particleCullSnapshot = List.copyOf(particlePublishScratch);
    }

    public void requestCull() {
        this.requestCull = true;
    }

    public void stop() {
        this.running = false;
    }

    private void cullParticles() {
        int considered = 0;
        int culled = 0;

        for (Particle particle : this.particleCullSnapshot) {
            if (!(particle instanceof Cullable cullable)) {
                continue;
            }

            considered++;

            try {
                // 1. Distance Culling (Fastest)
                double distSq = particle.getBoundingBox().getCenter().distanceToSqr(lastCameraPos);
                if (distSq > PARTICLE_FORCE_CULL_DISTANCE_SQ) {
                    cullable.fascinatedutils$setCulled(true);
                }
                else {
                    // 2. Frustum and Occlusion Culling
                    AABB box = cullingBoxForParticle(particle);
                    cullable.fascinatedutils$setCulled(!isVisible(box));
                }

                if (cullable.fascinatedutils$isCulled()) {
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