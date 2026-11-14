package cc.fascinated.fascinatedutils.debug.perf.report;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.ToDoubleFunction;

/**
 * Merges several reports of the same scenario (separate game launches) into one. Launch-to-launch variance (JIT
 * decisions, memory layout) is usually larger than variance inside one launch, so the merged spread is measured
 * across launches.
 */
public class PerfAggregate {

    /**
     * Merges reports by taking the median of every statistic across launches.
     *
     * @param label   the label for the merged report
     * @param reports the per-launch reports, at least one
     * @return the merged report
     */
    public PerfReport merge(String label, List<PerfReport> reports) {
        Map<String, List<PerfResult>> byId = new LinkedHashMap<>();
        for (PerfReport report : reports) {
            for (PerfResult result : report.results()) {
                byId.computeIfAbsent(result.id(), key -> new ArrayList<>()).add(result);
            }
        }
        List<PerfResult> merged = new ArrayList<>();
        for (Map.Entry<String, List<PerfResult>> entry : byId.entrySet()) {
            merged.add(mergeResults(entry.getKey(), entry.getValue()));
        }
        PerfReport first = reports.getFirst();
        Map<String, String> meta = new LinkedHashMap<>(first.meta());
        meta.put("launches", String.valueOf(reports.size()));
        return new PerfReport(label, first.timestampMillis(), first.modVersion(), first.gameVersion(), first.javaVersion(), meta, merged);
    }

    private PerfResult mergeResults(String id, List<PerfResult> results) {
        if (results.size() == 1) {
            return results.getFirst();
        }
        double[] medians = results.stream().mapToDouble(PerfResult::medianMs).toArray();
        double medianOfMedians = median(medians);
        double mean = Arrays.stream(medians).average().orElse(0);
        double squaredDeviation = Arrays.stream(medians).map(value -> (value - mean) * (value - mean)).sum();
        double cvPercent = mean == 0 ? 0 : Math.sqrt(squaredDeviation / (medians.length - 1)) / mean * 100.0;
        return new PerfResult(id, (long) median(results, result -> result.calls()), results.size(), median(results, PerfResult::totalMs), median(results, PerfResult::perCallMs), medianOfMedians, results.stream().mapToDouble(PerfResult::minMs).min().orElse(0), results.stream().mapToDouble(PerfResult::maxMs).max().orElse(0), median(results, PerfResult::p95Ms), cvPercent, mergeAllocation(results));
    }

    private double mergeAllocation(List<PerfResult> results) {
        // -1 means "not measured" in that launch, so it must not drag the median down.
        double[] measured = results.stream().mapToDouble(PerfResult::allocBytesPerCall).filter(value -> value >= 0).toArray();
        return measured.length == 0 ? -1 : median(measured);
    }

    private double median(List<PerfResult> results, ToDoubleFunction<PerfResult> selector) {
        return median(results.stream().mapToDouble(selector).toArray());
    }

    private double median(double[] values) {
        double[] sorted = values.clone();
        Arrays.sort(sorted);
        return sorted.length % 2 == 1 ? sorted[sorted.length / 2] : (sorted[sorted.length / 2 - 1] + sorted[sorted.length / 2]) / 2.0;
    }
}
