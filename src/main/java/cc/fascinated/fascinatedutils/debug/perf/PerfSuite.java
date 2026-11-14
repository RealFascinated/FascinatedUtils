package cc.fascinated.fascinatedutils.debug.perf;

import cc.fascinated.fascinatedutils.debug.perf.core.PerfProbe;
import cc.fascinated.fascinatedutils.debug.perf.core.PerfRunner;
import cc.fascinated.fascinatedutils.debug.perf.core.PerfTest;
import cc.fascinated.fascinatedutils.debug.perf.core.PerfTestRegistry;
import cc.fascinated.fascinatedutils.debug.perf.report.PerfComparison;
import cc.fascinated.fascinatedutils.debug.perf.report.PerfReport;
import cc.fascinated.fascinatedutils.debug.perf.report.PerfReportStore;
import cc.fascinated.fascinatedutils.debug.perf.report.PerfResult;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Code entry point for the performance suite, used by the chat command, the launch-time auto-run and any scripted or
 * agentic workflow. Must be called on the client thread after the client has started.
 */
public class PerfSuite {
    public static final PerfSuite INSTANCE = new PerfSuite();

    private final PerfRunner runner = new PerfRunner();
    private final PerfComparison comparison = new PerfComparison();

    /**
     * The outcome of {@link #run(String, String, boolean)}.
     *
     * @param report   the measured report
     * @param file     where the report was saved, or null when saving was skipped or failed
     * @param failures one message per test that threw
     */
    public record RunOutcome(PerfReport report, Path file, List<String> failures) {
    }

    /**
     * Runs the matching micro-benchmarks and optionally saves the report.
     *
     * @param label  the report label
     * @param filter only tests whose id contains this text run; null or blank runs all
     * @param save   whether to write the report to the report store
     * @return the report, its file and any test failures
     */
    public RunOutcome run(String label, String filter, boolean save) {
        PerfTestRegistry.INSTANCE.initialize();
        List<String> failures = new ArrayList<>();
        Map<String, String> gcMeta = new LinkedHashMap<>();
        List<PerfResult> results = runner.run(PerfTestRegistry.INSTANCE.selectMicro(filter), "", failures::add, gcMeta);
        PerfReport report = PerfReport.create(label, "micro", results, gcMeta);
        Path file = null;
        if (save) {
            try {
                file = PerfReportStore.INSTANCE.save(report);
            } catch (IOException exception) {
                failures.add("save: " + exception);
            }
        }
        return new RunOutcome(report, file, failures);
    }

    /**
     * Records live frame, tick and HUD timings for a number of ticks.
     *
     * @param label         the report label
     * @param durationTicks how many client ticks to record
     * @param onFinished    called on the client thread with the saved-or-not report
     * @return false when a live session is already running
     */
    public boolean startLive(String label, int durationTicks, Consumer<RunOutcome> onFinished) {
        return PerfProbe.INSTANCE.start(durationTicks, results -> {
            PerfReport report = PerfReport.create(label, "live", results);
            List<String> failures = new ArrayList<>();
            Path file = null;
            try {
                file = PerfReportStore.INSTANCE.save(report);
            } catch (IOException exception) {
                failures.add("save: " + exception);
            }
            onFinished.accept(new RunOutcome(report, file, failures));
        });
    }

    /**
     * Compares two saved reports.
     *
     * @param beforeLabel the baseline report label
     * @param afterLabel  the report label to judge
     * @return the comparison, or empty when either report is missing
     */
    public Optional<PerfComparison.Result> compare(String beforeLabel, String afterLabel) {
        return compareReports(beforeLabel, afterLabel);
    }

    /**
     * Runs the given tests on the calling thread, naming results {@code <idPrefix><test id>}.
     *
     * @param tests       the tests to run
     * @param idPrefix    the result id prefix
     * @param failureSink receives one message per test that threw
     * @param gcMeta      receives per-test GC deltas
     * @return one result per test that completed
     */
    public List<PerfResult> runTests(List<PerfTest> tests, String idPrefix, Consumer<String> failureSink, Map<String, String> gcMeta) {
        return runner.run(tests, idPrefix, failureSink, gcMeta);
    }

    private Optional<PerfComparison.Result> compareReports(String beforeLabel, String afterLabel) {
        Optional<PerfReport> before = PerfReportStore.INSTANCE.load(beforeLabel);
        Optional<PerfReport> after = PerfReportStore.INSTANCE.load(afterLabel);
        if (before.isEmpty() || after.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(comparison.compare(before.get(), after.get()));
    }
}
