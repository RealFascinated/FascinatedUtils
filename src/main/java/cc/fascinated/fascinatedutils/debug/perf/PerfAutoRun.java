package cc.fascinated.fascinatedutils.debug.perf;

import cc.fascinated.fascinatedutils.client.Client;
import cc.fascinated.fascinatedutils.event.FascinatedEventBus;
import cc.fascinated.fascinatedutils.event.impl.lifecycle.ClientStartedEvent;
import meteordevelopment.orbit.EventHandler;

import java.util.List;
import java.util.Locale;

/**
 * Runs the suite headlessly when the client starts, for scripts and agents. Controlled by JVM system properties:
 *
 * <ul>
 *   <li>{@code alumite.perf.run=<label>} runs the micro-benchmarks and saves the report as {@code <label>}</li>
 *   <li>{@code alumite.perf.filter=<text>} (optional) only runs tests whose id contains the text</li>
 *   <li>{@code alumite.perf.compare=<baselineLabel>} (optional) also prints a comparison against that saved report</li>
 *   <li>{@code alumite.perf.exit=true} (optional) closes the game when finished</li>
 * </ul>
 *
 * Output is printed to stdout as lines prefixed {@code ALUMITE_PERF}, ending with {@code ALUMITE_PERF_DONE}.
 */
public class PerfAutoRun {
    public static final PerfAutoRun INSTANCE = new PerfAutoRun();

    private static final String PROPERTY_RUN = "alumite.perf.run";
    private static final String PROPERTY_FILTER = "alumite.perf.filter";
    private static final String PROPERTY_COMPARE = "alumite.perf.compare";
    private static final String PROPERTY_EXIT = "alumite.perf.exit";
    private static final String PREFIX = "ALUMITE_PERF ";

    /**
     * Subscribes to client start when the run property is set.
     */
    public void register() {
        if (System.getProperty(PROPERTY_RUN) != null) {
            FascinatedEventBus.INSTANCE.subscribe(this);
        }
    }

    @EventHandler
    private void alumite$onClientStarted(ClientStartedEvent event) {
        String label = System.getProperty(PROPERTY_RUN);
        PerfSuite.RunOutcome outcome = PerfSuite.INSTANCE.run(label, System.getProperty(PROPERTY_FILTER), true);
        for (PerfResult result : outcome.report().results()) {
            print(String.format(Locale.ROOT, "%s perCallMs=%.4f totalMs=%.4f calls=%d p95Ms=%.4f maxMs=%.4f", result.id(), result.perCallMs(), result.totalMs(), result.calls(), result.p95Ms(), result.maxMs()));
        }
        for (String failure : outcome.failures()) {
            print("FAILED " + failure);
        }
        if (outcome.file() != null) {
            print("report " + outcome.file());
        }
        String baseline = System.getProperty(PROPERTY_COMPARE);
        if (baseline != null) {
            printComparison(baseline, label);
        }
        System.out.println("ALUMITE_PERF_DONE");
        if (Boolean.getBoolean(PROPERTY_EXIT)) {
            event.minecraftClient().stop();
        }
    }

    private void printComparison(String baseline, String label) {
        PerfSuite.INSTANCE.compare(baseline, label).ifPresentOrElse(rows -> printRows(baseline, label, rows), () -> print("compare failed: missing report " + baseline));
    }

    private void printRows(String baseline, String label, List<PerfComparison.Row> rows) {
        for (PerfComparison.Row row : rows) {
            print(String.format(Locale.ROOT, "compare %s->%s %s before=%.4f after=%.4f deltaPercent=%+.1f", baseline, label, row.id(), row.beforePerCallMs(), row.afterPerCallMs(), row.deltaPercent()));
        }
    }

    private void print(String line) {
        System.out.println(PREFIX + line);
        Client.LOG.info("[perf] {}", line);
    }
}
