package cc.fascinated.fascinatedutils.debug.perf.run;

import cc.fascinated.fascinatedutils.client.Client;
import cc.fascinated.fascinatedutils.debug.perf.PerfMode;
import cc.fascinated.fascinatedutils.debug.perf.PerfSuite;
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

    private static final String PROPERTY_RUN = PerfMode.PROPERTY_PREFIX + "run";
    private static final String PROPERTY_FILTER = PerfMode.PROPERTY_PREFIX + "filter";
    private static final String PROPERTY_COMPARE = PerfMode.PROPERTY_PREFIX + "compare";
    private static final String PROPERTY_EXIT = PerfMode.PROPERTY_PREFIX + "exit";
    private static final String PROPERTY_COVERAGE = PerfMode.PROPERTY_PREFIX + "coverage";
    private static final String PROPERTY_COMPARE_REPORTS = PerfMode.PROPERTY_PREFIX + "compareReports";
    private static final String GC_META_PREFIX = "gc.";
    private static final String PREFIX = "ALUMITE_PERF ";
    private static final int READY_SETTLE_TICKS = 100;
    private static final int READY_TIMEOUT_TICKS = 6_000;

    private int readyTicks;
    private boolean finished;

    /**
     * Subscribes the requested automatic runs.
     */
    public void register() {
        PerfProbe.INSTANCE.register();
        boolean titleScreenMode = System.getProperty(PROPERTY_RUN) != null || Boolean.getBoolean(PROPERTY_COVERAGE) || System.getProperty(PROPERTY_COMPARE_REPORTS) != null;
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
        String baseline = System.getProperty(PROPERTY_COMPARE);
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
            if (key.startsWith(GC_META_PREFIX) && key.endsWith(".count")) {
                String resultId = key.substring(GC_META_PREFIX.length(), key.length() - ".count".length());
                print("gc " + resultId + " count=" + value + " timeMs=" + report.meta().getOrDefault(GC_META_PREFIX + resultId + ".timeMs", "?"));
            }
        });
    }

    private void printComparison(String baseline, String label) {
        PerfSuite.INSTANCE.compare(baseline, label).ifPresentOrElse(result -> printResult(baseline, label, result), () -> print("compare failed: missing report " + baseline + " or " + label));
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
        if (Boolean.getBoolean(PROPERTY_COVERAGE)) {
            PerfCoverage.INSTANCE.printToStdout(PerfCoverage.INSTANCE.check());
        }
        String compareReports = System.getProperty(PROPERTY_COMPARE_REPORTS);
        if (compareReports != null) {
            String[] labels = compareReports.split(",");
            if (labels.length == 2) {
                printComparison(labels[0].trim(), labels[1].trim());
            } else {
                print("FAILED " + PROPERTY_COMPARE_REPORTS + " must be <before>,<after>, got " + compareReports);
            }
        }
        try {
            if (System.getProperty(PROPERTY_RUN) != null) {
                runMicroBenchmarks();
            }
        } catch (Throwable throwable) {
            Client.LOG.error("[perf] micro-benchmark run failed", throwable);
            print("FAILED run: " + throwable);
        } finally {
            finished = true;
            System.out.println("ALUMITE_PERF_DONE");
            if (Boolean.getBoolean(PROPERTY_EXIT)) {
                minecraft.execute(minecraft::stop);
            }
        }
    }

    private void runMicroBenchmarks() {
        String label = System.getProperty(PROPERTY_RUN);
        PerfSuite.RunOutcome outcome = PerfSuite.INSTANCE.run(label, System.getProperty(PROPERTY_FILTER), true);
        for (PerfResult result : outcome.report().results()) {
            print(String.format(Locale.ROOT, "%s medianMs=%.6f perCallMs=%.6f totalMs=%.6f calls=%d p95Ms=%.6f maxMs=%.6f cvPercent=%.1f allocBytesPerCall=%.0f", result.id(), result.medianMs(), result.perCallMs(), result.totalMs(), result.calls(), result.p95Ms(), result.maxMs(), result.cvPercent(), result.allocBytesPerCall()));
        }
        printGcLines(outcome.report());
        for (String failure : outcome.failures()) {
            print("FAILED " + failure);
        }
        if (outcome.file() != null) {
            print("report " + outcome.file());
        }
        printComparisonIfRequested(label);
    }

    private void printResult(String baseline, String label, PerfComparison.Result result) {
        result.warnings().forEach(warning -> print("compare warning " + warning));
        for (PerfComparison.Row row : result.rows()) {
            print(String.format(Locale.ROOT, "compare %s->%s %s beforeMedianMs=%.6f afterMedianMs=%.6f deltaPercent=%+.1f noisePercent=%.1f significant=%b", baseline, label, row.id(), row.beforeMedianMs(), row.afterMedianMs(), row.deltaPercent(), row.noisePercent(), row.significant()));
        }
        result.onlyInBefore().forEach(id -> print("compare only-in-before " + id));
        result.onlyInAfter().forEach(id -> print("compare only-in-after " + id));
    }

    private void print(String line) {
        System.out.println(PREFIX + line);
        Client.LOG.info("[perf] {}", line);
    }
}
