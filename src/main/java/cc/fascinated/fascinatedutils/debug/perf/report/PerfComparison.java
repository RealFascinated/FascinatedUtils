package cc.fascinated.fascinatedutils.debug.perf.report;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
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
    private static final double QUANTILE_MIN_EFFECT_PERCENT = 2.0;
    private static final double SIGNIFICANCE_LEVEL = 0.01;
    private static final double[] ERFC_COEFFICIENTS = {-1.26551223, 1.00002368, 0.37409196, 0.09678418, -0.18628806, 0.27886807, -1.13520398, 1.48851587, -0.82215223, 0.17087277};

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
     * @param pValue              two-sided Mann-Whitney p-value of the per-call quantiles, or NaN when not computed
     */
    public record Row(String id, double beforeMedianMs, double afterMedianMs, double beforeTotalMs, double afterTotalMs, double beforeAllocBytes, double afterAllocBytes, double deltaPercent, double noisePercent, boolean significant, double pValue) {
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
        if (hasQuantiles(before) && hasQuantiles(after)) {
            // Between-launch spread only exists when a side was merged from several launches.
            double launchNoisePercent = beforeMerged || afterMerged ? STANDARD_ERRORS * Math.hypot(beforeError, afterError) : 0;
            return quantileRow(before, after, deltaPercent, launchNoisePercent);
        }
        double noisePercent = Math.max(MIN_NOISE_PERCENT, STANDARD_ERRORS * Math.hypot(beforeError, afterError));
        return new Row(before.id(), before.medianMs(), after.medianMs(), before.totalMs(), after.totalMs(), before.allocBytesPerCall(), after.allocBytesPerCall(), deltaPercent, noisePercent, Math.abs(deltaPercent) > noisePercent, Double.NaN);
    }

    private Row quantileRow(PerfResult before, PerfResult after, double deltaPercent, double launchNoisePercent) {
        double pValue = mannWhitneyPValue(before.quantilesMs(), after.quantilesMs());
        double thresholdPercent = Math.max(QUANTILE_MIN_EFFECT_PERCENT, launchNoisePercent);
        boolean significant = pValue < SIGNIFICANCE_LEVEL && Math.abs(deltaPercent) > thresholdPercent;
        return new Row(before.id(), before.medianMs(), after.medianMs(), before.totalMs(), after.totalMs(), before.allocBytesPerCall(), after.allocBytesPerCall(), deltaPercent, thresholdPercent, significant, pValue);
    }

    private boolean hasQuantiles(PerfResult result) {
        return result.quantilesMs().length > 0;
    }

    /**
     * Two-sided Mann-Whitney U test with a normal approximation, a continuity correction and a tie correction.
     *
     * @param firstSample  the first sample of values
     * @param secondSample the second sample of values, at least one value
     * @return the two-sided p-value, 1 when the samples cannot be told apart
     */
    private double mannWhitneyPValue(double[] firstSample, double[] secondSample) {
        int firstCount = firstSample.length;
        int secondCount = secondSample.length;
        int totalCount = firstCount + secondCount;
        double[] pooled = new double[totalCount];
        System.arraycopy(firstSample, 0, pooled, 0, firstCount);
        System.arraycopy(secondSample, 0, pooled, firstCount, secondCount);
        Integer[] order = new Integer[totalCount];
        for (int index = 0; index < totalCount; index++) {
            order[index] = index;
        }
        Arrays.sort(order, Comparator.comparingDouble(index -> pooled[index]));

        double firstRankSum = 0;
        double tieTerm = 0;
        int groupStart = 0;
        while (groupStart < totalCount) {
            int groupEnd = groupStart;
            while (groupEnd < totalCount && pooled[order[groupEnd]] == pooled[order[groupStart]]) {
                groupEnd++;
            }
            int groupSize = groupEnd - groupStart;
            // Tied values share the average of the 1-based ranks they span.
            double averageRank = (groupStart + 1 + groupEnd) / 2.0;
            tieTerm += (double) groupSize * groupSize * groupSize - groupSize;
            for (int position = groupStart; position < groupEnd; position++) {
                if (order[position] < firstCount) {
                    firstRankSum += averageRank;
                }
            }
            groupStart = groupEnd;
        }

        double meanU = (double) firstCount * secondCount / 2.0;
        double firstU = firstRankSum - firstCount * (firstCount + 1) / 2.0;
        double varianceU = (double) firstCount * secondCount / 12.0 * ((totalCount + 1) - tieTerm / ((double) totalCount * (totalCount - 1)));
        if (varianceU <= 0) {
            return 1.0;
        }
        double continuityDeviation = Math.max(0, Math.abs(firstU - meanU) - 0.5);
        double z = continuityDeviation / Math.sqrt(varianceU);
        // Two-sided normal tail probability: 2 * (1 - Phi(z)) = erfc(z / sqrt(2)).
        return complementaryErrorFunction(z / Math.sqrt(2.0));
    }

    /**
     * Complementary error function with a fractional error below 1.2e-7 (Chebyshev fit).
     *
     * @param x the argument, zero or positive here
     * @return erfc(x)
     */
    private double complementaryErrorFunction(double x) {
        double t = 1.0 / (1.0 + 0.5 * Math.abs(x));
        double polynomial = ERFC_COEFFICIENTS[ERFC_COEFFICIENTS.length - 1];
        for (int index = ERFC_COEFFICIENTS.length - 2; index >= 0; index--) {
            polynomial = ERFC_COEFFICIENTS[index] + t * polynomial;
        }
        double result = t * Math.exp(-x * x + polynomial);
        return x >= 0 ? result : 2.0 - result;
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
