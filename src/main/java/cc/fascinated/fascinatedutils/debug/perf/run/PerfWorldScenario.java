package cc.fascinated.fascinatedutils.debug.perf.run;

import cc.fascinated.fascinatedutils.debug.perf.PerfIds;
import cc.fascinated.fascinatedutils.debug.perf.PerfMode;
import cc.fascinated.fascinatedutils.debug.perf.PerfOut;
import cc.fascinated.fascinatedutils.debug.perf.PerfProperties;
import cc.fascinated.fascinatedutils.debug.perf.core.PerfAllocationProfiler;
import cc.fascinated.fascinatedutils.debug.perf.core.PerfGcSnapshot;
import cc.fascinated.fascinatedutils.debug.perf.core.PerfJvmSnapshot;
import cc.fascinated.fascinatedutils.debug.perf.core.PerfProbe;
import cc.fascinated.fascinatedutils.debug.perf.core.PerfRunner;
import cc.fascinated.fascinatedutils.debug.perf.core.PerfSparkProfiler;
import cc.fascinated.fascinatedutils.debug.perf.core.PerfStartup;
import cc.fascinated.fascinatedutils.debug.perf.core.PerfTest;
import cc.fascinated.fascinatedutils.debug.perf.core.PerfTestRegistry;
import cc.fascinated.fascinatedutils.debug.perf.coverage.PerfCoverage;
import cc.fascinated.fascinatedutils.debug.perf.report.PerfReport;
import cc.fascinated.fascinatedutils.debug.perf.report.PerfReportStore;
import cc.fascinated.fascinatedutils.debug.perf.report.PerfResult;
import cc.fascinated.fascinatedutils.debug.perf.scene.PerfScenario;
import cc.fascinated.fascinatedutils.debug.perf.scene.PerfScene;
import cc.fascinated.fascinatedutils.debug.perf.scene.PerfSceneContext;
import cc.fascinated.fascinatedutils.debug.perf.scene.PerfSceneRegistry;
import cc.fascinated.fascinatedutils.event.FascinatedEventBus;
import cc.fascinated.fascinatedutils.event.impl.ClientTickEvent;
import cc.fascinated.fascinatedutils.systems.modules.Module;
import cc.fascinated.fascinatedutils.systems.modules.ModuleRegistry;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.player.Abilities;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Generic in-world runner. Starts at the title screen, opens (or creates) the scenario's world, then for each
 * {@link PerfScene}: flies the player to a fixed camera, runs the scene's setup, lets it settle, records live frame,
 * tick and system timings with {@link PerfProbe} for the measure window, runs the world {@link PerfTest}s that
 * declared that scene, and tears the scene down. Results are named {@code scene.<scene id>/<section or test id>}.
 *
 * <p>JVM properties:
 * <ul>
 *   <li>{@code alumite.perf.scenario=<name>} picks a preset from {@link PerfSceneRegistry} ({@code default},
 *       {@code quick}, {@code gui}, {@code full}); any other name runs the default settings under that name</li>
 *   <li>{@code alumite.perf.world=<label>} saves the report as {@code <label>} (defaults to the scenario name)</li>
 *   <li>{@code alumite.perf.worldName}, {@code seed}, {@code worldType=normal|flat}, {@code settleSeconds},
 *       {@code measureSeconds}, {@code modules=all|profile}, {@code scenes=<a,b>} override the preset</li>
 *   <li>{@code alumite.perf.filter=<text>} only runs world tests whose id contains the text</li>
 *   <li>{@code alumite.perf.exit=true} closes the game when finished</li>
 *   <li>{@code alumite.perf.allocProfile=true} records JFR allocation samples for each scene's measure window</li>
 *   <li>{@code alumite.perf.spark=cpu|alloc} runs Spark's client profiler over each scene's measure window and saves the
 *   profile under {@code spark/<label>/}; it replaces the JFR allocProfile for that run</li>
 *   <li>{@code alumite.perf.deep=true} enables deep checks: the soak leak check, which repeats the scene list for
 *   {@code alumite.perf.soakMinutes} minutes (default 10) printing retained heap per cycle</li>
 * </ul>
 * While a world run is active the mod does not write its config, profile or waypoints, so the user's files are safe.
 */
