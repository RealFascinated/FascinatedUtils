package cc.fascinated.fascinatedutils.debug.perf;

import cc.fascinated.fascinatedutils.debug.perf.tests.CalibrationPerfTest;
import cc.fascinated.fascinatedutils.debug.perf.tests.ClientTickDispatchPerfTest;
import cc.fascinated.fascinatedutils.debug.perf.tests.ConfigSavePerfTest;
import cc.fascinated.fascinatedutils.debug.perf.tests.HudPanelVisibilityPerfTest;
import cc.fascinated.fascinatedutils.debug.perf.tests.ModuleSerializePerfTest;
import cc.fascinated.fascinatedutils.systems.modules.Module;
import cc.fascinated.fascinatedutils.systems.modules.ModuleRegistry;

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
        register(new ClientTickDispatchPerfTest());
        register(new HudPanelVisibilityPerfTest());
        register(new ConfigSavePerfTest());
        for (Module module : ModuleRegistry.INSTANCE.getModules()) {
            register(new ModuleSerializePerfTest(module));
        }
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
}
