package cc.fascinated.fascinatedutils.common.culling;

import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;

/**
 * Mutable counters for a single cull category, plus an immutable snapshot of the previous tick.
 */
public final class CullCounters {
    private static final AtomicIntegerFieldUpdater<CullCounters> CONSIDERED_UPDATER = AtomicIntegerFieldUpdater.newUpdater(CullCounters.class, "considered");
    private static final AtomicIntegerFieldUpdater<CullCounters> CULLED_UPDATER = AtomicIntegerFieldUpdater.newUpdater(CullCounters.class, "culled");

    public volatile int considered = 0;
    public volatile int culled = 0;

    public volatile int lastConsidered = 0;
    public volatile int lastCulled = 0;

    public void increment(boolean wasCulled) {
        // Render thread is the only writer; a plain read-modify-write avoids a locked atomic per particle.
        considered = considered + 1;
        if (wasCulled) {
            culled = culled + 1;
        }
    }

    public void snapshotAndReset() {
        lastConsidered = CONSIDERED_UPDATER.getAndSet(this, 0);
        lastCulled = CULLED_UPDATER.getAndSet(this, 0);
    }
}