public class PerfWorldScenario {
    public static final PerfWorldScenario INSTANCE = new PerfWorldScenario();

    private static final String SCENARIO_META_PREFIX = "world:";
    private static final String ONE_OFF_RESULT_PREFIX = "oneoff.";

    private static final int TITLE_SETTLE_TICKS = 60;
    private static final int WORLD_SETTLE_TICKS = 100;
    private static final int TITLE_TIMEOUT_TICKS = 6_000;
    private static final int WORLD_TIMEOUT_TICKS = 12_000;
    private static final int SHOT_TIMEOUT_TICKS = 200;
    private static final int RELOAD_TIMEOUT_TICKS = 60 * 20;
    private static final double NANOS_PER_MILLI = 1_000_000.0;
    private static final Set<String> NETWORKED_MODULE_KEYS = Set.of("mcutils", "hypixel");
    private static final double SOAK_GROWTH_LIMIT_MB = 5.0;
    private static final int DEEP_SOAK_MINUTES = 10;

    private static final List<String> CAMERA_COMMANDS = List.of("gamemode creative", "time set noon", "weather clear", "kill @e[type=!player]", "execute positioned 0 0 0 positioned over world_surface run tp @s ~ ~18 ~ 0 20");

    private enum Phase {
        IDLE, WAIT_TITLE, WAIT_WORLD, SCENE_CAMERA, SCENE_SETUP, SCENE_SETTLE, SCENE_SHOT, SCENE_MEASURE, SCENE_TESTS, SCENE_TEARDOWN, RELOAD, DONE
    }

    private final PerfClientOptions clientOptions = new PerfClientOptions();
    private final PerfRunner runner = new PerfRunner();
    private final List<PerfResult> collected = new ArrayList<>();
    private final Map<String, String> extraMeta = new LinkedHashMap<>();
    private PerfScenario scenario;
    private List<PerfScene> scenes = List.of();
    private Phase phase = Phase.IDLE;
    private String label;
    private boolean optionsApplied;
    private boolean runningTests;
    private volatile boolean shotSaved;
    private int sceneIndex;
    private int phaseTicks;
    private int sceneTick;
    private long runStartNanos;
    private long worldOpenStartNanos;
    private boolean worldJoined;
    private long reloadStartNanos;
    private CompletableFuture<Void> reloadFuture;
    private PerfSceneContext context;
    private PerfGcSnapshot sceneGcStart;
    private PerfJvmSnapshot sceneJvmStart;
    private boolean allocProfile;
    private String sparkMode;
    private int soakMinutes;
    private int completedCycles;
    private long soakStartNanos;
    private final List<Double> soakRetainedMb = new ArrayList<>();

    /**
     * Subscribes to client ticks when a world run was requested.
     */
    public void register() {
        if (!PerfMode.isWorldScenario()) {
            return;
        }
        PerfSceneRegistry.INSTANCE.initialize();
        String scenarioName = System.getProperty(PerfProperties.SCENARIO, PerfSceneRegistry.DEFAULT_SCENARIO);
        Optional<PerfScenario> preset = PerfSceneRegistry.INSTANCE.getScenario(scenarioName);
        if (preset.isEmpty()) {
            PerfOut.INSTANCE.printAndLog("scenario " + scenarioName + " is not a preset (" + String.join(", ", PerfSceneRegistry.INSTANCE.getScenarioNames()) + "): running the default settings plus property overrides under that name");
        }
        scenario = preset.orElseGet(() -> PerfSceneRegistry.INSTANCE.getScenario(PerfSceneRegistry.DEFAULT_SCENARIO).orElseThrow().withName(scenarioName)).withPropertyOverrides();
        label = System.getProperty(PerfProperties.WORLD, scenario.name());
        sparkMode = PerfSparkProfiler.INSTANCE.requestedMode();
        allocProfile = PerfAllocationProfiler.INSTANCE.isRequested() && sparkMode == null;
        if (sparkMode != null && PerfAllocationProfiler.INSTANCE.isRequested()) {
            PerfOut.INSTANCE.printAndLog("note " + PerfProperties.ALLOC_PROFILE + " is skipped while " + PerfProperties.SPARK + " is set");
        }
        soakMinutes = Boolean.getBoolean(PerfProperties.DEEP) ? Integer.getInteger(PerfProperties.SOAK_MINUTES, DEEP_SOAK_MINUTES) : 0;
        if (soakMinutes == 0 && Integer.getInteger(PerfProperties.SOAK_MINUTES, 0) > 0) {
            PerfOut.INSTANCE.printAndLog("note " + PerfProperties.SOAK_MINUTES + " is ignored without " + PerfProperties.DEEP + "=true");
        }
        phase = Phase.WAIT_TITLE;
        FascinatedEventBus.INSTANCE.subscribe(this);
    }

