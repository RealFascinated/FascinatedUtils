package cc.fascinated.fascinatedutils.debug.perf.tests;

import cc.fascinated.fascinatedutils.debug.perf.PerfTest;

/**
 * Fixed CPU workload unrelated to the mod. If this changes a lot between two runs, the machine was noisy and the
 * other differences should not be trusted.
 */
public class CalibrationPerfTest extends PerfTest {
    private long sink;

    public CalibrationPerfTest() {
        super("calibration.cpu_loop", "Fixed arithmetic loop; use it to judge machine noise between runs");
    }

    @Override
    public void runOnce() {
        long accumulator = sink;
        for (int index = 0; index < 200_000; index++) {
            accumulator += (accumulator ^ index) * 31L;
        }
        sink = accumulator;
    }
}
