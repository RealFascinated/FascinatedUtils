package cc.fascinated.fascinatedutils.debug.perf;

import lombok.Getter;

/**
 * A single micro-benchmark. Subclass it, implement {@link #runOnce()} with the work to measure, and add one line to
 * {@link PerfTestRegistry#initialize()} to register it.
 */
@Getter
public abstract class PerfTest {
    private static final int DEFAULT_WARMUP_ITERATIONS = 20;
    private static final int DEFAULT_MEASURED_ITERATIONS = 200;

    private final String id;
    private final String description;
    private final int warmupIterations;
    private final int measuredIterations;

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
}