    @EventHandler
    private void alumite$onClientTick(ClientTickEvent event) {
        if (phase == Phase.IDLE || phase == Phase.DONE || runningTests) {
            return;
        }
        phaseTicks++;
        Minecraft minecraft = event.minecraftClient();
        try {
            if (!optionsApplied) {
                clientOptions.apply(minecraft);
                PerfScreenshots.INSTANCE.applyWindowSize(minecraft);
                optionsApplied = true;
            }
            if (minecraft.level != null && minecraft.screen instanceof PauseScreen && (context == null || !context.ownsScreen())) {
                // A pause screen stops the game ticking; only a scene that opened it on purpose keeps it.
                minecraft.setScreen(null);
            }
            keepFlying(minecraft);
            if (context != null) {
                context.reassertHeldKeys();
                context.flushCommands();
            }
            PerfSparkProfiler.INSTANCE.poll();
            switch (phase) {
                case WAIT_TITLE -> waitForTitle(minecraft);
                case WAIT_WORLD -> waitForWorld(minecraft);
                case SCENE_CAMERA -> afterCommands(this::setupScene);
                case SCENE_SETUP -> afterCommands(() -> enter(Phase.SCENE_SETTLE));
                case SCENE_SETTLE -> settle(minecraft);
                case SCENE_SHOT -> shoot(minecraft);
                case SCENE_MEASURE -> tickScene();
                case SCENE_TESTS -> runSceneTests();
                case SCENE_TEARDOWN -> afterCommands(this::nextScene);
                case RELOAD -> waitForReload(minecraft);
                default -> {
                }
            }
        } catch (Throwable throwable) {
            fail(minecraft, throwable.toString());
        }
    }

    private void waitForTitle(Minecraft minecraft) {
        if (phaseTicks > TITLE_TIMEOUT_TICKS) {
            fail(minecraft, "timed out waiting for the title screen");
            return;
        }
        if (minecraft.getOverlay() != null || !(minecraft.screen instanceof TitleScreen)) {
            return;
        }
        PerfStartup.INSTANCE.markClientReady();
        if (phaseTicks < TITLE_SETTLE_TICKS) {
            return;
        }
        scenes = selectScenes(minecraft);
        if (scenes == null) {
            return;
        }
        String worldName = scenario.worldName();
        PerfOut.INSTANCE.printAndLog(String.format(Locale.ROOT, "scenario %s world=%s type=%s seed=%d settleTicks=%d measureTicks=%d modules=%s scenes=%s", scenario.name(), worldName, scenario.worldType(), scenario.seed(), scenario.settleTicks(), scenario.measureTicks(), scenario.moduleMode(), String.join(",", scenes.stream().map(PerfScene::getId).toList())));
        runStartNanos = System.nanoTime();
        worldOpenStartNanos = runStartNanos;
        if (minecraft.getLevelSource().levelExists(worldName)) {
            minecraft.createWorldOpenFlows().openWorld(worldName, () -> fail(minecraft, "world open was cancelled"));
        } else {
            LevelSettings settings = new LevelSettings(worldName, GameType.CREATIVE, new LevelSettings.DifficultySettings(Difficulty.PEACEFUL, false, false), true, WorldDataConfiguration.DEFAULT);
            WorldOptions options = new WorldOptions(scenario.seed(), false, false);
            if (scenario.worldType() == PerfScenario.WorldType.FLAT) {
                minecraft.createWorldOpenFlows().createFreshLevel(worldName, settings, options, WorldPresets::createFlatWorldDimensions, new TitleScreen());
            } else {
                minecraft.createWorldOpenFlows().createFreshLevel(worldName, settings, options, WorldPresets::createNormalWorldDimensions, new TitleScreen());
            }
        }
        enter(Phase.WAIT_WORLD);
    }

