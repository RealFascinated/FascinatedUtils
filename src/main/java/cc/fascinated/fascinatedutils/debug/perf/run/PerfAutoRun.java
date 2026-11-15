package cc.fascinated.fascinatedutils.debug.perf.run;

import cc.fascinated.fascinatedutils.client.Client;
import cc.fascinated.fascinatedutils.debug.perf.PerfMode;
import cc.fascinated.fascinatedutils.debug.perf.PerfOut;
import cc.fascinated.fascinatedutils.debug.perf.PerfProperties;
import cc.fascinated.fascinatedutils.debug.perf.PerfSuite;
import cc.fascinated.fascinatedutils.debug.perf.core.PerfGcSnapshot;
import cc.fascinated.fascinatedutils.debug.perf.core.PerfProbe;
import cc.fascinated.fascinatedutils.debug.perf.coverage.PerfCoverage;
import cc.fascinated.fascinatedutils.debug.perf.report.PerfComparison;
import cc.fascinated.fascinatedutils.debug.perf.report.PerfReport;
import cc.fascinated.fascinatedutils.debug.perf.report.PerfResult;
import cc.fascinated.fascinatedutils.event.FascinatedEventBus;
import cc.fascinated.fascinatedutils.event.impl.ClientTickEvent;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;

import java.util.Locale;

/**
 * Runs the suite headlessly for scripts and agents, driven by JVM system properties.
 *
 * <p>Micro-benchmarks, run on the title screen once loading has finished:
 * <ul>
 *   <li>{@code alumite.perf.run=<label>} runs the micro-benchmarks and saves the report as {@code <label>}</li>
 *   <li>{@code alumite.perf.filter=<text>} (optional) only runs tests whose id contains the text</li>
 * </ul>
 *
 * <p>Headless checks, also run on the title screen:
 * <ul>
 *   <li>{@code alumite.perf.coverage=true} prints the coverage inventory, what covers each entry, the exclusions and
 *       the uncovered list</li>
 *   <li>{@code alumite.perf.compareReports=<before>,<after>} only compares two saved reports (used by
 *       {@code scripts/perf-compare.sh})</li>
 * </ul>
 *
 * <p>Shared by both modes:
 * <ul>
 *   <li>{@code alumite.perf.compare=<baselineLabel>} (optional) also prints a comparison against that saved report</li>
 *   <li>{@code alumite.perf.exit=true} (optional) closes the game when finished</li>
 *   <li>{@code alumite.perf.reportDir=<path>} (optional) reads and writes reports there instead of the config folder</li>
 * </ul>
 *
 * The in-world runs ({@code alumite.perf.scenario=<name>} or {@code alumite.perf.world=<label>}) are described on
 * {@link PerfWorldScenario}; when one is requested the title-screen modes above are skipped. Output is
 * printed to stdout as lines prefixed {@code ALUMITE_PERF}, ending with {@code ALUMITE_PERF_DONE}.
 */
public class PerfAutoRun {
    public static final PerfAutoRun INSTANCE = new PerfAutoRun();

    private static final int READY_SETTLE_TICKS = 100;

    private int readyTicks;
    private boolean finished;

    /**
     * Subscribes the requested automatic runs.
     */
    public void register() {
        PerfProbe.INSTANCE.register();
        boolean titleScreenMode = System.getProperty(PerfProperties.RUN) != null || Boolean.getBoolean(PerfProperties.COVERAGE) || System.getProperty(PerfProperties.COMPARE_REPORTS) != null;
        if (titleScreenMode && !PerfMode.isWorldScenario()) {
            FascinatedEventBus.INSTANCE.subscribe(this);
        }
        PerfWorldScenario.INSTANCE.register();
    }

    /**
     * Prints a comparison against {@code alumite.perf.compare} when that property is set.
     *
     * @param label the report that was just produced
     */
    public void printComparisonIfRequested(String label) {
        String baseline = System.getProperty(PerfProperties.COMPARE);
        if (baseline != null) {
            printComparison(baseline, label);
        }
    }

