package cc.fascinated.fascinatedutils.debug.perf.core;

import cc.fascinated.fascinatedutils.debug.perf.tests.hud.HudPanelPerfTest;
import cc.fascinated.fascinatedutils.debug.perf.tests.ui.ScreenFramePerfTest;
import cc.fascinated.fascinatedutils.debug.perf.tests.ui.ScreenOpenPerfTest;
import cc.fascinated.fascinatedutils.debug.perf.tests.ui.TextWidthCacheChurnPerfTest;
import cc.fascinated.fascinatedutils.gui.screens.impl.screenshot.ScreenshotScreen;
import cc.fascinated.fascinatedutils.gui.screens.impl.settings.ModSettingsScreen;
import cc.fascinated.fascinatedutils.gui.screens.impl.waypoint.CreateEditWaypointScreen;
import cc.fascinated.fascinatedutils.gui.screens.impl.waypoint.WaypointsScreen;
import cc.fascinated.fascinatedutils.systems.hud.HUDManager;
import cc.fascinated.fascinatedutils.systems.hud.HudPanel;
import cc.fascinated.fascinatedutils.systems.modules.ModuleRegistry;
import net.minecraft.client.gui.screens.Screen;
import cc.fascinated.fascinatedutils.debug.perf.tests.calibration.CalibrationPerfTest;
import cc.fascinated.fascinatedutils.debug.perf.tests.chat.ChatDuplicateStackPerfTest;
import cc.fascinated.fascinatedutils.debug.perf.tests.chat.ChatSearchPerfTest;
import cc.fascinated.fascinatedutils.debug.perf.tests.config.ConfigLoadProfilePerfTest;
import cc.fascinated.fascinatedutils.debug.perf.tests.config.ConfigWriteChangedProfilePerfTest;
import cc.fascinated.fascinatedutils.debug.perf.tests.config.ConfigWriteProfilePerfTest;
import cc.fascinated.fascinatedutils.debug.perf.tests.entity.BlockModelCachePerfTest;
import cc.fascinated.fascinatedutils.debug.perf.tests.entity.CullCorrectnessPerfTest;
import cc.fascinated.fascinatedutils.debug.perf.tests.entity.EntityCullPassPerfTest;
import cc.fascinated.fascinatedutils.debug.perf.tests.hooks.SoundFilterPlayPerfTest;
import cc.fascinated.fascinatedutils.debug.perf.tests.hooks.BlockOutlineRenderPerfTest;
import cc.fascinated.fascinatedutils.debug.perf.tests.hooks.StatusEffectsRenderPerfTest;
import cc.fascinated.fascinatedutils.debug.perf.tests.hooks.TitleRenderPerfTest;
import cc.fascinated.fascinatedutils.debug.perf.tests.hooks.ScoreboardSidebarPerfTest;
import cc.fascinated.fascinatedutils.debug.perf.tests.waypoint.WaypointLookupPerfTest;
import cc.fascinated.fascinatedutils.debug.perf.tests.entity.EntitySnapshotPublishPerfTest;
import cc.fascinated.fascinatedutils.debug.perf.tests.event.ClientTickDispatchPerfTest;
import cc.fascinated.fascinatedutils.debug.perf.tests.event.EventDispatchPerfTest;
import cc.fascinated.fascinatedutils.debug.perf.tests.hud.HudPanelMergerPerfTest;
import cc.fascinated.fascinatedutils.debug.perf.tests.hud.HudRenderPerfTest;
import cc.fascinated.fascinatedutils.debug.perf.tests.module.ModuleLookupPerfTest;
import cc.fascinated.fascinatedutils.debug.perf.tests.module.ModuleTogglePerfTest;
import cc.fascinated.fascinatedutils.debug.perf.tests.particle.ParticleCullPassPerfTest;
import cc.fascinated.fascinatedutils.debug.perf.tests.particle.ParticleFilterPerfTest;
import cc.fascinated.fascinatedutils.debug.perf.tests.text.ItemTooltipPerfTest;
import cc.fascinated.fascinatedutils.debug.perf.tests.text.MiniMessageCacheChurnPerfTest;
import cc.fascinated.fascinatedutils.debug.perf.tests.text.MiniMessageParsePerfTest;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Holds every registered {@link PerfTest}. To add a test, write a {@link PerfTest} subclass and add one
 * {@code register(...)} line in {@link #initialize()}.
 */
public class PerfTestRegistry {
    public static final PerfTestRegistry INSTANCE = new PerfTestRegistry();

    private final List<PerfTest> tests = new ArrayList<>();
    private boolean initialized;

    /**
     * Registers every built-in test. Runs once, after modules have been loaded.
     */
    public synchronized void initialize() {
        if (initialized) {
            return;
        }
        initialized = true;

        register(new CalibrationPerfTest());
        for (HudPanel panel : HUDManager.INSTANCE.getWidgets()) {
            register(new HudPanelPerfTest(panel.getId()));
        }
        register(new ScreenFramePerfTest(List.of("screen.settings", "screen.settings_detail", "screen.settings_profiles", "screen.settings_global", "screen.hud_editor", "screen.screenshots", "screen.waypoints", "screen.waypoint_edit")));
        register(new ScreenOpenPerfTest("settings", () -> new ModSettingsScreen(null, null)));
        register(new ScreenOpenPerfTest("settings_detail", () -> {
            ModSettingsScreen.rememberTab(ModSettingsScreen.TAB_MODULES);
            return new ModSettingsScreen(ModuleRegistry.INSTANCE.getModules().stream().filter(module -> module.getDisplayName().equals("Armor HUD")).findFirst().orElse(null), null);
        }));
        register(new ScreenOpenPerfTest("settings_profiles", () -> {
            ModSettingsScreen.rememberTab(ModSettingsScreen.TAB_PROFILES);
            return new ModSettingsScreen(null, null);
        }));
        register(new ScreenOpenPerfTest("settings_global", () -> {
            ModSettingsScreen.rememberTab(ModSettingsScreen.TAB_SETTINGS);
            return new ModSettingsScreen(null, null);
        }));
        register(new ScreenOpenPerfTest("screenshots", ScreenshotScreen::new));
        register(new ScreenOpenPerfTest("waypoints", WaypointsScreen::new));
        register(new ScreenOpenPerfTest("waypoint_edit", () -> new CreateEditWaypointScreen(CreateEditWaypointScreen.Type.CREATE, null)));
        register(new ParticleFilterPerfTest());
        register(new SoundFilterPlayPerfTest());
        register(new BlockOutlineRenderPerfTest());
        register(new StatusEffectsRenderPerfTest());
        register(new TitleRenderPerfTest());
        register(new ScoreboardSidebarPerfTest());
        register(new WaypointLookupPerfTest());
        register(new MiniMessageParsePerfTest());
        register(new MiniMessageCacheChurnPerfTest());
        register(new TextWidthCacheChurnPerfTest());
        register(new ConfigWriteProfilePerfTest());
        register(new ConfigWriteChangedProfilePerfTest());
        register(new ModuleTogglePerfTest());
        register(new ModuleLookupPerfTest());
        register(new ItemTooltipPerfTest());
        register(new EventDispatchPerfTest());
        register(new ClientTickDispatchPerfTest());
        register(new HudRenderPerfTest());
        register(new EntityCullPassPerfTest());
        register(new CullCorrectnessPerfTest());
        register(new EntitySnapshotPublishPerfTest());
        register(new ParticleCullPassPerfTest());
        register(new ConfigLoadProfilePerfTest());
        register(new ChatSearchPerfTest());
        register(new ChatDuplicateStackPerfTest());
        register(new HudPanelMergerPerfTest());
        register(new BlockModelCachePerfTest());
    }

    public void register(PerfTest test) {
        tests.add(test);
    }

    public List<PerfTest> getTests() {
        return Collections.unmodifiableList(tests);
    }

    /**
     * Selects tests whose id contains the filter text, ignoring case.
     *
     * @param filter the text to look for, or null/blank for every test
     * @return the matching tests
     */
    private List<PerfTest> select(String filter) {
        if (filter == null || filter.isBlank()) {
            return getTests();
        }
        String needle = filter.toLowerCase(Locale.ROOT);
        return tests.stream().filter(test -> test.getId().toLowerCase(Locale.ROOT).contains(needle)).toList();
    }

    /**
     * Selects the title-screen micro tests (no scene) whose id contains the filter text.
     *
     * @param filter the text to look for, or null/blank for every micro test
     * @return the matching micro tests
     */
    public List<PerfTest> selectMicro(String filter) {
        return select(filter).stream().filter(test -> test.sceneIds().isEmpty()).toList();
    }

    /**
     * Selects the world tests that declared a scene and whose id contains the filter text.
     *
     * @param sceneId the scene that just finished measuring
     * @param filter  the text to look for, or null/blank for every test of that scene
     * @return the matching world tests
     */
    public List<PerfTest> selectForScene(String sceneId, String filter) {
        return select(filter).stream().filter(test -> test.sceneIds().contains(sceneId)).toList();
    }
}