    private List<PerfScene> selectScenes(Minecraft minecraft) {
        List<PerfScene> selected = new ArrayList<>();
        for (String sceneId : scenario.sceneIds()) {
            Optional<PerfScene> scene = PerfSceneRegistry.INSTANCE.getScene(sceneId);
            if (scene.isEmpty()) {
                fail(minecraft, "unknown scene " + sceneId + " (known: " + String.join(",", PerfSceneRegistry.INSTANCE.getScenes().stream().map(PerfScene::getId).toList()) + ")");
                return null;
            }
            selected.add(scene.get());
        }
        return selected;
    }

    private void waitForWorld(Minecraft minecraft) {
        if (phaseTicks > WORLD_TIMEOUT_TICKS) {
            fail(minecraft, "timed out waiting for the world to load");
            return;
        }
        if (!worldJoined && minecraft.level != null && minecraft.player != null) {
            worldJoined = true;
            recordOneOff("world_join", System.nanoTime() - worldOpenStartNanos);
        }
        boolean loaded = minecraft.level != null && minecraft.player != null && minecraft.screen == null && minecraft.player.tickCount > WORLD_SETTLE_TICKS && minecraft.levelRenderer.hasRenderedAllSections();
        if (!loaded) {
            return;
        }
        prepareClient(minecraft);
        startScene(minecraft);
    }

    private void prepareClient(Minecraft minecraft) {
        if (scenario.moduleMode() == PerfScenario.ModuleMode.ALL) {
            for (Module module : ModuleRegistry.INSTANCE.getModules()) {
                if (!NETWORKED_MODULE_KEYS.contains(module.getModuleKey())) {
                    module.setEnabled(true);
                }
            }
        }
        PerfScreenshots.INSTANCE.applyGuiScale(minecraft);
        keepFlying(minecraft);
    }

    /**
     * Holds the player in flight. A teleport or a game mode change can clear flying, which would drop the camera out of
     * its fixed position, so this runs every tick once the player exists.
     */
    private void keepFlying(Minecraft minecraft) {
        if (minecraft.player == null || phase == Phase.WAIT_TITLE) {
            return;
        }
        Abilities abilities = minecraft.player.getAbilities();
        if (!abilities.mayfly || !abilities.flying) {
            abilities.mayfly = true;
            abilities.flying = true;
            minecraft.player.onUpdateAbilities();
        }
    }

    private void startScene(Minecraft minecraft) {
        if (sceneIndex >= scenes.size()) {
            if (soakMinutes > 0 && endSoakCycle(minecraft)) {
                return;
            }
            if (PerfScreenshots.INSTANCE.isEnabled()) {
                finish(minecraft);
            } else {
                startResourceReload(minecraft);
            }
            return;
        }
        PerfScene scene = scenes.get(sceneIndex);
        PerfOut.INSTANCE.printAndLog("scene " + scene.getId() + " - " + scene.getDescription());
        context = new PerfSceneContext(minecraft, scenario);
        CAMERA_COMMANDS.forEach(context::command);
        enter(Phase.SCENE_CAMERA);
    }

    private void afterCommands(Runnable next) {
        if (!context.hasPendingCommands()) {
            next.run();
        }
    }

    private void setupScene() {
        scenes.get(sceneIndex).setup(context);
        sceneTick = 0;
        enter(Phase.SCENE_SETUP);
    }

