package cc.fascinated.fascinatedutils.common.culling;

import cc.fascinated.fascinatedutils.client.Client;
import cc.fascinated.fascinatedutils.debug.perf.core.PerfAllocation;
import cc.fascinated.fascinatedutils.debug.perf.core.PerfProbe;
import com.logisticscraft.occlusionculling.OcclusionCullingInstance;
import com.logisticscraft.occlusionculling.cache.ArrayOcclusionCache;
import com.logisticscraft.occlusionculling.util.Vec3d;
import lombok.AccessLevel;
import lombok.Getter;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

/**
 * Background worker that culls a published snapshot against the latest camera and frustum handed over by the render thread.
 */
public abstract class AbstractCullTask implements Runnable {
    private static final int OCCLUSION_RANGE = 128;

    private final OcclusionCullingInstance culling;
    private final Vec3d aabbMin = new Vec3d(0, 0, 0);
    private final Vec3d aabbMax = new Vec3d(0, 0, 0);
    private final Vec3d cameraVec3d = new Vec3d(0, 0, 0);
    private final long sleepMillis;
    private final long frustumCopyIntervalNanos;
    private final PerfProbe.Section cullPassSection;
    private final PerfProbe.Section cullCpuSection;
    private final PerfProbe.Section cullLatencySection;
    private final Set<String> loggedFailures = ConcurrentHashMap.newKeySet();
    private volatile boolean running = true;
    private volatile boolean requestCull = false;
    private volatile Thread cullThread = null;
    private volatile boolean inGame = false;
    private volatile Vec3 camera = Vec3.ZERO;
    private volatile Frustum frustum = null;
    @Getter
    private volatile long lastPassNanos = 0L;
    @Getter(AccessLevel.PROTECTED)
    private Vec3 lastCameraPos = Vec3.ZERO;
    private long lastFrustumCopyNanos = 0L;

    protected AbstractCullTask(OcclusionCullingInstance culling, int sleepMillis, long frustumCopyIntervalNanos, PerfProbe.Section cullPassSection) {
        this.culling = culling;
        this.sleepMillis = sleepMillis;
        this.frustumCopyIntervalNanos = frustumCopyIntervalNanos;
        this.cullPassSection = cullPassSection;
        this.cullCpuSection = PerfProbe.INSTANCE.section(cullPassSection.getName() + "_cpu");
        this.cullLatencySection = PerfProbe.INSTANCE.section(cullPassSection.getName().replace("cull_pass", "cull_latency"));
    }

    /**
     * Creates the occlusion culler shared by every turbo cull task.
     *
     * <p>The aabbExpansion of 0 matters: the default 0.5 widens single-block boxes so the library's all-axis INSIDE
     * fast-path treats the camera cell as inside the target for most adjacent block entities when buried in solids.
     *
     * @return a new occlusion culling instance
     */
    public static OcclusionCullingInstance createOcclusionCulling() {
        return new OcclusionCullingInstance(OCCLUSION_RANGE, new OcclusionProvider(), new ArrayOcclusionCache(OCCLUSION_RANGE), 0.0);
    }

