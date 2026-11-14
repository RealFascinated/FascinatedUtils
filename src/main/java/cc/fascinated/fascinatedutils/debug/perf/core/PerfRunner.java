package cc.fascinated.fascinatedutils.debug.perf.core;

import cc.fascinated.fascinatedutils.debug.perf.report.PerfResult;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Executes {@link PerfTest}s. Each test is warmed up for a minimum time so the JIT has compiled it, then timed in
 * batches so very fast calls are not dominated by the cost of reading the clock.
 */
public class PerfRunner {
    private static final long MAX_WARMUP_NANOS = 3_000_000_000L;
    private static final long TARGET_BATCH_NANOS = 1_000_000L;
    private static final int MAX_BATCH_SIZE = 10_000;
    private static final int CALIBRATION_CALLS = 5;
    private static final int MIN_SAMPLES = 10;
    private static final int MAX_SAMPLES = 200;

    /**
     * Runs every test in order on the calling thread.
     *
     * @param tests       the tests to run
     * @param idPrefix    prepended to each result id and GC key, e.g. {@code scene.idle/}, or empty
     * @param failureSink receives one message per test that threw
     * @param gcMeta      receives {@code gc.<id>.count} and {@code gc.<id>.timeMs} per test
     * @return one result per test that completed
     */
    public List<PerfResult> run(Collection<PerfTest> tests, String idPrefix, Consumer<String> failureSink, Map<String, String> gcMeta) {
        List<PerfResult> results = new ArrayList<>();
        for (PerfTest test : tests) {
            try {
                results.add(runSingle(test, idPrefix + test.getId(), gcMeta).withId(idPrefix + test.getId()));
            } catch (Throwable throwable) {
                failureSink.accept(idPrefix + test.getId() + ": " + throwable);
            }
        }
        return results;
    }

    private PerfResult runSingle(PerfTest test, String gcId, Map<String, String> gcMeta) {
        test.setup();
        try {
            warmUp(test);
            int batchSize = calibrateBatchSize(test);
            int sampleCount = Math.max(MIN_SAMPLES, Math.min(MAX_SAMPLES, (int) Math.ceil(test.getMeasuredIterations() / (double) batchSize)));
            double[] samples = new double[sampleCount];
            PerfGcSnapshot gcBefore = PerfGcSnapshot.now();
            long allocatedBefore = PerfAllocation.INSTANCE.currentThreadBytes();
            for (int sampleIndex = 0; sampleIndex < sampleCount; sampleIndex++) {
                long start = System.nanoTime();
                for (int callIndex = 0; callIndex < batchSize; callIndex++) {
                    test.runOnce();
                }
                samples[sampleIndex] = (System.nanoTime() - start) / (double) batchSize;
            }
            long allocatedAfter = PerfAllocation.INSTANCE.currentThreadBytes();
            gcBefore.recordSince(gcId, gcMeta);
            double allocPerCall = allocatedBefore < 0 ? -1 : (allocatedAfter - allocatedBefore) / (double) ((long) sampleCount * batchSize);
            return PerfResult.fromSamples(test.getId(), samples, batchSize, allocPerCall);
        } finally {
            test.teardown();
        }
    }

    private void warmUp(PerfTest test) {
        long deadline = System.nanoTime() + test.getWarmupMillis() * 1_000_000L;
        long hardStop = System.nanoTime() + MAX_WARMUP_NANOS;
        int calls = 0;
        while (calls < test.getWarmupIterations() || System.nanoTime() < deadline) {
            test.runOnce();
            calls++;
            if (System.nanoTime() > hardStop) {
                break;
            }
        }
    }

    private int calibrateBatchSize(PerfTest test) {
        long start = System.nanoTime();
        for (int callIndex = 0; callIndex < CALIBRATION_CALLS; callIndex++) {
            test.runOnce();
        }
        long nanosPerCall = Math.max(1, (System.nanoTime() - start) / CALIBRATION_CALLS);
        return (int) Math.max(1, Math.min(MAX_BATCH_SIZE, TARGET_BATCH_NANOS / nanosPerCall));
    }
}