    private void settle(Minecraft minecraft) {
        PerfScene scene = scenes.get(sceneIndex);
        tickScene();
        if (phaseTicks < scene.settleTicks(scenario)) {
            return;
        }
        if (PerfScreenshots.INSTANCE.isEnabled()) {
            shotSaved = false;
            enter(Phase.SCENE_SHOT);
            return;
        }
        boolean started = PerfProbe.INSTANCE.start(scene.measureTicks(scenario), results -> sceneMeasured(scene, results));
        if (!started) {
            fail(minecraft, "a live perf session was already running");
            return;
        }
        sceneGcStart = PerfGcSnapshot.now();
        sceneJvmStart = PerfJvmSnapshot.now();
        if (allocProfile && !isSoakRepeat()) {
            PerfAllocationProfiler.INSTANCE.start();
        }
        if (sparkMode != null && !isSoakRepeat()) {
            PerfSparkProfiler.INSTANCE.start(minecraft, label, scene.getId(), sparkMode);
        }
        enter(Phase.SCENE_MEASURE);
    }

    private void shoot(Minecraft minecraft) {
        tickScene();
        if (phaseTicks == 1) {
            PerfScreenshots.INSTANCE.capture(minecraft, scenes.get(sceneIndex).getId(), () -> shotSaved = true, reason -> minecraft.execute(() -> fail(minecraft, reason)));
        } else if (phaseTicks > SHOT_TIMEOUT_TICKS) {
            fail(minecraft, "timed out saving the screenshot for " + scenes.get(sceneIndex).getId());
        } else if (shotSaved) {
            scenes.get(sceneIndex).teardown(context);
            enter(Phase.SCENE_TEARDOWN);
        }
    }

    private void tickScene() {
        context.reopenScreenIfClosed();
        scenes.get(sceneIndex).onTick(context, sceneTick++);
    }

    private void sceneMeasured(PerfScene scene, List<PerfResult> results) {
        PerfSparkProfiler.INSTANCE.stop(Minecraft.getInstance());
        Path allocFile = PerfReportStore.INSTANCE.directory().resolve("alloc").resolve(PerfReportStore.sanitizeLabel(label)).resolve(scene.getId() + ".jfr");
        PerfAllocationProfiler.INSTANCE.stop(allocFile, scene.getId());
        if (isSoakRepeat()) {
            // Soak repeats only exercise the scenes; their numbers and output are dropped.
            enter(Phase.SCENE_TESTS);
            return;
        }
        for (PerfResult result : results) {
            collected.add(result.withId(PerfIds.sceneResultPrefix(scene.getId()) + result.id()));
        }
        sceneGcStart.recordSince(PerfIds.SCENE_RESULT_PREFIX + scene.getId(), extraMeta);
        PerfOut.INSTANCE.printAndLog(sceneJvmStart.recordSince(PerfIds.SCENE_RESULT_PREFIX + scene.getId(), extraMeta));
        Path timelineFile = PerfReportStore.INSTANCE.directory().resolve("timeline").resolve(PerfReportStore.sanitizeLabel(label)).resolve(scene.getId() + ".csv");
        try {
            PerfProbe.INSTANCE.writeTimeline(timelineFile);
            PerfOut.INSTANCE.printAndLog("timeline " + timelineFile);
        } catch (IOException exception) {
            PerfOut.INSTANCE.printAndLog("FAILED timeline: " + exception);
        }
        // Tests run on the next tick, outside the probe's own tick callback.
        enter(Phase.SCENE_TESTS);
    }

    private void runSceneTests() {
        PerfScene scene = scenes.get(sceneIndex);
        PerfTestRegistry.INSTANCE.initialize();
        List<PerfTest> tests = PerfTestRegistry.INSTANCE.selectForScene(scene.getId(), System.getProperty(PerfProperties.FILTER));
        if (!tests.isEmpty()) {
            runningTests = true;
            try {
                List<PerfResult> testResults = runner.run(tests, PerfIds.sceneResultPrefix(scene.getId()), failure -> PerfOut.INSTANCE.printAndLog("FAILED " + failure), extraMeta);
                if (!isSoakRepeat()) {
                    collected.addAll(testResults);
                }
            } finally {
                runningTests = false;
            }
        }
        scene.teardown(context);
        enter(Phase.SCENE_TEARDOWN);
    }

