package cc.fascinated.fascinatedutils.debug.perf.report;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Compares two {@link PerfReport}s test by test, deciding per test whether a change is larger than the noise. Reports
 * from different scenarios are refused: the result has no rows and a {@code REFUSED} warning.
 */
public class PerfComparison {
    private static final double MIN_NOISE_PERCENT = 5.0;
    private static final double STANDARD_ERRORS = 3.0;

    /**
     * One compared test. A negative {@code deltaPercent} means the test got faster.
     *
     * @param id                  the test id
     * @param beforeMedianMs      median per-call time in the before report
     * @param afterMedianMs       median per-call time in the after report
     * @param beforeTotalMs       total time in the before report
     * @param afterTotalMs        total time in the after report
     * @param beforeAllocBytes    bytes allocated per call in the before report, or -1
     * @param afterAllocBytes     bytes allocated per call in the after report, or -1
     * @param deltaPercent        change in median time, as a percentage of the before value
     * @param noisePercent        the change that cannot be told apart from noise for this test
     * @param significant         true when the change is larger than the noise
     */
    public record Row(String id, double beforeMedianMs, double afterMedianMs, double beforeTotalMs, double afterTotalMs, double beforeAllocBytes, double afterAllocBytes, double deltaPercent, double noisePercent, boolean significant) {
    }

    /**
     * The rows plus every id that exists in only one report, so a renamed or removed test is not silently ignored.
     *
     * @param rows         one row per id present in both reports
     * @param onlyInBefore ids missing from the after report
     * @param onlyInAfter  ids missing from the before report
     * @param warnings     problems that make the comparison less trustworthy, such as differing scenarios
     */
    public record Result(List<Row> rows, List<String> onlyInBefore, List<String> onlyInAfter, List<String> warnings) {
    }

    /**
     * Pairs up results by id and computes the change in median per-call time.
     *
     * @param before the baseline report
     * @param after  the report to judge against the baseline
     * @return the comparison, with no rows when the scenarios differ
     */
    public Result compare(PerfReport before, PerfReport after) {
        String beforeScenario = before.meta().get(PerfReport.META_SCENARIO);
        String afterScenario = after.meta().get(PerfReport.META_SCENARIO);
        if (beforeScenario != null && afterScenario != null && !beforeScenario.equals(afterScenario)) {
            // Numbers from different scenarios measure different work, so no row would mean anything.
            return new Result(List.of(), List.of(), List.of(), List.of("REFUSED: scenario differs (" + beforeScenario + " vs " + afterScenario + "); run both sides with the same scenario"));
        }
        Map<String, PerfResult> beforeById = new HashMap<>();
        for (PerfResult result : before.results()) {
            beforeById.put(result.id(), result);
        }
        List<Row> rows = new ArrayList<>();
        List<String> onlyInAfter = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (PerfResult afterResult : after.results()) {
            PerfResult beforeResult = beforeById.get(afterResult.id());
            if (beforeResult == null) {
                onlyInAfter.add(afterResult.id());
                continue;
            }
            seen.add(afterResult.id());
            rows.add(toRow(beforeResult, afterResult, isMerged(before), isMerged(after)));
        }
        List<String> onlyInBefore = before.results().stream().map(PerfResult::id).filter(id -> !seen.contains(id)).toList();
        return new Result(rows, onlyInBefore, onlyInAfter, warnings(before, after));
    }

    private boolean isMerged(PerfReport report) {
        try {
            return Integer.parseInt(report.meta().getOrDefault("launches", "1")) > 1;
        } catch (NumberFormatException exception) {
            return false;
        }
    }

    private double noiseError(PerfResult result, boolean merged) {
        // Merged results hold the spread of per-launch medians (samples = launches), which already is the run-to-run
        // noise, so it is used as is. Single launches hold the spread of samples inside one run, scaled to a standard error.
        return merged ? result.cvPercent() : result.cvPercent() / Math.sqrt(Math.max(1, result.samples()));
    }

    private Row toRow(PerfResult before, PerfResult after, boolean beforeMerged, boolean afterMerged) {
        double deltaPercent;
        if (before.medianMs() == 0) {
            // No baseline to scale against: any growth is an unbounded increase, no growth is no change.
            deltaPercent = after.medianMs() > 0 ? Double.POSITIVE_INFINITY : 0;
        } else {
            deltaPercent = (after.medianMs() - before.medianMs()) / before.medianMs() * 100.0;
        }
        double beforeError = noiseError(before, beforeMerged);
        double afterError = noiseError(after, afterMerged);
        double noisePercent = Math.max(MIN_NOISE_PERCENT, STANDARD_ERRORS * Math.hypot(beforeError, afterError));
        return new Row(before.id(), before.medianMs(), after.medianMs(), before.totalMs(), after.totalMs(), before.allocBytesPerCall(), after.allocBytesPerCall(), deltaPercent, noisePercent, Math.abs(deltaPercent) > noisePercent);
    }

    private List<String> warnings(PerfReport before, PerfReport after) {
        List<String> warnings = new ArrayList<>();
        for (String key : List.of("scenes", "measureTicks", "worldType", "moduleMode", "cpus", "gc")) {
            String beforeValue = before.meta().get(key);
            String afterValue = after.meta().get(key);
            if (beforeValue != null && afterValue != null && !beforeValue.equals(afterValue)) {
                warnings.add(key + " differs: " + beforeValue + " vs " + afterValue);
            }
        }
        return warnings;
    }
}
