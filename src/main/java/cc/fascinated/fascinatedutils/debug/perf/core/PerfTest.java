package cc.fascinated.fascinatedutils.debug.perf.core;

import cc.fascinated.fascinatedutils.debug.perf.coverage.PerfCoverage;
import cc.fascinated.fascinatedutils.debug.perf.scene.PerfSceneRegistry;
import lombok.Getter;

import java.util.List;

/**
 * A single benchmark. Subclass it, implement {@link #runOnce()} with the work to measure, and add one line to
 * {@link PerfTestRegistry#initialize()} to register it. By default a test runs on the title screen; override
 * {@link #sceneIds()} to run it inside a world scene instead, where its result is named {@code scene.<id>/<test id>}.
 */
@Getter
public abstract class PerfTest {
    private static final int DEFAULT_WARMUP_ITERATIONS = 20;
    private static final int DEFAULT_MEASURED_ITERATIONS = 200;

    private final String id;
    private final String description;
    private final int warmupIterations;
    private final int measuredIterations;

    /**
     * Minimum warm-up time in milliseconds, so the JIT has compiled the test before timing starts. Subclasses may
     * lower it in their constructor for slow tests.
     */
    protected long warmupMillis = 300;

    protected PerfTest(String id, String description, int warmupIterations, int measuredIterations) {
        this.id = id;
        this.description = description;
        this.warmupIterations = warmupIterations;
        this.measuredIterations = measuredIterations;
    }

    protected PerfTest(String id, String description) {
        this(id, description, DEFAULT_WARMUP_ITERATIONS, DEFAULT_MEASURED_ITERATIONS);
    }

    /**
     * Prepares state before warm-up. Not timed.
     */
    public void setup() {
    }

    /**
     * Runs the work being measured exactly once. Called for every warm-up and measured iteration.
     */
    public abstract void runOnce();

    /**
     * Releases state after the measured iterations. Not timed.
     */
    public void teardown() {
    }

    /**
     * The world scenes this test runs in, after the scene's live measurement. Empty means a title-screen micro test.
     *
     * @return scene ids from {@link PerfSceneRegistry}
     */
    public List<String> sceneIds() {
        return List.of();
    }

    /**
     * The inventory ids this test really drives, as checked by {@link PerfCoverage}.
     *
     * @return coverage ids such as {@code mixin:itemtooltip.ItemStackTooltipMixin}
     */
    public List<String> covers() {
        return List.of();
    }
}