    private void nextScene() {
        context.releaseAll();
        context = null;
        if (!isSoakRepeat()) {
            recordRetainedHeap(scenes.get(sceneIndex).getId());
        }
        sceneIndex++;
        startScene(Minecraft.getInstance());
    }

    private void recordRetainedHeap(String sceneId) {
        // A full GC after teardown leaves only what the scene kept alive; growth from scene to scene points at a leak.
        System.gc();
        String value = String.format(Locale.ROOT, "%.1f", usedHeapMb());
        extraMeta.put("heap." + PerfIds.SCENE_RESULT_PREFIX + sceneId + ".retainedMb", value);
        PerfOut.INSTANCE.printAndLog("heap " + PerfIds.SCENE_RESULT_PREFIX + sceneId + " retainedMb=" + value);
    }

    private static double usedHeapMb() {
        Runtime runtime = Runtime.getRuntime();
        return (runtime.totalMemory() - runtime.freeMemory()) / (1024.0 * 1024.0);
    }

    /**
     * Reports whether the current pass over the scene list is a soak repeat. Only the first pass prints per-scene
     * results and saves the report.
     *
     * @return true after the first soak cycle has completed
     */
    private boolean isSoakRepeat() {
        return completedCycles > 0;
    }

    /**
     * Ends one pass over the scene list during a soak run: collects garbage, records the retained heap and starts
     * another pass while the soak time has not elapsed.
     *
     * @param minecraft the client
     * @return true when another pass was started
     */
    private boolean endSoakCycle(Minecraft minecraft) {
        completedCycles++;
        System.gc();
        double retainedMb = usedHeapMb();
        soakRetainedMb.add(retainedMb);
        PerfOut.INSTANCE.printAndLog(String.format(Locale.ROOT, "soak cycle %d retainedMb=%.1f", completedCycles, retainedMb));
        if (completedCycles == 1) {
            soakStartNanos = System.nanoTime();
        }
        if (System.nanoTime() - soakStartNanos < TimeUnit.MINUTES.toNanos(soakMinutes)) {
            sceneIndex = 0;
            startScene(minecraft);
            return true;
        }
        printSoakSummary();
        return false;
    }

    private void printSoakSummary() {
        double growthMbPerCycle = slopePerCycle(soakRetainedMb, 0);
        double laterGrowthMbPerCycle = slopePerCycle(soakRetainedMb, 1);
        PerfOut.INSTANCE.printAndLog(String.format(Locale.ROOT, "soak cycles=%d firstMb=%.1f lastMb=%.1f growthMbPerCycle=%.2f", completedCycles, soakRetainedMb.getFirst(), soakRetainedMb.getLast(), growthMbPerCycle));
        if (laterGrowthMbPerCycle > SOAK_GROWTH_LIMIT_MB) {
            PerfOut.INSTANCE.printAndLog(String.format(Locale.ROOT, "FAILED soak: retained heap grew %.2f MB per cycle", laterGrowthMbPerCycle));
        }
    }

    /**
     * Least-squares slope of the retained heap against the cycle number, over the cycles from a start index.
     *
     * @param values    the retained heap per cycle, in MB
     * @param fromIndex the first index to include
     * @return the growth in MB per cycle, or 0 when fewer than two cycles are included
     */
    private static double slopePerCycle(List<Double> values, int fromIndex) {
        int count = values.size() - fromIndex;
        if (count < 2) {
            return 0;
        }
        double meanCycle = 0;
        double meanMb = 0;
        for (int index = fromIndex; index < values.size(); index++) {
            meanCycle += index;
            meanMb += values.get(index);
        }
        meanCycle /= count;
        meanMb /= count;
        double covariance = 0;
        double variance = 0;
        for (int index = fromIndex; index < values.size(); index++) {
            double cycleOffset = index - meanCycle;
            covariance += cycleOffset * (values.get(index) - meanMb);
            variance += cycleOffset * cycleOffset;
        }
        return covariance / variance;
    }

