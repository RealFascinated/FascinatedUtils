package cc.fascinated.fascinatedutils.debug.perf.core;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.util.Locale;
import java.util.Map;

/**
 * Total garbage collection count and time across every collector at one moment, so a test or scene can report how
 * many collections ran while it was measured.
 *
 * @param count  collections so far
 * @param timeMs milliseconds spent collecting so far
 */
public record PerfGcSnapshot(long count, long timeMs) {

    /**
     * Reads the current totals.
     *
     * @return the snapshot
     */
    public static PerfGcSnapshot now() {
        long count = 0;
        long timeMs = 0;
        for (GarbageCollectorMXBean collector : ManagementFactory.getGarbageCollectorMXBeans()) {
            count += Math.max(0, collector.getCollectionCount());
            timeMs += Math.max(0, collector.getCollectionTime());
        }
        return new PerfGcSnapshot(count, timeMs);
    }

    /**
     * Records the collections since this snapshot into report meta as {@code gc.<id>.count} and {@code gc.<id>.timeMs}.
     *
     * @param resultId the test or scene id
     * @param meta     the meta map to write into
     * @return a printable {@code gc <id> count=… timeMs=…} line
     */
    public String recordSince(String resultId, Map<String, String> meta) {
        PerfGcSnapshot current = now();
        long countDelta = current.count() - count;
        long timeDelta = current.timeMs() - timeMs;
        meta.put("gc." + resultId + ".count", String.valueOf(countDelta));
        meta.put("gc." + resultId + ".timeMs", String.valueOf(timeDelta));
        return String.format(Locale.ROOT, "gc %s count=%d timeMs=%d", resultId, countDelta, timeDelta);
    }
}
