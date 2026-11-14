package cc.fascinated.fascinatedutils.debug.perf.core;

import cc.fascinated.fascinatedutils.debug.perf.report.PerfResult;
import cc.fascinated.fascinatedutils.event.FascinatedEventBus;
import cc.fascinated.fascinatedutils.event.impl.ClientTickEvent;
import lombok.Getter;
import meteordevelopment.orbit.EventHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Times named sections of the running game (frames, ticks, HUD widgets) during a live session. When no session is
 * active every call returns immediately, so the hooks cost almost nothing in normal play.
 *
 * <p>Usage at a hook site: resolve a {@link Section} once (a static field or a map built at init), then per sample:
 * <pre>
 * long start = SECTION.begin();
 * ... work ...
 * SECTION.end(start);
 * </pre>
 * A section handle owns its sample buffers, so a sample costs no string building and no map lookup.
 */
public class PerfProbe {
    public static final PerfProbe INSTANCE = new PerfProbe();

    private static final long NOT_RECORDING = Long.MIN_VALUE;
    private static final long NO_ALLOCATION = -1;

    private final Map<String, Section> sections = new ConcurrentHashMap<>();
    private final Section frameSection = section("frame");
    private volatile boolean recording;
    private int remainingTicks;
    private long lastFrameNanos;
    private Consumer<List<PerfResult>> completion;

    /**
     * Subscribes the frame and tick hooks. Called from mod init rather than the constructor, because mixin static
     * fields resolve sections while {@code Minecraft} is still being class-initialised, before the event bus is ready.
     */
    public void register() {
        FascinatedEventBus.INSTANCE.subscribe(this);
    }

    /**
     * Returns the handle for a section name, creating it on first use. Resolve handles once, outside hot paths.
     *
     * @param name the section name
     * @return the shared handle for that name
     */
    public Section section(String name) {
        return sections.computeIfAbsent(name, Section::new);
    }

    /**
     * Starts a live session that ends automatically after the given number of client ticks.
     *
     * @param durationTicks how many client ticks to record for
     * @param completion    called on the client thread with one result per section when the session ends
     * @return false when a session is already running
     */
    public synchronized boolean start(int durationTicks, Consumer<List<PerfResult>> completion) {
        if (recording) {
            return false;
        }
        for (Section section : sections.values()) {
            section.reset();
        }
        this.remainingTicks = durationTicks;
        this.completion = completion;
        this.lastFrameNanos = 0;
        this.recording = true;
        return true;
    }

    /**
     * Reports whether a live session is running.
     *
     * @return true while recording
     */
    public boolean isRecording() {
        return recording;
    }

    /**
     * Marks the start of a section.
     *
     * @return a token to pass to {@code end}
     */
    public long begin() {
        return recording ? System.nanoTime() : NOT_RECORDING;
    }

    /**
     * Records the time since {@link #begin()} under a section name. Hot paths should use a {@link Section} instead.
     *
     * @param section the section name
     * @param token   the value returned by {@link #begin()}
     */
    public void end(String section, long token) {
        if (token == NOT_RECORDING || !recording) {
            return;
        }
        record(section, System.nanoTime() - token);
    }

    /**
     * Marks the start of an allocation measurement on the calling thread.
     *
     * @return a token to pass to {@code endAllocation}, or -1 when not recording or unsupported
     */
    public long beginAllocation() {
        return recording ? PerfAllocation.INSTANCE.currentThreadBytes() : NO_ALLOCATION;
    }

    /**
     * Records the bytes the calling thread allocated since {@link #beginAllocation()} against a section name.
     *
     * @param section the section name
     * @param token   the value returned by {@link #beginAllocation()}
     */
    public void endAllocation(String section, long token) {
        if (token < 0 || !recording) {
            return;
        }
        section(section).endAllocation(token);
    }

    /**
     * Records an already-measured duration under a section name. Hot paths should use a {@link Section} instead.
     *
     * @param section       the section name
     * @param durationNanos the duration in nanoseconds
     */
    public void record(String section, long durationNanos) {
        if (!recording) {
            return;
        }
        section(section).record(durationNanos);
    }

    /**
     * Call once per rendered frame; records the time since the previous frame under {@code frame}.
     */
    public void markFrame() {
        if (!recording) {
            return;
        }
        long now = System.nanoTime();
        if (lastFrameNanos != 0) {
            frameSection.record(now - lastFrameNanos);
        }
        lastFrameNanos = now;
    }

    @EventHandler
    private void alumite$onClientTick(ClientTickEvent event) {
        if (!recording || --remainingTicks > 0) {
            return;
        }
        finish();
    }

    private synchronized void finish() {
        recording = false;
        List<PerfResult> results = new ArrayList<>();
        sections.keySet().stream().sorted().forEach(name -> {
            Section section = sections.get(name);
            long[] durations = section.durations.toArray();
            if (durations.length > 0 || section.meanAllocation() >= 0) {
                results.add(PerfResult.fromNanos(name, durations, section.meanAllocation()));
            }
        });
        Consumer<List<PerfResult>> finished = completion;
        completion = null;
        if (finished != null) {
            finished.accept(results);
        }
    }

    /**
     * A resolved probe section. Holds its own sample buffers, so recording a sample does no lookup.
     */
    public class Section {
        @Getter
        private final String name;
        private volatile NanoSamples durations = new NanoSamples();
        private volatile NanoSamples allocations = new NanoSamples();

        private Section(String name) {
            this.name = name;
        }

        /**
         * Marks the start of a sample.
         *
         * @return a token to pass to {@link #end(long)}
         */
        public long begin() {
            return recording ? System.nanoTime() : NOT_RECORDING;
        }

        /**
         * Records the time since {@link #begin()}.
         *
         * @param token the value returned by {@link #begin()}
         */
        public void end(long token) {
            if (token == NOT_RECORDING || !recording) {
                return;
            }
            durations.add(System.nanoTime() - token);
        }

        /**
         * Records an already-measured duration.
         *
         * @param durationNanos the duration in nanoseconds
         */
        public void record(long durationNanos) {
            if (!recording) {
                return;
            }
            durations.add(durationNanos);
        }

        /**
         * Records the bytes the calling thread allocated since {@link PerfProbe#beginAllocation()}.
         *
         * @param token the value returned by {@link PerfProbe#beginAllocation()}
         */
        public void endAllocation(long token) {
            if (token < 0 || !recording) {
                return;
            }
            long now = PerfAllocation.INSTANCE.currentThreadBytes();
            if (now >= token) {
                allocations.add(now - token);
            }
        }

        private void reset() {
            durations = new NanoSamples();
            allocations = new NanoSamples();
        }

        private double meanAllocation() {
            long[] values = allocations.toArray();
            if (values.length == 0) {
                return -1;
            }
            long sum = 0;
            for (long value : values) {
                sum += value;
            }
            return sum / (double) values.length;
        }
    }
}
