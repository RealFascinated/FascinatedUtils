package cc.fascinated.fascinatedutils.common.culling;

import lombok.Getter;

import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;

/**
 * Mutable counters for a single cull category, plus an immutable snapshot of the previous tick.
 */
public class CullCounters {
    private static final AtomicIntegerFieldUpdater<CullCounters> CONSIDERED_UPDATER = AtomicIntegerFieldUpdater.newUpdater(CullCounters.class, "considered");
    private static final AtomicIntegerFieldUpdater<CullCounters> CULLED_UPDATER = AtomicIntegerFieldUpdater.newUpdater(CullCounters.class, "culled");

    private volatile int considered = 0;
    private volatile int culled = 0;

    @Getter
    private volatile int lastConsidered = 0;
    @Getter
    private volatile int lastCulled = 0;

    public void increment(boolean wasCulled) {
        // Render thread is the only writer; a plain read-modify-write avoids a locked atomic per particle.
        considered = considered + 1;
        if (wasCulled) {
            culled = culled + 1;
        }
    }

    /**
     * Counts one back-face test and reports whether the face points away from the camera.
     *
     * @param facing  dot product of the face normal and the vector to the camera
     * @param epsilon how far behind the face the camera must be before it counts as facing away
     * @return true when the face should be culled
     */
    public boolean cullIfFacingAway(double facing, double epsilon) {
        boolean facingAway = facing < -epsilon;
        increment(facingAway);
        return facingAway;
    }

    public void snapshotAndReset() {
        lastConsidered = CONSIDERED_UPDATER.getAndSet(this, 0);
        lastCulled = CULLED_UPDATER.getAndSet(this, 0);
    }
}
