package cc.fascinated.fascinatedutils.debug.perf.core;

import java.lang.management.ManagementFactory;

/**
 * Timings of the client's startup: how long the mod's client initializer took, and how long after the JVM started the
 * world runner first became ready to run a scenario.
 */
public class PerfStartup {
    public static final PerfStartup INSTANCE = new PerfStartup();

    private static final double NANOS_PER_MILLI = 1_000_000.0;

    private long initStartNanos;
    private long initEndNanos;
    private long clientReadyWallMillis;

    /**
     * Marks the start of the mod's client initializer.
     */
    public void markInitStart() {
        initStartNanos = System.nanoTime();
    }

    /**
     * Marks the end of the mod's client initializer.
     */
    public void markInitEnd() {
        initEndNanos = System.nanoTime();
    }

    /**
     * Marks the first moment the title screen is shown with no loading overlay. Later calls are ignored.
     */
    public void markClientReady() {
        if (clientReadyWallMillis != 0) {
            return;
        }
        clientReadyWallMillis = System.currentTimeMillis();
    }

    /**
     * Returns how long the mod's client initializer took.
     *
     * @return the duration in nanoseconds
     */
    public long modInitNanos() {
        return initEndNanos - initStartNanos;
    }

    /**
     * Returns how long after the JVM started the world runner became ready.
     *
     * @return the duration in nanoseconds, measured in wall clock milliseconds and then converted
     */
    public long clientReadyNanos() {
        long jvmStartMillis = ManagementFactory.getRuntimeMXBean().getStartTime();
        return (long) ((clientReadyWallMillis - jvmStartMillis) * NANOS_PER_MILLI);
    }
}
