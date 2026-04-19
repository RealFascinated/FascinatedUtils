package cc.fascinated.fascinatedutils.common.culling;

/**
 * Mutable counters for a single cull category, plus an immutable snapshot of the previous tick.
 */
public final class CullCounters {
    public volatile int considered = 0;
    public volatile int culled = 0;

    public volatile int lastConsidered = 0;
    public volatile int lastCulled = 0;

    public void increment(boolean wasCulled) {
        considered++;
        if (wasCulled) {
            culled++;
        }
    }

    public void snapshotAndReset() {
        lastConsidered = considered;
        lastCulled = culled;
        considered = 0;
        culled = 0;
    }
}