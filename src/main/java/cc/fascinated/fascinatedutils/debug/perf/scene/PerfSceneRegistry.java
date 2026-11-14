package cc.fascinated.fascinatedutils.debug.perf.scene;

import cc.fascinated.fascinatedutils.debug.perf.coverage.PerfCoverage;
import cc.fascinated.fascinatedutils.debug.perf.scene.impl.CameraPerfScene;
import cc.fascinated.fascinatedutils.debug.perf.scene.impl.EntitiesPerfScene;
import cc.fascinated.fascinatedutils.debug.perf.scene.impl.TurboWorldPerfScene;
import cc.fascinated.fascinatedutils.debug.perf.scene.impl.HudOverlaysPerfScene;
import cc.fascinated.fascinatedutils.debug.perf.scene.impl.IdlePerfScene;
import cc.fascinated.fascinatedutils.debug.perf.scene.impl.InteractPerfScene;
import cc.fascinated.fascinatedutils.debug.perf.scene.impl.InventoryPerfScene;
import cc.fascinated.fascinatedutils.debug.perf.scene.impl.KeybindsPerfScene;
import cc.fascinated.fascinatedutils.debug.perf.scene.impl.ParticlesPerfScene;
import cc.fascinated.fascinatedutils.debug.perf.scene.impl.ScreenPerfScene;
import cc.fascinated.fascinatedutils.systems.modules.ModuleRegistry;
import cc.fascinated.fascinatedutils.debug.perf.scene.impl.SoundsPerfScene;
import cc.fascinated.fascinatedutils.debug.perf.scene.impl.WaterPerfScene;
import cc.fascinated.fascinatedutils.gui.screens.impl.hudeditor.HUDEditorScreen;
import cc.fascinated.fascinatedutils.gui.screens.impl.screenshot.ScreenshotScreen;
import cc.fascinated.fascinatedutils.gui.screens.impl.settings.ModSettingsScreen;
import cc.fascinated.fascinatedutils.gui.screens.impl.waypoint.CreateEditWaypointScreen;
import cc.fascinated.fascinatedutils.gui.screens.impl.waypoint.WaypointsScreen;
import cc.fascinated.fascinatedutils.systems.hud.HUDManager;
import cc.fascinated.fascinatedutils.systems.modules.impl.WorldSizeModule;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Holds every {@link PerfScene} and every named {@link PerfScenario} preset. To add a scene, write the class and add
 * one {@code register(...)} line in {@link #initialize()}; to add a preset, add one {@code registerScenario(...)} line.
 */
public class PerfSceneRegistry {
    public static final PerfSceneRegistry INSTANCE = new PerfSceneRegistry();
    public static final String DEFAULT_SCENARIO = "default";

    private static final String DEFAULT_WORLD_NAME = "alumite_perf";
    private static final long DEFAULT_SEED = 7_340_193L;
    private static final int STANDARD_SETTLE_TICKS = 5 * PerfScenario.TICKS_PER_SECOND;
    private static final int STANDARD_MEASURE_TICKS = 10 * PerfScenario.TICKS_PER_SECOND;
    private static final int QUICK_SETTLE_TICKS = 2 * PerfScenario.TICKS_PER_SECOND;
    private static final int QUICK_MEASURE_TICKS = 3 * PerfScenario.TICKS_PER_SECOND;
    private static final int SHOT_SETTLE_TICKS = 3 * PerfScenario.TICKS_PER_SECOND;
    private static final int GUI_MEASURE_TICKS = 5 * PerfScenario.TICKS_PER_SECOND;

    private final Map<String, PerfScene> scenes = new LinkedHashMap<>();
    private final Map<String, PerfScenario> scenarios = new LinkedHashMap<>();
    private boolean initialized;

    /**
     * Registers every built-in scene and scenario. Runs once, after modules have been loaded.
     */
    public synchronized void initialize() {
        if (initialized) {
            return;
        }
        initialized = true;

        register(new IdlePerfScene());
        register(new EntitiesPerfScene());
        register(new TurboWorldPerfScene(TurboWorldPerfScene.RAIN, "Rain over the default terrain, to time weather extraction"));
        register(new TurboWorldPerfScene(TurboWorldPerfScene.DISPLAYS, "75 block, text and item displays at 50 to 60 blocks"));
        register(new TurboWorldPerfScene(TurboWorldPerfScene.NAMETAGS, "100 named sheep at 42 to 60 blocks"));
        register(new ParticlesPerfScene());
        register(new HudOverlaysPerfScene());
        register(new CameraPerfScene());
        register(new InteractPerfScene());
        register(new WaterPerfScene());
        register(new SoundsPerfScene());
        register(new KeybindsPerfScene());
        register(new InventoryPerfScene());
        register(new ScreenPerfScene("pause", "Vanilla pause menu with the mod's buttons and actions overlay", () -> new PauseScreen(true), List.of(PerfCoverage.mixin("GameMenuScreenMixin"), PerfCoverage.mixin("confirmdisconnect.GameMenuScreenMixin"))));
        register(new ScreenPerfScene("settings", "Mod settings screen", () -> new ModSettingsScreen(null, null), List.of(PerfCoverage.screen(ModSettingsScreen.class), PerfCoverage.gui("node"), PerfCoverage.gui("layout"), PerfCoverage.gui("core"), PerfCoverage.common("setting"), PerfCoverage.common("EnumUtils"))));
        register(new ScreenPerfScene("settings_detail", "Module settings page", () -> {
            ModSettingsScreen.rememberTab(ModSettingsScreen.TAB_MODULES);
            return new ModSettingsScreen(ModuleRegistry.INSTANCE.getModules().stream().filter(module -> module.getDisplayName().equals("Armor HUD")).findFirst().orElse(null), null);
        }, List.of(PerfCoverage.screen(ModSettingsScreen.class))));
        register(new ScreenPerfScene("settings_profiles", "Profiles tab", () -> {
            ModSettingsScreen.rememberTab(ModSettingsScreen.TAB_PROFILES);
            return new ModSettingsScreen(null, null);
        }, List.of(PerfCoverage.screen(ModSettingsScreen.class))));
        register(new ScreenPerfScene("settings_global", "Global settings tab", () -> {
            ModSettingsScreen.rememberTab(ModSettingsScreen.TAB_SETTINGS);
            return new ModSettingsScreen(null, null);
        }, List.of(PerfCoverage.screen(ModSettingsScreen.class))));
        register(new ScreenPerfScene("hud_editor", "HUD editor with every panel", () -> {
            HUDManager.INSTANCE.markEditModeActive();
            return new HUDEditorScreen();
        }, HUDManager.INSTANCE::clearEditModeAfterEditorRemoved, List.of(PerfCoverage.screen(HUDEditorScreen.class), PerfCoverage.gui("anim"))));
        register(new ScreenPerfScene("screenshots", "Screenshot gallery", ScreenshotScreen::new, List.of(PerfCoverage.screen(ScreenshotScreen.class), PerfCoverage.system("screenshot"), PerfCoverage.system("TextureManager"))));
        register(new ScreenPerfScene("waypoints", "Waypoint list", WaypointsScreen::new, List.of(PerfCoverage.screen(WaypointsScreen.class), PerfCoverage.system("waypoint"))));
        register(new ScreenPerfScene("waypoint_edit", "Create waypoint form", () -> new CreateEditWaypointScreen(CreateEditWaypointScreen.Type.CREATE, null), List.of(PerfCoverage.screen(CreateEditWaypointScreen.class))));
        register(new ScreenPerfScene("select_world", "Vanilla world list with world sizes", () -> new SelectWorldScreen(new TitleScreen()), List.of(PerfCoverage.mixin("worldsize.SelectWorldScreenMixin"), PerfCoverage.mixin("worldsize.WorldListEntryMixin"), PerfCoverage.mixin("worldsize.WorldSelectionListMixin"), PerfCoverage.module(WorldSizeModule.class))));
        register(new ScreenPerfScene("title", "Vanilla title screen with the mod's additions", TitleScreen::new, List.of(PerfCoverage.mixin("titlemenu.TitleScreenMixin"))));

        List<String> allScenes = new ArrayList<>(scenes.keySet());
        List<String> screenScenes = allScenes.stream().filter(sceneId -> sceneId.startsWith("screen.") || sceneId.equals("inventory")).toList();
        registerScenario(new PerfScenario(DEFAULT_SCENARIO, DEFAULT_WORLD_NAME, DEFAULT_SEED, PerfScenario.WorldType.NORMAL, STANDARD_SETTLE_TICKS, STANDARD_MEASURE_TICKS, PerfScenario.ModuleMode.ALL, List.of("idle", "entities", "particles", "hud_overlays", "camera"), false));
        registerScenario(new PerfScenario("quick", DEFAULT_WORLD_NAME, DEFAULT_SEED, PerfScenario.WorldType.NORMAL, QUICK_SETTLE_TICKS, QUICK_MEASURE_TICKS, PerfScenario.ModuleMode.ALL, List.of("idle", "entities", "particles"), false));
        registerScenario(new PerfScenario("gui", DEFAULT_WORLD_NAME, DEFAULT_SEED, PerfScenario.WorldType.NORMAL, QUICK_SETTLE_TICKS, GUI_MEASURE_TICKS, PerfScenario.ModuleMode.ALL, screenScenes, false));
        List<String> shotScenes = new ArrayList<>(List.of("idle", "hud_overlays"));
        shotScenes.addAll(screenScenes);
        registerScenario(new PerfScenario("shots", DEFAULT_WORLD_NAME, DEFAULT_SEED, PerfScenario.WorldType.NORMAL, SHOT_SETTLE_TICKS, QUICK_MEASURE_TICKS, PerfScenario.ModuleMode.ALL, shotScenes, false));
        registerScenario(new PerfScenario("full", DEFAULT_WORLD_NAME, DEFAULT_SEED, PerfScenario.WorldType.NORMAL, STANDARD_SETTLE_TICKS, STANDARD_MEASURE_TICKS, PerfScenario.ModuleMode.ALL, allScenes, true));
    }

    /**
     * Adds a scene. Its id must be unique.
     *
     * @param scene the scene to add
     */
    public void register(PerfScene scene) {
        if (scenes.putIfAbsent(scene.getId(), scene) != null) {
            throw new IllegalArgumentException("Duplicate perf scene " + scene.getId());
        }
    }

    /**
     * Adds a scenario preset, selectable with {@code -Dalumite.perf.scenario=<name>}.
     *
     * @param scenario the preset to add
     */
    public void registerScenario(PerfScenario scenario) {
        scenarios.put(scenario.name(), scenario);
    }

    /**
     * Lists every registered scene in run order.
     *
     * @return an unmodifiable list of scenes
     */
    public List<PerfScene> getScenes() {
        initialize();
        return Collections.unmodifiableList(new ArrayList<>(scenes.values()));
    }

    /**
     * Looks up a scene by id.
     *
     * @param sceneId the scene id
     * @return the scene, or empty when no scene has that id
     */
    public Optional<PerfScene> getScene(String sceneId) {
        initialize();
        return Optional.ofNullable(scenes.get(sceneId));
    }

    /**
     * Looks up a scenario preset by name.
     *
     * @param name the preset name
     * @return the preset, or empty when none has that name
     */
    public Optional<PerfScenario> getScenario(String name) {
        initialize();
        return Optional.ofNullable(scenarios.get(name));
    }

    /**
     * Lists the preset names.
     *
     * @return the names in registration order
     */
    public List<String> getScenarioNames() {
        initialize();
        return List.copyOf(scenarios.keySet());
    }
}
