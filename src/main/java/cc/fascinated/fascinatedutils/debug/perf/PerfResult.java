package cc.fascinated.fascinatedutils.debug.perf;

import com.google.gson.JsonObject;

import java.util.Arrays;

/**
 * Timing summary for one test or live section. All durations are in milliseconds.
 *
 * @param id        the test or section id
 * @param calls     how many measured calls were made
 * @param totalMs   the sum of every measured call
 * @param perCallMs the mean duration of one call
 * @param minMs     the fastest call
 * @param maxMs     the slowest call
 * @param p95Ms     the 95th percentile call
 */
public record PerfResult(String id, int calls, double totalMs, double perCallMs, double minMs, double maxMs, double p95Ms) {
    private static final double NANOS_PER_MILLI = 1_000_000.0;

    /**
     * Summarises raw nanosecond samples.
     *
     * @param id      the test or section id
     * @param samples the measured durations in nanoseconds
     * @return the summary, or an all-zero result when there are no samples
     */
    public static PerfResult fromNanos(String id, long[] samples) {
        if (samples.length == 0) {
            return new PerfResult(id, 0, 0, 0, 0, 0, 0);
        }
        long[] sorted = samples.clone();
        Arrays.sort(sorted);
        long sum = 0;
        for (long sample : sorted) {
            sum += sample;
        }
        int percentileIndex = Math.max(0, (int) Math.ceil(sorted.length * 0.95) - 1);
        return new PerfResult(id, sorted.length, sum / NANOS_PER_MILLI, sum / (double) sorted.length / NANOS_PER_MILLI, sorted[0] / NANOS_PER_MILLI, sorted[sorted.length - 1] / NANOS_PER_MILLI, sorted[percentileIndex] / NANOS_PER_MILLI);
    }

    /**
     * Reads a result previously written by {@link #toJson()}.
     *
     * @param json the serialised result
     * @return the parsed result
     */
    public static PerfResult fromJson(JsonObject json) {
        return new PerfResult(json.get("id").getAsString(), json.get("calls").getAsInt(), json.get("totalMs").getAsDouble(), json.get("perCallMs").getAsDouble(), json.get("minMs").getAsDouble(), json.get("maxMs").getAsDouble(), json.get("p95Ms").getAsDouble());
    }

    /**
     * Serialises this result.
     *
     * @return the JSON form
     */
    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("id", id);
        json.addProperty("calls", calls);
        json.addProperty("totalMs", totalMs);
        json.addProperty("perCallMs", perCallMs);
        json.addProperty("minMs", minMs);
        json.addProperty("maxMs", maxMs);
        json.addProperty("p95Ms", p95Ms);
        return json;
    }
}
