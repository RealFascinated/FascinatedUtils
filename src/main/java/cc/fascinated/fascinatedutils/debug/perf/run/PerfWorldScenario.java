package cc.fascinated.fascinatedutils.debug.perf.run;

import cc.fascinated.fascinatedutils.client.Client;
import cc.fascinated.fascinatedutils.debug.perf.PerfMode;
import cc.fascinated.fascinatedutils.debug.perf.PerfSuite;
import cc.fascinated.fascinatedutils.debug.perf.core.PerfGcSnapshot;
import cc.fascinated.fascinatedutils.debug.perf.core.PerfProbe;
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
 * </ul>
 * While a world run is active the mod does not write its config, profile or waypoints, so the user's files are safe.
 */
public class PerfWorldScenario {
    public static final PerfWorldScenario INSTANCE = new PerfWorldScenario();

    private static final String PROPERTY_WORLD = PerfMode.PROPERTY_PREFIX + "world";
    private static final String PROPERTY_SCENARIO = PerfMode.PROPERTY_PREFIX + "scenario";
    private static final String PROPERTY_FILTER = PerfMode.PROPERTY_PREFIX + "filter";
    private static final String PROPERTY_EXIT = PerfMode.PROPERTY_PREFIX + "exit";
    private static final String SCENE_RESULT_PREFIX = "scene.";
    private static final String SCENARIO_META_PREFIX = "world:";

    private static final int TITLE_SETTLE_TICKS = 60;
    private static final int WORLD_SETTLE_TICKS = 100;
    private static final int TITLE_TIMEOUT_TICKS = 6_000;
    private static final int WORLD_TIMEOUT_TICKS = 12_000;
    private static final int SHOT_TIMEOUT_TICKS = 200;
    private static final Set<String> NETWORKED_MODULE_KEYS = Set.of("mcutils", "hypixel");

    private static final List<String> CAMERA_COMMANDS = List.of("gamemode creative", "time set noon", "weather clear", "kill @e[type=!player]", "execute positioned 0 0 0 positioned over world_surface run tp @s ~ ~18 ~ 0 20");

    private enum Phase {
        IDLE, WAIT_TITLE, WAIT_WORLD, SCENE_CAMERA, SCENE_SETUP, SCENE_SETTLE, SCENE_SHOT, SCENE_MEASURE, SCENE_TESTS, SCENE_TEARDOWN, DONE
    }

    private final PerfClientOptions clientOptions = new PerfClientOptions();
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
    private PerfSceneContext context;
    private PerfGcSnapshot sceneGcStart;

