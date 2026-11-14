package cc.fascinated.fascinatedutils.debug.perf.report;

import com.google.gson.JsonObject;

import java.util.Arrays;

/**
 * Timing summary for one test or live section. All durations are in milliseconds.
 *
 * @param id                the test or section id
 * @param calls             how many calls were timed in total
 * @param samples           how many samples the statistics were computed from
 * @param totalMs           the time spent in all timed calls
 * @param perCallMs         the mean duration of one call
 * @param medianMs          the median sample, which ignores GC and scheduler spikes
 * @param minMs             the fastest sample
 * @param maxMs             the slowest sample
 * @param p95Ms             the 95th percentile sample
 * @param cvPercent         the spread of the samples (standard deviation over mean, in percent)
 * @param allocBytesPerCall bytes allocated per call on the measuring thread, or -1 when not measured
 */
public record PerfResult(String id, long calls, int samples, double totalMs, double perCallMs, double medianMs, double minMs, double maxMs, double p95Ms, double cvPercent, double allocBytesPerCall) {
    private static final double NANOS_PER_MILLI = 1_000_000.0;

    /**
     * Summarises per-call samples.
     *
     * @param id                the test or section id
     * @param nanosPerCall      one value per sample: the mean nanoseconds of one call
     * @param callsPerSample    how many calls each sample averaged over
     * @param allocBytesPerCall bytes allocated per call, or -1 when not measured
     * @return the summary, or an all-zero result when there are no samples
     */
    public static PerfResult fromSamples(String id, double[] nanosPerCall, long callsPerSample, double allocBytesPerCall) {
        if (nanosPerCall.length == 0) {
            return new PerfResult(id, 0, 0, 0, 0, 0, 0, 0, 0, 0, allocBytesPerCall);
        }
        double[] sorted = nanosPerCall.clone();
        Arrays.sort(sorted);
        double sum = 0;
        for (double sample : sorted) {
            sum += sample;
        }
        double mean = sum / sorted.length;
        double squaredDeviation = 0;
        for (double sample : sorted) {
            squaredDeviation += (sample - mean) * (sample - mean);
        }
        double standardDeviation = sorted.length > 1 ? Math.sqrt(squaredDeviation / (sorted.length - 1)) : 0;
        int percentileIndex = Math.max(0, (int) Math.ceil(sorted.length * 0.95) - 1);
        double median = sorted.length % 2 == 1 ? sorted[sorted.length / 2] : (sorted[sorted.length / 2 - 1] + sorted[sorted.length / 2]) / 2.0;
        return new PerfResult(id, sorted.length * callsPerSample, sorted.length, sum * callsPerSample / NANOS_PER_MILLI, mean / NANOS_PER_MILLI, median / NANOS_PER_MILLI, sorted[0] / NANOS_PER_MILLI, sorted[sorted.length - 1] / NANOS_PER_MILLI, sorted[percentileIndex] / NANOS_PER_MILLI, mean == 0 ? 0 : standardDeviation / mean * 100.0, allocBytesPerCall);
    }

    /**
     * Summarises raw nanosecond samples where every sample is one call (live sections).
     *
     * @param id      the section id
     * @param samples the measured durations in nanoseconds
     * @return the summary
     */
    public static PerfResult fromNanos(String id, long[] samples) {
        return fromNanos(id, samples, -1);
    }

    /**
     * Summarises raw nanosecond samples where every sample is one call (live sections), with allocation data.
     *
     * @param id                the section id
     * @param samples           the measured durations in nanoseconds
     * @param allocBytesPerCall mean bytes allocated per call, or -1 when not measured
     * @return the summary
     */
    public static PerfResult fromNanos(String id, long[] samples, double allocBytesPerCall) {
        double[] values = new double[samples.length];
        for (int index = 0; index < samples.length; index++) {
            values[index] = samples[index];
        }
        return fromSamples(id, values, 1, allocBytesPerCall);
    }

    /**
     * Copies this result under a different id.
     *
     * @param newId the id for the copy
     * @return the renamed result
     */
    public PerfResult withId(String newId) {
        return new PerfResult(newId, calls, samples, totalMs, perCallMs, medianMs, minMs, maxMs, p95Ms, cvPercent, allocBytesPerCall);
    }

    /**
     * Reads a result previously written by {@link #toJson()}. Fields missing from older reports fall back to the mean.
     *
     * @param json the serialised result
     * @return the parsed result
     */
    public static PerfResult fromJson(JsonObject json) {
        double perCall = json.get("perCallMs").getAsDouble();
        int calls = json.get("calls").getAsInt();
        return new PerfResult(json.get("id").getAsString(), calls, json.has("samples") ? json.get("samples").getAsInt() : calls, json.get("totalMs").getAsDouble(), perCall, json.has("medianMs") ? json.get("medianMs").getAsDouble() : perCall, json.get("minMs").getAsDouble(), json.get("maxMs").getAsDouble(), json.get("p95Ms").getAsDouble(), json.has("cvPercent") ? json.get("cvPercent").getAsDouble() : 0, json.has("allocBytesPerCall") ? json.get("allocBytesPerCall").getAsDouble() : -1);
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
        json.addProperty("samples", samples);
        json.addProperty("totalMs", totalMs);
        json.addProperty("perCallMs", perCallMs);
        json.addProperty("medianMs", medianMs);
        json.addProperty("minMs", minMs);
        json.addProperty("maxMs", maxMs);
        json.addProperty("p95Ms", p95Ms);
        json.addProperty("cvPercent", cvPercent);
        json.addProperty("allocBytesPerCall", allocBytesPerCall);
        return json;
    }
}