    @Override
    public void run() {
        cullThread = Thread.currentThread();
        while (running && Minecraft.getInstance().isRunning()) {
            try {
                // Woken early by requestCull(); otherwise polls for camera movement every sleepMillis.
                LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(sleepMillis));
                if (Thread.currentThread().isInterrupted()) {
                    break;
                }
                if (!inGame) {
                    continue;
                }

                Vec3 cameraSnap = camera;
                if (requestCull || !cameraSnap.equals(lastCameraPos)) {
                    requestCull = false;
                    cullPass(cameraSnap);
                }
            } catch (Exception exception) {
                // A failed pass must not end the thread, or everything keeps its last culled flag forever.
                Client.LOG.warn("{} cull pass failed: {}", getClass().getSimpleName(), exception.toString());
            }
        }
    }

    /**
     * Runs one occlusion and frustum pass over the published snapshots.
     *
     * @param cameraPosition the camera position to cull from
     */
    public void cullPass(Vec3 cameraPosition) {
        lastCameraPos = cameraPosition;
        cameraVec3d.set(cameraPosition.x, cameraPosition.y, cameraPosition.z);
        boolean recording = PerfProbe.INSTANCE.isRecording();
        long cpuStart = recording ? PerfAllocation.INSTANCE.currentThreadCpuNanos() : -1;
        long allocStart = PerfProbe.INSTANCE.beginAllocation();
        long passStart = System.nanoTime();
        cullSnapshots();
        long passEnd = System.nanoTime();
        lastPassNanos = passEnd - passStart;
        cullPassSection.record(lastPassNanos);
        long newestPublishNanos = newestSnapshotPublishNanos();
        if (newestPublishNanos > 0) {
            cullLatencySection.record(passEnd - newestPublishNanos);
        }
        cullPassSection.endAllocation(allocStart);
        if (cpuStart >= 0) {
            cullCpuSection.record(PerfAllocation.INSTANCE.currentThreadCpuNanos() - cpuStart);
        }
    }

    /**
     * Culls the currently published snapshots, reading the camera through {@link #getLastCameraPos()}.
     */
    protected abstract void cullSnapshots();

    /**
     * Returns when the newest snapshot this task culls was published, for measuring how stale the culled data is.
     *
     * @return the {@link System#nanoTime()} of the most recent publish, or 0 when nothing was published yet
     */
    protected abstract long newestSnapshotPublishNanos();

    /**
     * Hands the cull thread the latest camera and, at most once per cull interval, a private copy of the frustum.
     * Marks the worker as out of game when there is no level or player.
     *
     * @param renderCamera the render thread's camera
     * @param liveFrustum  the render thread's frustum
     * @return true when the frame was handed over, false when not in a level
     */
    public boolean handOffFrame(Camera renderCamera, Frustum liveFrustum) {
        Minecraft client = Minecraft.getInstance();
        if (client.level == null || client.player == null) {
            inGame = false;
            return false;
        }

        inGame = true;
        camera = renderCamera.position();
        long now = System.nanoTime();
        if (now - lastFrustumCopyNanos >= frustumCopyIntervalNanos) {
            lastFrustumCopyNanos = now;
            frustum = new Frustum(liveFrustum);
        }
        return true;
    }

    /**
     * Asks the cull thread for one pass as soon as it wakes. Requests do not queue: repeated calls before the next pass
     * collapse into a single pass.
     */
    public void requestCull() {
        this.requestCull = true;
        Thread worker = cullThread;
        if (worker != null) {
            LockSupport.unpark(worker);
        }
    }

    public void stop() {
        this.running = false;
        Thread worker = cullThread;
        if (worker != null) {
            LockSupport.unpark(worker);
        }
    }

    /**
     * Whether this task culls against the frustum copied from the render thread.
     *
     * <p>The copy lags the camera, so a subject outside it stays hidden until the next pass after the camera turns back.
     * Tasks whose subjects vanilla already frustum-tests on the render thread should return false.
     *
     * @return true when {@link #isVisible(AABB)} should apply the frustum test
     */
    protected boolean usesFrustum() {
        return true;
    }

    /**
     * Tests a box against the frustum and the occlusion culler, from the camera of the current pass.
     *
     * @param box the world-space box to test; null counts as visible
     * @return true when the box should still render
     */
    protected boolean isVisible(AABB box) {
        if (box == null) {
            return true;
        }

        if (box.getXsize() > CullDistances.HITBOX_LIMIT || box.getYsize() > CullDistances.HITBOX_LIMIT || box.getZsize() > CullDistances.HITBOX_LIMIT) {
            return true;
        }

        if (usesFrustum() && frustum != null && !frustum.isVisible(box)) {
            return false;
        }

        aabbMin.set(box.minX, box.minY, box.minZ);
        aabbMax.set(box.maxX, box.maxY, box.maxZ);
        return culling.isAABBVisible(aabbMin, aabbMax, cameraVec3d);
    }

    /**
     * Resets the occlusion cache; call once per pass before testing boxes.
     */
    protected void resetOcclusionCache() {
        culling.resetCache();
    }

    /**
     * Counts from one cull pass over a snapshot.
     *
     * @param considered how many subjects took part in the pass
     * @param culled     how many of those subjects ended up culled
     */
    protected record CullTally(int considered, int culled) {
    }

    /**
     * Culling decision for one snapshot subject.
     *
     * @param <T> the snapshot element type
     */
    @FunctionalInterface
    protected interface CullDecision<T> {
        /**
         * Sets the culled flag of one subject.
         *
         * @param subject  the snapshot element
         * @param cullable the same element, already known to be {@link Cullable}
         * @return true when the subject takes part in the tally, false when it is skipped
         * @throws Exception when the decision fails
         */
        boolean decide(T subject, Cullable cullable) throws Exception;
    }

    /**
     * Applies one culling decision to every {@link Cullable} in a snapshot and tallies the outcome.
     *
     * <p>A decision that throws marks its subject visible and logs the failure once. The subject still counts as
     * considered, but never as culled.
     *
     * @param snapshot    the snapshot to walk
     * @param subjectName the subject name used in the failure log
     * @param decision    the culling decision for one subject
     * @param <T>         the snapshot element type
     * @return how many subjects were considered and how many of those were culled
     */
    protected <T> CullTally cullSnapshot(List<T> snapshot, String subjectName, CullDecision<T> decision) {
        int considered = 0;
        int culled = 0;
        for (T subject : snapshot) {
            if (!(subject instanceof Cullable cullable)) {
                continue;
            }
            try {
                if (!decision.decide(subject, cullable)) {
                    continue;
                }
            } catch (Exception exception) {
                // Fail visible: one bad subject must not stay hidden or abort the pass for the rest.
                cullable.alumite$setCulled(false);
                logFailureOnce(subjectName, exception);
            }
            considered++;
            if (cullable.alumite$isCulled()) {
                culled++;
            }
        }
        return new CullTally(considered, culled);
    }

    /**
     * Logs a cull failure at debug level, once per subject and exception type.
     *
     * @param subject   what was being culled
     * @param exception the failure
     */
    protected void logFailureOnce(String subject, Exception exception) {
        if (loggedFailures.add(subject + exception.getClass().getName())) {
            Client.LOG.debug("{} cull failed: {}", subject, exception.toString());
        }
    }
}
