package cc.fascinated.fascinatedutils.debug.perf;

import cc.fascinated.fascinatedutils.client.Client;
import cc.fascinated.fascinatedutils.client.command.annotation.Arg;
import cc.fascinated.fascinatedutils.client.command.annotation.ClientCommand;
import cc.fascinated.fascinatedutils.client.command.annotation.Subcommand;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Chat commands for the performance suite.
 *
 * <pre>
 * /perf run [label] [filter]     run the micro-benchmarks and save them as a report (default label: latest)
 * /perf live [label] [seconds]   record real frames, ticks and HUD widgets while you play (default: live, 30s)
 * /perf compare before after     compare two saved reports
 * /perf list                     list saved reports
 * /perf tests                    list registered tests
 * </pre>
 *
 * <p>Typical before/after workflow: {@code /perf run before}, make the change, {@code /perf run after},
 * {@code /perf compare before after}.
 */
@ClientCommand(value = "perf", aliases = {"alumiteperf"})
public class PerfCommand {
    private static final String DEFAULT_RUN_LABEL = "latest";
    private static final String DEFAULT_LIVE_LABEL = "live";
    private static final int DEFAULT_LIVE_SECONDS = 30;
    private static final int TICKS_PER_SECOND = 20;
    private static final double NOISE_THRESHOLD_PERCENT = 5.0;

    @Subcommand({"run"})
    public static int run(FabricClientCommandSource source) {
        return run(source, DEFAULT_RUN_LABEL, "");
    }

    @Subcommand({"run"})
    public static int run(FabricClientCommandSource source, @Arg("label") String label) {
        return run(source, label, "");
    }

    @Subcommand({"run"})
    public static int run(FabricClientCommandSource source, @Arg("label") String label, @Arg("filter") String filter) {
        PerfTestRegistry.INSTANCE.initialize();
        int matching = PerfTestRegistry.INSTANCE.select(filter).size();
        if (matching == 0) {
            source.sendError(Component.literal("No perf tests match '" + filter + "'"));
            return 0;
        }
        source.sendFeedback(Component.literal("Running " + matching + " perf tests, the game will pause briefly...").withStyle(ChatFormatting.GRAY));
        PerfSuite.RunOutcome outcome = PerfSuite.INSTANCE.run(label, filter, true);
        report(source, outcome);
        return 1;
    }

    @Subcommand({"live"})
    public static int live(FabricClientCommandSource source) {
        return live(source, DEFAULT_LIVE_LABEL, DEFAULT_LIVE_SECONDS);
    }

    @Subcommand({"live"})
    public static int live(FabricClientCommandSource source, @Arg("label") String label) {
        return live(source, label, DEFAULT_LIVE_SECONDS);
    }

    @Subcommand({"live"})
    public static int live(FabricClientCommandSource source, @Arg("label") String label, @Arg("seconds") int seconds) {
        int clampedSeconds = Math.max(1, seconds);
        boolean started = PerfSuite.INSTANCE.startLive(label, clampedSeconds * TICKS_PER_SECOND, outcome -> report(source, outcome));
        if (!started) {
            source.sendError(Component.literal("A live perf session is already running"));
            return 0;
        }
        source.sendFeedback(Component.literal("Recording live perf for " + clampedSeconds + "s, play normally...").withStyle(ChatFormatting.GRAY));
        return 1;
    }

    @Subcommand({"compare"})
    public static int compare(FabricClientCommandSource source, @Arg("before") String beforeLabel, @Arg("after") String afterLabel) {
        Optional<List<PerfComparison.Row>> comparison = PerfSuite.INSTANCE.compare(beforeLabel, afterLabel);
        if (comparison.isEmpty()) {
            source.sendError(Component.literal("Missing report for '" + beforeLabel + "' or '" + afterLabel + "' (see /perf list)"));
            return 0;
        }
        List<PerfComparison.Row> rows = comparison.get();
        source.sendFeedback(Component.literal("Perf " + beforeLabel + " -> " + afterLabel + " (per call, negative is faster)").withStyle(ChatFormatting.AQUA));
        for (PerfComparison.Row row : rows) {
            source.sendFeedback(Component.literal(String.format(Locale.ROOT, "%s: %s -> %s ms (%+.1f%%) total %s -> %s ms", row.id(), formatMs(row.beforePerCallMs()), formatMs(row.afterPerCallMs()), row.deltaPercent(), formatMs(row.beforeTotalMs()), formatMs(row.afterTotalMs()))).withStyle(deltaColor(row.deltaPercent())));
        }
        long faster = rows.stream().filter(row -> row.deltaPercent() < -NOISE_THRESHOLD_PERCENT).count();
        long slower = rows.stream().filter(row -> row.deltaPercent() > NOISE_THRESHOLD_PERCENT).count();
        source.sendFeedback(Component.literal(rows.size() + " compared: " + faster + " faster, " + slower + " slower (beyond " + NOISE_THRESHOLD_PERCENT + "%)").withStyle(ChatFormatting.AQUA));
        return 1;
    }

    @Subcommand({"list"})
    public static int list(FabricClientCommandSource source) {
        List<String> labels = PerfReportStore.INSTANCE.listLabels();
        source.sendFeedback(Component.literal(labels.isEmpty() ? "No saved perf reports" : "Saved perf reports: " + String.join(", ", labels)).withStyle(ChatFormatting.GRAY));
        return 1;
    }

    @Subcommand({"tests"})
    public static int tests(FabricClientCommandSource source) {
        PerfTestRegistry.INSTANCE.initialize();
        for (PerfTest test : PerfTestRegistry.INSTANCE.getTests()) {
            source.sendFeedback(Component.literal(test.getId() + " - " + test.getDescription()).withStyle(ChatFormatting.GRAY));
        }
        return 1;
    }

    private static void report(FabricClientCommandSource source, PerfSuite.RunOutcome outcome) {
        PerfReport report = outcome.report();
        outcome.failures().forEach(failure -> source.sendError(Component.literal("Failed " + failure)));
        source.sendFeedback(Component.literal("Perf report '" + report.label() + "' (per call / total)").withStyle(ChatFormatting.AQUA));
        for (PerfResult result : report.results()) {
            String line = String.format(Locale.ROOT, "%s: %s ms/call, %s ms total (%d calls, p95 %s, max %s)", result.id(), formatMs(result.perCallMs()), formatMs(result.totalMs()), result.calls(), formatMs(result.p95Ms()), formatMs(result.maxMs()));
            source.sendFeedback(Component.literal(line).withStyle(ChatFormatting.WHITE));
            Client.LOG.info("[perf:{}] {}", report.label(), line);
        }
        if (outcome.file() != null) {
            source.sendFeedback(Component.literal("Saved " + outcome.file()).withStyle(ChatFormatting.GRAY));
        }
    }

    private static String formatMs(double milliseconds) {
        return String.format(Locale.ROOT, "%.4f", milliseconds);
    }

    private static ChatFormatting deltaColor(double deltaPercent) {
        if (deltaPercent < -NOISE_THRESHOLD_PERCENT) {
            return ChatFormatting.GREEN;
        }
        if (deltaPercent > NOISE_THRESHOLD_PERCENT) {
            return ChatFormatting.RED;
        }
        return ChatFormatting.GRAY;
    }
}
