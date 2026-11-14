package cc.fascinated.fascinatedutils.debug.perf;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;

/**
 * Executes {@link PerfTest}s: warm-up, then timed iterations measured with {@link System#nanoTime()}.
 */
public class PerfRunner {

    /**
     * Runs every test in order on the calling thread.
     *
     * @param tests       the tests to run
     * @param failureSink receives one message per test that threw
     * @return one result per test that completed
     */
    public List<PerfResult> run(Collection<PerfTest> tests, Consumer<String> failureSink) {
        List<PerfResult> results = new ArrayList<>();
        for (PerfTest test : tests) {
            try {
                results.add(runSingle(test));
            } catch (Throwable throwable) {
                failureSink.accept(test.getId() + ": " + throwable);
            }
        }
        return results;
    }

    private PerfResult runSingle(PerfTest test) {
        test.setup();
        try {
            for (int warmupIndex = 0; warmupIndex < test.getWarmupIterations(); warmupIndex++) {
                test.runOnce();
            }
            long[] samples = new long[test.getMeasuredIterations()];
            for (int iterationIndex = 0; iterationIndex < samples.length; iterationIndex++) {
                long start = System.nanoTime();
                test.runOnce();
                samples[iterationIndex] = System.nanoTime() - start;
            }
            return PerfResult.fromNanos(test.getId(), samples);
        } finally {
            test.teardown();
        }
    }
}
