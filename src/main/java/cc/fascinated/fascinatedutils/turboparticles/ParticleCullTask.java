package cc.fascinated.fascinatedutils.turboparticles;

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

@Getter
public class ParticleCullTask implements Runnable {
    private static final int SLEEP_DELAY = 10;
    private static final int PARTICLE_BUFFER_CAPACITY = 8192;

    private final OcclusionCullingInstance culling;

    private final Vec3d aabbMin = new Vec3d(0, 0, 0);
    private final Vec3d aabbMax = new Vec3d(0, 0, 0);
    private final Vec3d cameraVec3d = new Vec3d(0, 0, 0);

    private volatile boolean running = true;
    private volatile boolean requestCull = false;
    @Setter
    private volatile boolean inGame = false;
    @Setter
    private volatile Vec3 camera = Vec3.ZERO;

    private volatile int snapshotReadSlot;

    private volatile int workerActiveReadSlot = -1;

    private final ArrayList<Particle> particleBuffer0 = new ArrayList<>(PARTICLE_BUFFER_CAPACITY);
    private final ArrayList<Particle> particleBuffer1 = new ArrayList<>(PARTICLE_BUFFER_CAPACITY);
    @Setter
    private volatile Frustum frustum = null;

    private volatile int consideredParticleCount = 0;
    private volatile int culledParticleCount = 0;
    private volatile long lastPassNanos = 0L;

    private Vec3 lastCameraPos = Vec3.ZERO;

    public ParticleCullTask(OcclusionCullingInstance culling) {
        this.culling = culling;
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
                    int readSlot = snapshotReadSlot;
                    workerActiveReadSlot = readSlot;
                    try {
                        cullParticles(readSlot);
                    }
                    finally {
                        workerActiveReadSlot = -1;
                    }
                    lastPassNanos = System.nanoTime() - passStart;
                }
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    public int snapshotWriteSlot() {
        int workerSlot = workerActiveReadSlot;
        if (workerSlot == 0) {
            return 1;
        }
        if (workerSlot == 1) {
            return 0;
        }
        return 1 - snapshotReadSlot;
    }

    public ArrayList<Particle> particleBufferForWrite(int writeSlot) {
        return writeSlot == 0 ? particleBuffer0 : particleBuffer1;
    }

    public void publishSnapshotWrite(int writeSlot) {
        this.snapshotReadSlot = writeSlot;
    }

    public void requestCull() {
        this.requestCull = true;
    }

    public void stop() {
        this.running = false;
    }

    private void cullParticles(int readSlot) {
        ArrayList<Particle> snapshot = readSlot == 0 ? particleBuffer0 : particleBuffer1;
        int considered = 0;
        int culled = 0;

        for (Particle particle : snapshot) {
            if (particle == null) {
                break;
            }
            if (!(particle instanceof SingleQuadParticle)) {
                continue;
            }
            if (!(particle instanceof Cullable cullable)) {
                continue;
            }

            try {
                considered++;
                if (cullable.fascinatedutils$isCulled()) {
                    culled++;
                }

                AABB box = cullingBoxForParticle(particle);
                cullable.fascinatedutils$setCulled(!isParticleVisible(box));
            } catch (Exception ignored) {
            }
        }

        consideredParticleCount = considered;
        culledParticleCount = culled;
    }

    private static AABB cullingBoxForParticle(Particle particle) {
        AABB particleBoundingBox = particle.getBoundingBox();
        double centerX = (particleBoundingBox.minX + particleBoundingBox.maxX) / 2.0;
        double baseY = particleBoundingBox.minY;
        double centerZ = (particleBoundingBox.minZ + particleBoundingBox.maxZ) / 2.0;
        return new AABB(centerX - 0.125, baseY - 0.125, centerZ - 0.125, centerX + 0.125, baseY + 0.125, centerZ + 0.125);
    }

    private boolean isParticleVisible(AABB box) {
        if (box == null) {
            return true;
        }

        Frustum currentFrustum = frustum;
        if (currentFrustum != null && !currentFrustum.isVisible(box)) {
            return false;
        }

        aabbMin.set(box.minX, box.minY, box.minZ);
        aabbMax.set(box.maxX, box.maxY, box.maxZ);
        return culling.isAABBVisible(aabbMin, aabbMax, cameraVec3d);
    }
}
