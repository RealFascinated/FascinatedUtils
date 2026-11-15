package cc.fascinated.fascinatedutils.debug.perf.report;

import com.google.gson.JsonArray;
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
 * @param p99Ms             the 99th percentile sample
 * @param p999Ms            the 99.9th percentile sample
 * @param stutters          how many samples took more than twice the median
 * @param quantilesMs       evenly spaced per-call quantiles in milliseconds for significance tests, or none when the
 *                          report predates them
 */
public record PerfResult(String id, long calls, int samples, double totalMs, double perCallMs, double medianMs, double minMs, double maxMs, double p95Ms, double cvPercent, double allocBytesPerCall, double p99Ms, double p999Ms, int stutters, double[] quantilesMs) {
    private static final double NANOS_PER_MILLI = 1_000_000.0;
    private static final double STUTTER_FACTOR = 2.0;
    private static final int QUANTILE_COUNT = 200;

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
            return new PerfResult(id, 0, 0, 0, 0, 0, 0, 0, 0, 0, allocBytesPerCall, 0, 0, 0, new double[0]);
        }
        double[] sorted = nanosPerCall.clone();
        Arrays.sort(sorted);
        double sum = 0;
        for (double sample : sorted) {
            sum += sample;
        }
        double mean = sum / sorted.length;
        double standardDeviation = standardDeviation(sorted, mean);
        int percentileIndex = Math.max(0, (int) Math.ceil(sorted.length * 0.95) - 1);
        double median = medianOfSorted(sorted);
        int stutters = 0;
        for (double sample : sorted) {
            if (sample > median * STUTTER_FACTOR) {
                stutters++;
            }
        }
        return new PerfResult(id, sorted.length * callsPerSample, sorted.length, sum * callsPerSample / NANOS_PER_MILLI, mean / NANOS_PER_MILLI, median / NANOS_PER_MILLI, sorted[0] / NANOS_PER_MILLI, sorted[sorted.length - 1] / NANOS_PER_MILLI, sorted[percentileIndex] / NANOS_PER_MILLI, cvPercent(mean, standardDeviation), allocBytesPerCall, percentile(sorted, 0.99) / NANOS_PER_MILLI, percentile(sorted, 0.999) / NANOS_PER_MILLI, stutters, evenlySpacedQuantiles(sorted, NANOS_PER_MILLI));
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
     * Picks evenly spaced quantiles from sorted values. With fewer values than the quantile count, every value is kept.
     *
     * @param sortedValues the values in ascending order
     * @param divisor      the value each picked element is divided by, for unit conversion (1 keeps it as is)
     * @return the quantiles, at most {@value #QUANTILE_COUNT} of them
     */
    static double[] evenlySpacedQuantiles(double[] sortedValues, double divisor) {
        int count = Math.min(QUANTILE_COUNT, sortedValues.length);
        double[] quantiles = new double[count];
        for (int index = 0; index < count; index++) {
            int valueIndex = sortedValues.length <= QUANTILE_COUNT ? index : (int) ((index + 0.5) * sortedValues.length / QUANTILE_COUNT);
            quantiles[index] = sortedValues[valueIndex] / divisor;
        }
        return quantiles;
    }

    private static double percentile(double[] sorted, double fraction) {
        return sorted[Math.max(0, (int) Math.ceil(sorted.length * fraction) - 1)];
    }

    static double median(double[] values) {
        double[] sorted = values.clone();
        Arrays.sort(sorted);
        return medianOfSorted(sorted);
    }

    static double medianOfSorted(double[] sorted) {
        return sorted.length % 2 == 1 ? sorted[sorted.length / 2] : (sorted[sorted.length / 2 - 1] + sorted[sorted.length / 2]) / 2.0;
    }

    static double mean(double[] values) {
        double sum = 0;
        for (double value : values) {
            sum += value;
        }
        return sum / values.length;
    }

    static double standardDeviation(double[] values, double mean) {
        double squaredDeviation = 0;
        for (double value : values) {
            squaredDeviation += (value - mean) * (value - mean);
        }
        return values.length > 1 ? Math.sqrt(squaredDeviation / (values.length - 1)) : 0;
    }

    static double cvPercent(double mean, double standardDeviation) {
        return mean == 0 ? 0 : standardDeviation / mean * 100.0;
    }

    /**
     * Copies this result under a different id.
     *
     * @param newId the id for the copy
     * @return the renamed result
     */
    public PerfResult withId(String newId) {
        return new PerfResult(newId, calls, samples, totalMs, perCallMs, medianMs, minMs, maxMs, p95Ms, cvPercent, allocBytesPerCall, p99Ms, p999Ms, stutters, quantilesMs);
    }

    /**
     * Reads a result previously written by {@link #toJson()}. Fields missing from older reports fall back to the mean,
     * and reports without quantiles get none.
     *
     * @param json the serialised result
     * @return the parsed result
     */
    public static PerfResult fromJson(JsonObject json) {
        double perCall = json.get("perCallMs").getAsDouble();
        int calls = json.get("calls").getAsInt();
        return new PerfResult(json.get("id").getAsString(), calls, json.has("samples") ? json.get("samples").getAsInt() : calls, json.get("totalMs").getAsDouble(), perCall, json.has("medianMs") ? json.get("medianMs").getAsDouble() : perCall, json.get("minMs").getAsDouble(), json.get("maxMs").getAsDouble(), json.get("p95Ms").getAsDouble(), json.has("cvPercent") ? json.get("cvPercent").getAsDouble() : 0, json.has("allocBytesPerCall") ? json.get("allocBytesPerCall").getAsDouble() : -1, json.has("p99Ms") ? json.get("p99Ms").getAsDouble() : 0, json.has("p999Ms") ? json.get("p999Ms").getAsDouble() : 0, json.has("stutters") ? json.get("stutters").getAsInt() : 0, readQuantiles(json));
    }

    private static double[] readQuantiles(JsonObject json) {
        if (!json.has("quantilesMs")) {
            return new double[0];
        }
        JsonArray array = json.getAsJsonArray("quantilesMs");
        double[] quantiles = new double[array.size()];
        for (int index = 0; index < quantiles.length; index++) {
            quantiles[index] = array.get(index).getAsDouble();
        }
        return quantiles;
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
        json.addProperty("p99Ms", p99Ms);
        json.addProperty("p999Ms", p999Ms);
        json.addProperty("stutters", stutters);
        JsonArray quantiles = new JsonArray();
        for (double quantile : quantilesMs) {
            quantiles.add(quantile);
        }
        json.add("quantilesMs", quantiles);
        return json;
    }
}
