package cc.fascinated.fascinatedutils.debug.perf;

import cc.fascinated.fascinatedutils.client.Client;
import cc.fascinated.fascinatedutils.debug.perf.report.PerfResult;

import java.util.Locale;

/**
 * Writes the harness's stdout protocol. Every line starts with {@link #PREFIX} and a run ends with {@link #DONE_MARKER};
 * {@code scripts/perf-compare.sh}, {@code scripts/ui-shots.sh} and {@code build.gradle} grep for both, so they must not change.
 */
public class PerfOut {
    public static final PerfOut INSTANCE = new PerfOut();

    public static final String PREFIX = "ALUMITE_PERF ";
    public static final String DONE_MARKER = "ALUMITE_PERF_DONE";

    /**
     * Prints one line to stdout only.
     *
     * @param line the line, without the prefix
     */
    public void print(String line) {
        System.out.println(PREFIX + line);
    }

    /**
     * Prints one line to stdout and mirrors it to the client log.
     *
     * @param line the line, without the prefix
     */
    public void printAndLog(String line) {
        print(line);
        Client.LOG.info("[perf] {}", line);
    }

    /**
     * Prints a result as the line that {@code scripts/perf-compare.sh} parses, and mirrors it to the client log.
     *
     * @param result the result to print
     */
    public void printResult(PerfResult result) {
        printAndLog(String.format(Locale.ROOT, "%s medianMs=%.6f perCallMs=%.6f totalMs=%.6f calls=%d p95Ms=%.6f maxMs=%.6f cvPercent=%.1f allocBytesPerCall=%.0f p99Ms=%.6f p999Ms=%.6f stutters=%d", result.id(), result.medianMs(), result.perCallMs(), result.totalMs(), result.calls(), result.p95Ms(), result.maxMs(), result.cvPercent(), result.allocBytesPerCall(), result.p99Ms(), result.p999Ms(), result.stutters()));
    }

    /**
     * Prints the end-of-run marker that the scripts wait for.
     */
    public void done() {
        System.out.println(DONE_MARKER);
    }
}