    /**
     * Subscribes to client ticks when a world run was requested.
     */
    public void register() {
        if (!PerfMode.isWorldScenario()) {
            return;
        }
        PerfSceneRegistry.INSTANCE.initialize();
        String scenarioName = System.getProperty(PROPERTY_SCENARIO, PerfSceneRegistry.DEFAULT_SCENARIO);
        Optional<PerfScenario> preset = PerfSceneRegistry.INSTANCE.getScenario(scenarioName);
        if (preset.isEmpty()) {
            print("scenario " + scenarioName + " is not a preset (" + String.join(", ", PerfSceneRegistry.INSTANCE.getScenarioNames()) + "): running the default settings plus property overrides under that name");
        }
        scenario = preset.orElseGet(() -> PerfSceneRegistry.INSTANCE.getScenario(PerfSceneRegistry.DEFAULT_SCENARIO).orElseThrow().withName(scenarioName)).withPropertyOverrides();
        label = System.getProperty(PROPERTY_WORLD, scenario.name());
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
        if (minecraft.getOverlay() != null || !(minecraft.screen instanceof TitleScreen) || phaseTicks < TITLE_SETTLE_TICKS) {
            return;
        }
        scenes = selectScenes(minecraft);
        if (scenes == null) {
            return;
        }
        String worldName = scenario.worldName();
        print(String.format(Locale.ROOT, "scenario %s world=%s type=%s seed=%d settleTicks=%d measureTicks=%d modules=%s scenes=%s", scenario.name(), worldName, scenario.worldType(), scenario.seed(), scenario.settleTicks(), scenario.measureTicks(), scenario.moduleMode(), String.join(",", scenes.stream().map(PerfScene::getId).toList())));
        runStartNanos = System.nanoTime();
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
            finish(minecraft);
            return;
        }
        PerfScene scene = scenes.get(sceneIndex);
        print("scene " + scene.getId() + " - " + scene.getDescription());
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
        for (PerfResult result : results) {
            collected.add(result.withId(SCENE_RESULT_PREFIX + scene.getId() + "/" + result.id()));
        }
        sceneGcStart.recordSince(SCENE_RESULT_PREFIX + scene.getId(), extraMeta);
        // Tests run on the next tick, outside the probe's own tick callback.
        enter(Phase.SCENE_TESTS);
    }

    private void runSceneTests() {
        PerfScene scene = scenes.get(sceneIndex);
        PerfTestRegistry.INSTANCE.initialize();
        List<PerfTest> tests = PerfTestRegistry.INSTANCE.selectForScene(scene.getId(), System.getProperty(PROPERTY_FILTER));
        if (!tests.isEmpty()) {
            runningTests = true;
            try {
                collected.addAll(PerfSuite.INSTANCE.runTests(tests, SCENE_RESULT_PREFIX + scene.getId() + "/", failure -> print("FAILED " + failure), extraMeta));
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
        sceneIndex++;
        startScene(Minecraft.getInstance());
    }

    private void finish(Minecraft minecraft) {
        phase = Phase.DONE;
        if (PerfScreenshots.INSTANCE.isEnabled()) {
            end(minecraft);
            return;
        }
        extraMeta.put("scenes", String.join(",", scenes.stream().map(PerfScene::getId).toList()));
        extraMeta.put("worldType", scenario.worldType().name());
        extraMeta.put("settleTicks", String.valueOf(scenario.settleTicks()));
        extraMeta.put("measureTicks", String.valueOf(scenario.measureTicks()));
        extraMeta.put("moduleMode", scenario.moduleMode().name());
        PerfReport report = PerfReport.create(label, SCENARIO_META_PREFIX + scenario.name(), collected, extraMeta);
        for (PerfResult result : report.results()) {
            print(String.format(Locale.ROOT, "%s medianMs=%.6f perCallMs=%.6f totalMs=%.6f calls=%d p95Ms=%.6f maxMs=%.6f cvPercent=%.1f allocBytesPerCall=%.0f", result.id(), result.medianMs(), result.perCallMs(), result.totalMs(), result.calls(), result.p95Ms(), result.maxMs(), result.cvPercent(), result.allocBytesPerCall()));
        }
        PerfAutoRun.INSTANCE.printGcLines(report);
        print(String.format(Locale.ROOT, "run seconds=%.1f", (System.nanoTime() - runStartNanos) / 1_000_000_000.0));
        if (scenario.printSummary()) {
            PerfCoverage.Report coverage = PerfCoverage.INSTANCE.check();
            PerfCoverage.INSTANCE.printAreaSummary(coverage, collected, this::print);
        }
        try {
            Path file = PerfReportStore.INSTANCE.save(report);
            print("report " + file);
        } catch (IOException exception) {
            print("FAILED save: " + exception);
        }
        PerfAutoRun.INSTANCE.printComparisonIfRequested(label);
        end(minecraft);
    }

    private void fail(Minecraft minecraft, String reason) {
        phase = Phase.DONE;
        print("FAILED " + reason);
        if (context != null) {
            try {
                if (sceneIndex >= 0 && sceneIndex < scenes.size()) {
                    scenes.get(sceneIndex).teardown(context);
                }
            } catch (Throwable throwable) {
                print("FAILED teardown after failure: " + throwable);
            }
            try {
                context.releaseAll();
            } catch (Throwable throwable) {
                print("FAILED release after failure: " + throwable);
            }
            context = null;
        }
        end(minecraft);
    }

    private void end(Minecraft minecraft) {
        System.out.println("ALUMITE_PERF_DONE");
        if (Boolean.getBoolean(PROPERTY_EXIT)) {
            minecraft.execute(minecraft::stop);
        }
    }

    private void enter(Phase next) {
        phase = next;
        phaseTicks = 0;
    }

    private void print(String line) {
        System.out.println("ALUMITE_PERF " + line);
        Client.LOG.info("[perf] {}", line);
    }
}
