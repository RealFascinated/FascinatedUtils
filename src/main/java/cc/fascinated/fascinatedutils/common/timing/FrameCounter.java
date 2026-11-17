package cc.fascinated.fascinatedutils.common.timing;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;

public class FrameCounter {
    private static final FrameCounter INSTANCE = new FrameCounter();
    private final Deque<FrameSample> samples = new ArrayDeque<>();
    private final long smoothWindow = 500_000_000L; // 0.5 seconds in nanoseconds
    private final long windowNanos = 30_000_000_000L;   // 30 seconds in nanoseconds, so 0.1% low averages more than one frame
    private final long maxFrameDeltaNanos = 1_000_000_000L; // longer gaps (minimized, loading, alt-tab) are not real frames
    private final long updateIntervalNanos = 500_000_000L; // 0.5 seconds in nanoseconds
    private long lastFrameTime = -1;
    private long lastUpdateTime = 0;
    private double cachedSmoothFps = 0;
    private double cachedOnePercentLowFps = 0;
    private double cachedPointOnePercentLowFps = 0;
    private long[] sortedDeltaScratch = new long[0];

    public static FrameCounter getInstance() {
        return FrameCounter.INSTANCE;
    }

    public synchronized void onFrame() {
        long now = System.nanoTime();

        // Record new frame delta
        if (this.lastFrameTime != -1) {
            long delta = now - this.lastFrameTime;
            if (delta <= this.maxFrameDeltaNanos) {
                this.samples.addLast(new FrameSample(now, delta));
            }
        }
        this.lastFrameTime = now;

        // Trim old samples
        while (!this.samples.isEmpty() && now - this.samples.peekFirst().timestamp > this.windowNanos) {
            this.samples.removeFirst();
        }

        // Throttle stat computation
        if (now - this.lastUpdateTime >= this.updateIntervalNanos) {
            this.lastUpdateTime = now;
            if (!this.samples.isEmpty()) {
                int sampleCount = this.samples.size();
                if (this.sortedDeltaScratch.length < sampleCount) {
                    this.sortedDeltaScratch = new long[sampleCount * 2];
                }
                int fillIndex = 0;
                for (FrameSample sample : this.samples) {
                    this.sortedDeltaScratch[fillIndex++] = sample.deltaNanos;
                }
                Arrays.sort(this.sortedDeltaScratch, 0, sampleCount);
                this.cachedOnePercentLowFps = this.computePercentileLow(1.0, sampleCount);
                this.cachedPointOnePercentLowFps = this.computePercentileLow(0.1, sampleCount);
                this.cachedSmoothFps = this.computeSmoothFpsFromRecentFrames(now);
            }
            else {
                this.cachedOnePercentLowFps = this.cachedPointOnePercentLowFps = this.cachedSmoothFps = 0;
            }
        }
    }

    private double computeSmoothFpsFromRecentFrames(long now) {
        long recentTotalNanos = 0;
        int recentCount = 0;
        for (FrameSample sample : this.samples) {
            if (now - sample.timestamp <= this.smoothWindow) {
                recentTotalNanos += sample.deltaNanos;
                recentCount++;
            }
        }

        if (recentCount == 0) {
            return 0;
        }

        double avgDelta = (double) recentTotalNanos / recentCount;
        return 1_000_000_000.0 / avgDelta;
    }

    public synchronized int getSmoothFps() {
        return (int) Math.round(this.cachedSmoothFps);
    }

    public synchronized int getOnePercentLowFps() {
        return (int) Math.round(this.cachedOnePercentLowFps);
    }

    public synchronized int getPointOnePercentLowFps() {
        return (int) Math.round(this.cachedPointOnePercentLowFps);
    }

    private double computePercentileLow(double percent, int sampleCount) {
        int count = Math.max(1, (int) Math.ceil(sampleCount * (percent / 100.0)));
        long sum = 0;
        for (int index = 0; index < count; index++) {
            sum += this.sortedDeltaScratch[sampleCount - 1 - index]; // slowest frames are at the end of the ascending sort
        }
        double avgDelta = sum / (double) count;
        return 1_000_000_000.0 / avgDelta;
    }

    private record FrameSample(long timestamp, long deltaNanos) {}
}