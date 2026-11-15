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
    private static final String KEY_PREFIX = "gc.";
    private static final String COUNT_SUFFIX = ".count";
    private static final String TIME_SUFFIX = ".timeMs";

    /**
     * Reads the current totals across every collector.
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
     * The report meta key that holds a result's collection count.
     *
     * @param resultId the test or scene id
     * @return {@code gc.<id>.count}
     */
    public static String countKey(String resultId) {
        return KEY_PREFIX + resultId + COUNT_SUFFIX;
    }

    /**
     * The report meta key that holds a result's collection time.
     *
     * @param resultId the test or scene id
     * @return {@code gc.<id>.timeMs}
     */
    public static String timeKey(String resultId) {
        return KEY_PREFIX + resultId + TIME_SUFFIX;
    }

    /**
     * Reports whether a report meta key is a collection count key.
     *
     * @param key the meta key
     * @return true for {@code gc.<id>.count} keys
     */
    public static boolean isCountKey(String key) {
        return key.startsWith(KEY_PREFIX) && key.endsWith(COUNT_SUFFIX);
    }

    /**
     * Extracts the result id from a collection count key.
     *
     * @param countKey a key for which {@link #isCountKey} is true
     * @return the test or scene id
     */
    public static String resultIdOfCountKey(String countKey) {
        return countKey.substring(KEY_PREFIX.length(), countKey.length() - COUNT_SUFFIX.length());
    }

    /**
     * Records the collections since this snapshot into report meta.
     *
     * @param resultId the test or scene id
     * @param meta     the meta map to write into
     * @return a printable {@code gc <id> count=… timeMs=…} line
     */
    public String recordSince(String resultId, Map<String, String> meta) {
        PerfGcSnapshot current = now();
        long countDelta = current.count() - count;
        long timeDelta = current.timeMs() - timeMs;
        meta.put(countKey(resultId), String.valueOf(countDelta));
        meta.put(timeKey(resultId), String.valueOf(timeDelta));
        return String.format(Locale.ROOT, "gc %s count=%d timeMs=%d", resultId, countDelta, timeDelta);
    }
}
