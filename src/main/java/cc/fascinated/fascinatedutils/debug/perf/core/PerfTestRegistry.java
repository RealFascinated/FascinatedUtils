package cc.fascinated.fascinatedutils.debug.perf.core;

import cc.fascinated.fascinatedutils.debug.perf.tests.calibration.CalibrationPerfTest;
import cc.fascinated.fascinatedutils.debug.perf.tests.config.ConfigWriteProfilePerfTest;
import cc.fascinated.fascinatedutils.debug.perf.tests.entity.EntityCullPassPerfTest;
import cc.fascinated.fascinatedutils.debug.perf.tests.entity.EntitySnapshotPublishPerfTest;
import cc.fascinated.fascinatedutils.debug.perf.tests.event.ClientTickDispatchPerfTest;
import cc.fascinated.fascinatedutils.debug.perf.tests.event.EventDispatchPerfTest;
import cc.fascinated.fascinatedutils.debug.perf.tests.hud.HudRenderPerfTest;
import cc.fascinated.fascinatedutils.debug.perf.tests.module.ModuleTogglePerfTest;
import cc.fascinated.fascinatedutils.debug.perf.tests.particle.ParticleCullPassPerfTest;
import cc.fascinated.fascinatedutils.debug.perf.tests.particle.ParticleFilterPerfTest;
import cc.fascinated.fascinatedutils.debug.perf.tests.text.ItemTooltipPerfTest;
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
        register(new ParticleFilterPerfTest());
        register(new MiniMessageParsePerfTest());
        register(new ConfigWriteProfilePerfTest());
        register(new ModuleTogglePerfTest());
        register(new ItemTooltipPerfTest());
        register(new EventDispatchPerfTest());
        register(new ClientTickDispatchPerfTest());
        register(new HudRenderPerfTest());
        register(new EntityCullPassPerfTest());
        register(new EntitySnapshotPublishPerfTest());
        register(new ParticleCullPassPerfTest());
    }

    /**
     * Adds a test to the suite.
     *
     * @param test the test to add
     */
    public void register(PerfTest test) {
        tests.add(test);
    }

    /**
     * Lists every registered test.
     *
     * @return an unmodifiable list of tests
     */
    public List<PerfTest> getTests() {
        return Collections.unmodifiableList(tests);
    }

    /**
     * Selects tests whose id contains the filter text, ignoring case.
     *
     * @param filter the text to look for, or null/blank for every test
     * @return the matching tests
     */
    public List<PerfTest> select(String filter) {
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