    private void startResourceReload(Minecraft minecraft) {
        reloadStartNanos = System.nanoTime();
        reloadFuture = minecraft.reloadResourcePacks();
        enter(Phase.RELOAD);
    }

    private void waitForReload(Minecraft minecraft) {
        if (phaseTicks > RELOAD_TIMEOUT_TICKS) {
            fail(minecraft, "resource reload did not complete within 60 seconds");
            return;
        }
        if (!reloadFuture.isDone()) {
            return;
        }
        if (reloadFuture.isCompletedExceptionally()) {
            fail(minecraft, "resource reload failed");
            return;
        }
        recordOneOff("resource_reload", System.nanoTime() - reloadStartNanos);
        finish(minecraft);
    }

    private void finish(Minecraft minecraft) {
        phase = Phase.DONE;
        if (PerfScreenshots.INSTANCE.isEnabled()) {
            end(minecraft);
            return;
        }
        recordOneOff("mod_init", PerfStartup.INSTANCE.modInitNanos());
        recordOneOff("client_ready", PerfStartup.INSTANCE.clientReadyNanos());
        extraMeta.put("scenes", String.join(",", scenes.stream().map(PerfScene::getId).toList()));
        extraMeta.put("worldType", scenario.worldType().name());
        extraMeta.put("settleTicks", String.valueOf(scenario.settleTicks()));
        extraMeta.put("measureTicks", String.valueOf(scenario.measureTicks()));
        extraMeta.put("moduleMode", scenario.moduleMode().name());
        PerfReport report = PerfReport.create(label, SCENARIO_META_PREFIX + scenario.name(), collected, extraMeta);
        for (PerfResult result : report.results()) {
            PerfOut.INSTANCE.printResult(result);
        }
        PerfAutoRun.INSTANCE.printGcLines(report);
        PerfOut.INSTANCE.printAndLog(String.format(Locale.ROOT, "run seconds=%.1f", (System.nanoTime() - runStartNanos) / 1_000_000_000.0));
        if (scenario.printSummary()) {
            PerfCoverage.Report coverage = PerfCoverage.INSTANCE.check();
            PerfCoverage.INSTANCE.printAreaSummary(coverage, collected, PerfOut.INSTANCE::printAndLog);
        }
        try {
            Path file = PerfReportStore.INSTANCE.save(report);
            PerfOut.INSTANCE.printAndLog("report " + file);
        } catch (IOException exception) {
            PerfOut.INSTANCE.printAndLog("FAILED save: " + exception);
        }
        PerfAutoRun.INSTANCE.printComparisonIfRequested(label);
        end(minecraft);
    }

    private void recordOneOff(String name, long nanos) {
        collected.add(PerfResult.fromNanos(ONE_OFF_RESULT_PREFIX + name, new long[]{nanos}));
        PerfOut.INSTANCE.printAndLog(String.format(Locale.ROOT, "oneoff %s ms=%.3f", name, nanos / NANOS_PER_MILLI));
    }

    private void fail(Minecraft minecraft, String reason) {
        phase = Phase.DONE;
        PerfSparkProfiler.INSTANCE.cancel(minecraft);
        PerfOut.INSTANCE.printAndLog("FAILED " + reason);
        if (context != null) {
            try {
                if (sceneIndex >= 0 && sceneIndex < scenes.size()) {
                    scenes.get(sceneIndex).teardown(context);
                }
            } catch (Throwable throwable) {
                PerfOut.INSTANCE.printAndLog("FAILED teardown after failure: " + throwable);
            }
            try {
                context.releaseAll();
            } catch (Throwable throwable) {
                PerfOut.INSTANCE.printAndLog("FAILED release after failure: " + throwable);
            }
            context = null;
        }
        end(minecraft);
    }

    private void end(Minecraft minecraft) {
        PerfOut.INSTANCE.done();
        if (Boolean.getBoolean(PerfProperties.EXIT)) {
            minecraft.execute(minecraft::stop);
        }
    }

    private void enter(Phase next) {
        phase = next;
        phaseTicks = 0;
    }
}
