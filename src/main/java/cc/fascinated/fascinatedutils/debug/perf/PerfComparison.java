package cc.fascinated.fascinatedutils.debug.perf;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Compares two {@link PerfReport}s test by test.
 */
public class PerfComparison {

    /**
     * One compared test. A negative {@code deltaPercent} means the test got faster.
     *
     * @param id                 the test id
     * @param beforePerCallMs    mean per-call time in the before report
     * @param afterPerCallMs     mean per-call time in the after report
     * @param beforeTotalMs      total time in the before report
     * @param afterTotalMs       total time in the after report
     * @param deltaPercent       change in per-call time, as a percentage of the before value
     */
    public record Row(String id, double beforePerCallMs, double afterPerCallMs, double beforeTotalMs, double afterTotalMs, double deltaPercent) {
    }

    /**
     * Pairs up results by id and computes the change in per-call time. Ids present in only one report are skipped.
     *
     * @param before the baseline report
     * @param after  the report to judge against the baseline
     * @return one row per id present in both reports, in the order of the after report
     */
    public List<Row> compare(PerfReport before, PerfReport after) {
        Map<String, PerfResult> beforeById = new HashMap<>();
        for (PerfResult result : before.results()) {
            beforeById.put(result.id(), result);
        }
        List<Row> rows = new ArrayList<>();
        for (PerfResult afterResult : after.results()) {
            PerfResult beforeResult = beforeById.get(afterResult.id());
            if (beforeResult == null) {
                continue;
            }
            double deltaPercent = beforeResult.perCallMs() == 0 ? 0 : (afterResult.perCallMs() - beforeResult.perCallMs()) / beforeResult.perCallMs() * 100.0;
            rows.add(new Row(afterResult.id(), beforeResult.perCallMs(), afterResult.perCallMs(), beforeResult.totalMs(), afterResult.totalMs(), deltaPercent));
        }
        return rows;
    }
}