    /**
     * Prints the {@code gc.<id>.count} and {@code gc.<id>.timeMs} meta of a report as {@code gc <id> ...} lines.
     *
     * @param report the report whose GC meta to print
     */
    public void printGcLines(PerfReport report) {
        report.meta().forEach((key, value) -> {
            if (PerfGcSnapshot.isCountKey(key)) {
                String resultId = PerfGcSnapshot.resultIdOfCountKey(key);
                PerfOut.INSTANCE.printAndLog("gc " + resultId + " count=" + value + " timeMs=" + report.meta().getOrDefault(PerfGcSnapshot.timeKey(resultId), "?"));
            }
        });
    }

    private void printComparison(String baseline, String label) {
        PerfSuite.INSTANCE.compare(baseline, label).ifPresentOrElse(result -> printResult(baseline, label, result), () -> PerfOut.INSTANCE.printAndLog("compare failed: missing report " + baseline + " or " + label));
    }

    @EventHandler
    private void alumite$onClientTick(ClientTickEvent event) {
        if (finished) {
            return;
        }
        Minecraft minecraft = event.minecraftClient();
        boolean ready = minecraft.getOverlay() == null && minecraft.screen instanceof TitleScreen;
        readyTicks = ready ? readyTicks + 1 : 0;
        if (readyTicks < READY_SETTLE_TICKS) {
            return;
        }
        finished = true;
        if (Boolean.getBoolean(PerfProperties.COVERAGE)) {
            PerfCoverage.INSTANCE.printToStdout(PerfCoverage.INSTANCE.check());
        }
        String compareReports = System.getProperty(PerfProperties.COMPARE_REPORTS);
        if (compareReports != null) {
            String[] labels = compareReports.split(",");
            if (labels.length == 2) {
                printComparison(labels[0].trim(), labels[1].trim());
            } else {
                PerfOut.INSTANCE.printAndLog("FAILED " + PerfProperties.COMPARE_REPORTS + " must be <before>,<after>, got " + compareReports);
            }
        }
        try {
            if (System.getProperty(PerfProperties.RUN) != null) {
                runMicroBenchmarks();
            }
        } catch (Throwable throwable) {
            Client.LOG.error("[perf] micro-benchmark run failed", throwable);
            PerfOut.INSTANCE.printAndLog("FAILED run: " + throwable);
        } finally {
            PerfOut.INSTANCE.done();
            if (Boolean.getBoolean(PerfProperties.EXIT)) {
                minecraft.execute(minecraft::stop);
            }
        }
    }

    private void runMicroBenchmarks() {
        String label = System.getProperty(PerfProperties.RUN);
        PerfSuite.RunOutcome outcome = PerfSuite.INSTANCE.run(label, System.getProperty(PerfProperties.FILTER), true);
        for (PerfResult result : outcome.report().results()) {
            PerfOut.INSTANCE.printResult(result);
        }
        printGcLines(outcome.report());
        for (String failure : outcome.failures()) {
            PerfOut.INSTANCE.printAndLog("FAILED " + failure);
        }
        if (outcome.file() != null) {
            PerfOut.INSTANCE.printAndLog("report " + outcome.file());
        }
        printComparisonIfRequested(label);
    }

    private void printResult(String baseline, String label, PerfComparison.Result result) {
        result.warnings().forEach(warning -> PerfOut.INSTANCE.printAndLog("compare warning " + warning));
        for (PerfComparison.Row row : result.rows()) {
            PerfOut.INSTANCE.printAndLog(String.format(Locale.ROOT, "compare %s->%s %s beforeMedianMs=%.6f afterMedianMs=%.6f deltaPercent=%+.1f noisePercent=%.1f pValue=%.4f significant=%b", baseline, label, row.id(), row.beforeMedianMs(), row.afterMedianMs(), row.deltaPercent(), row.noisePercent(), row.pValue(), row.significant()));
        }
        result.onlyInBefore().forEach(id -> PerfOut.INSTANCE.printAndLog("compare only-in-before " + id));
        result.onlyInAfter().forEach(id -> PerfOut.INSTANCE.printAndLog("compare only-in-after " + id));
    }
}
