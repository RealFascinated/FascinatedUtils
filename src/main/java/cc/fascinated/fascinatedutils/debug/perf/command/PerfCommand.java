package cc.fascinated.fascinatedutils.debug.perf.command;

import cc.fascinated.fascinatedutils.client.Client;
import cc.fascinated.fascinatedutils.client.command.annotation.Arg;
import cc.fascinated.fascinatedutils.client.command.annotation.ClientCommand;
import cc.fascinated.fascinatedutils.client.command.annotation.Subcommand;
import cc.fascinated.fascinatedutils.debug.perf.PerfSuite;
import cc.fascinated.fascinatedutils.debug.perf.core.PerfTest;
import cc.fascinated.fascinatedutils.debug.perf.core.PerfTestRegistry;
import cc.fascinated.fascinatedutils.debug.perf.coverage.PerfCoverage;
import cc.fascinated.fascinatedutils.debug.perf.report.PerfComparison;
import cc.fascinated.fascinatedutils.debug.perf.report.PerfReport;
import cc.fascinated.fascinatedutils.debug.perf.report.PerfReportStore;
import cc.fascinated.fascinatedutils.debug.perf.report.PerfResult;
import cc.fascinated.fascinatedutils.debug.perf.scene.PerfScene;
import cc.fascinated.fascinatedutils.debug.perf.scene.PerfSceneRegistry;
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
 * /perf scenes                   list world scenes and scenario presets
 * /perf coverage                 list mod code that no test or scene covers
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
        int matching = PerfTestRegistry.INSTANCE.selectMicro(filter).size();
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
        Optional<PerfComparison.Result> comparison = PerfSuite.INSTANCE.compare(beforeLabel, afterLabel);
        if (comparison.isEmpty()) {
            source.sendError(Component.literal("Missing report for '" + beforeLabel + "' or '" + afterLabel + "' (see /perf list)"));
            return 0;
        }
        PerfComparison.Result result = comparison.get();
        source.sendFeedback(Component.literal("Perf " + beforeLabel + " -> " + afterLabel + " (median per call, negative is faster)").withStyle(ChatFormatting.AQUA));
        result.warnings().forEach(warning -> source.sendFeedback(Component.literal("Warning: " + warning).withStyle(ChatFormatting.YELLOW)));
        for (PerfComparison.Row row : result.rows()) {
            source.sendFeedback(Component.literal(String.format(Locale.ROOT, "%s: %s -> %s ms (%+.1f%%, noise %.1f%%, p=%.4f) total %s -> %s ms%s", row.id(), formatMs(row.beforeMedianMs()), formatMs(row.afterMedianMs()), row.deltaPercent(), row.noisePercent(), row.pValue(), formatMs(row.beforeTotalMs()), formatMs(row.afterTotalMs()), formatAlloc(row))).withStyle(deltaColor(row)));
        }
        if (!result.onlyInBefore().isEmpty() || !result.onlyInAfter().isEmpty()) {
            source.sendFeedback(Component.literal("Not in both reports: only before " + result.onlyInBefore() + ", only after " + result.onlyInAfter()).withStyle(ChatFormatting.YELLOW));
        }
        long faster = result.rows().stream().filter(row -> row.significant() && row.deltaPercent() < 0).count();
        long slower = result.rows().stream().filter(row -> row.significant() && row.deltaPercent() > 0).count();
        source.sendFeedback(Component.literal(result.rows().size() + " compared: " + faster + " faster, " + slower + " slower (beyond each test's noise)").withStyle(ChatFormatting.AQUA));
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

    @Subcommand({"scenes"})
    public static int scenes(FabricClientCommandSource source) {
        for (PerfScene scene : PerfSceneRegistry.INSTANCE.getScenes()) {
            source.sendFeedback(Component.literal(scene.getId() + " - " + scene.getDescription()).withStyle(ChatFormatting.GRAY));
        }
        source.sendFeedback(Component.literal("Scenarios (-Dalumite.perf.scenario=<name>): " + String.join(", ", PerfSceneRegistry.INSTANCE.getScenarioNames())).withStyle(ChatFormatting.AQUA));
        return 1;
    }

    @Subcommand({"coverage"})
    public static int coverage(FabricClientCommandSource source) {
        PerfCoverage.Report report = PerfCoverage.INSTANCE.check();
        PerfCoverage.INSTANCE.printToStdout(report);
        source.sendFeedback(Component.literal(String.format(Locale.ROOT, "Coverage: %d entries, %d covered, %d excluded, %d uncovered, %d unknown (full list in the log)", report.inventory().size(), report.coveredBy().size(), report.excluded().size(), report.uncovered().size(), report.unknown().size())).withStyle(report.uncovered().isEmpty() && report.unknown().isEmpty() ? ChatFormatting.GREEN : ChatFormatting.YELLOW));
        report.uncovered().forEach(entryId -> source.sendFeedback(Component.literal("Uncovered " + entryId).withStyle(ChatFormatting.RED)));
        report.unknown().forEach(entryId -> source.sendFeedback(Component.literal("Unknown " + entryId).withStyle(ChatFormatting.YELLOW)));
        return 1;
    }

    private static void report(FabricClientCommandSource source, PerfSuite.RunOutcome outcome) {
        PerfReport report = outcome.report();
        outcome.failures().forEach(failure -> source.sendError(Component.literal("Failed " + failure)));
        source.sendFeedback(Component.literal("Perf report '" + report.label() + "' (per call / total)").withStyle(ChatFormatting.AQUA));
        for (PerfResult result : report.results()) {
            String line = String.format(Locale.ROOT, "%s: median %s ms/call (mean %s, p95 %s, p99 %s, max %s, %d stutters, cv %.1f%%), %s ms total, %d calls%s", result.id(), formatMs(result.medianMs()), formatMs(result.perCallMs()), formatMs(result.p95Ms()), formatMs(result.p99Ms()), formatMs(result.maxMs()), result.stutters(), result.cvPercent(), formatMs(result.totalMs()), result.calls(), result.allocBytesPerCall() < 0 ? "" : String.format(Locale.ROOT, ", %.0f B/call", result.allocBytesPerCall()));
            source.sendFeedback(Component.literal(line).withStyle(ChatFormatting.WHITE));
            Client.LOG.info("[perf:{}] {}", report.label(), line);
        }
        if (outcome.file() != null) {
            source.sendFeedback(Component.literal("Saved " + outcome.file()).withStyle(ChatFormatting.GRAY));
        }
    }

    private static String formatMs(double milliseconds) {
        return String.format(Locale.ROOT, "%.6f", milliseconds);
    }

    private static String formatAlloc(PerfComparison.Row row) {
        if (row.beforeAllocBytes() < 0 || row.afterAllocBytes() < 0) {
            return "";
        }
        return String.format(Locale.ROOT, ", alloc %.0f -> %.0f B/call", row.beforeAllocBytes(), row.afterAllocBytes());
    }

    private static ChatFormatting deltaColor(PerfComparison.Row row) {
        if (!row.significant()) {
            return ChatFormatting.GRAY;
        }
        return row.deltaPercent() < 0 ? ChatFormatting.GREEN : ChatFormatting.RED;
    }
}
