package cc.fascinated.fascinatedutils.debug.perf.core;

import cc.fascinated.fascinatedutils.debug.perf.report.PerfResult;
import cc.fascinated.fascinatedutils.event.FascinatedEventBus;
import cc.fascinated.fascinatedutils.event.impl.ClientTickEvent;
import lombok.Getter;
import meteordevelopment.orbit.EventHandler;

import com.sun.management.GarbageCollectionNotificationInfo;

import javax.management.NotificationEmitter;
import javax.management.NotificationListener;
import javax.management.openmbean.CompositeData;
import java.io.IOException;
import java.lang.management.CompilationMXBean;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;
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

    /**
     * The section that holds each frame's CPU time. The headline area summary keys on it.
     */
    public static final String FRAME_CPU_SECTION = "frame.cpu";

    private static final long NOT_RECORDING = Long.MIN_VALUE;
    private static final long NO_ALLOCATION = -1;
    private static final String COUNTER_PREFIX = "count.";
    private static final double NANOS_PER_MILLI = 1_000_000.0;

    private final Map<String, Section> sections = new ConcurrentHashMap<>();
    private final Map<String, Counter> counters = new ConcurrentHashMap<>();
    private final Section frameSection = section("frame");
    private final Section frameCpuSection = section(FRAME_CPU_SECTION);
    private final Section frameWaitSection = section("frame.wait");
    private final NanoSamples timelineFrameEnds = new NanoSamples();
    private final NanoSamples timelineFrameNanos = new NanoSamples();
    private final NanoSamples timelineCpuNanos = new NanoSamples();
    private final NanoSamples gcEventEnds = new NanoSamples();
    private final NanoSamples gcEventNanos = new NanoSamples();
    private final NanoSamples jitSampleTimes = new NanoSamples();
    private final NanoSamples jitSampleTotals = new NanoSamples();
    private final CompilationMXBean compilationBean = ManagementFactory.getCompilationMXBean();
    private boolean gcListenerInstalled;
    private long sessionStartNanos;
    private long lastFrameCpuNanos;
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
     * Returns the handle for a counter, creating it on first use. A counter's per-tick totals are reported as the
     * section {@code count.<name>}, where {@code medianMs} and the other statistics hold the count per tick, not
     * milliseconds.
     *
     * @param name the counter name
     * @return the shared counter for that name
     */
    public Counter counter(String name) {
        return counters.computeIfAbsent(name, key -> new Counter(section(COUNTER_PREFIX + key)));
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
        for (Counter counter : counters.values()) {
            counter.pending.reset();
        }
        timelineFrameEnds.clear();
        timelineFrameNanos.clear();
        timelineCpuNanos.clear();
        gcEventEnds.clear();
        gcEventNanos.clear();
        jitSampleTimes.clear();
        jitSampleTotals.clear();
        installGcListener();
        this.remainingTicks = durationTicks;
        this.completion = completion;
        this.lastFrameNanos = 0;
        this.lastFrameCpuNanos = 0;
        this.sessionStartNanos = System.nanoTime();
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
     * Marks the start of an allocation measurement on the calling thread.
     *
     * @return a token to pass to {@code endAllocation}, or -1 when not recording or unsupported
     */
    public long beginAllocation() {
        return recording ? PerfAllocation.INSTANCE.currentThreadBytes() : NO_ALLOCATION;
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
            long interval = now - lastFrameNanos;
            frameSection.record(interval);
            // Time between frames not spent in the previous frame's CPU work: GPU sync, swap and driver waits.
            if (lastFrameCpuNanos > 0 && lastFrameCpuNanos <= interval) {
                frameWaitSection.record(interval - lastFrameCpuNanos);
            }
            timelineFrameEnds.add(now);
            timelineFrameNanos.add(interval);
            timelineCpuNanos.add(lastFrameCpuNanos);
        }
        lastFrameNanos = now;
    }

    /**
     * Records the CPU time of the frame that just finished under {@code frame.cpu}, and keeps it for {@code frame.wait}.
     *
     * @param token the value returned by {@link #begin()} at the start of the frame
     */
    public void endFrameCpu(long token) {
        if (token == NOT_RECORDING || !recording) {
            return;
        }
        lastFrameCpuNanos = System.nanoTime() - token;
        frameCpuSection.record(lastFrameCpuNanos);
    }

    /**
     * Writes the last session's per-frame timeline as CSV: frame time, CPU time, wait time, and the GC pause and JIT
     * compile time that landed in each frame. Call after the session finished, before the next one starts.
     *
     * @param file the CSV file to write
     * @throws IOException when the file cannot be written
     */
    public void writeTimeline(Path file) throws IOException {
        long[] frameEnds = timelineFrameEnds.toArray();
        long[] frameNanos = timelineFrameNanos.toArray();
        long[] cpuNanos = timelineCpuNanos.toArray();
        long[] gcEnds = gcEventEnds.toArray();
        long[] gcNanos = gcEventNanos.toArray();
        long[] jitTimes = jitSampleTimes.toArray();
        long[] jitTotals = jitSampleTotals.toArray();
        StringBuilder csv = new StringBuilder("frame,timeMs,frameMs,cpuMs,waitMs,gcMs,jitMs\n");
        int gcIndex = 0;
        int jitIndex = 0;
        long previousJitTotal = jitTotals.length > 0 ? jitTotals[0] : 0;
        for (int frame = 0; frame < frameEnds.length; frame++) {
            long frameEnd = frameEnds[frame];
            long gcInFrame = 0;
            while (gcIndex < gcEnds.length && gcEnds[gcIndex] <= frameEnd) {
                gcInFrame += gcNanos[gcIndex];
                gcIndex++;
            }
            long jitInFrame = 0;
            while (jitIndex < jitTimes.length && jitTimes[jitIndex] <= frameEnd) {
                jitInFrame += jitTotals[jitIndex] - previousJitTotal;
                previousJitTotal = jitTotals[jitIndex];
                jitIndex++;
            }
            long waitNanos = cpuNanos[frame] > 0 && cpuNanos[frame] <= frameNanos[frame] ? frameNanos[frame] - cpuNanos[frame] : 0;
            csv.append(String.format(Locale.ROOT, "%d,%.3f,%.4f,%.4f,%.4f,%.3f,%d%n", frame, (frameEnd - sessionStartNanos) / NANOS_PER_MILLI, frameNanos[frame] / NANOS_PER_MILLI, cpuNanos[frame] / NANOS_PER_MILLI, waitNanos / NANOS_PER_MILLI, gcInFrame / NANOS_PER_MILLI, jitInFrame));
        }
        Files.createDirectories(file.getParent());
        Files.writeString(file, csv);
    }

    private synchronized void installGcListener() {
        if (gcListenerInstalled) {
            return;
        }
        gcListenerInstalled = true;
        NotificationListener listener = (notification, handback) -> {
            if (!recording || !GarbageCollectionNotificationInfo.GARBAGE_COLLECTION_NOTIFICATION.equals(notification.getType())) {
                return;
            }
            GarbageCollectionNotificationInfo info = GarbageCollectionNotificationInfo.from((CompositeData) notification.getUserData());
            gcEventEnds.add(System.nanoTime());
            gcEventNanos.add((long) (info.getGcInfo().getDuration() * NANOS_PER_MILLI));
        };
        for (GarbageCollectorMXBean collector : ManagementFactory.getGarbageCollectorMXBeans()) {
            if (collector instanceof NotificationEmitter emitter) {
                emitter.addNotificationListener(listener, null, null);
            }
        }
    }

    @EventHandler
    private void alumite$onClientTick(ClientTickEvent event) {
        if (!recording) {
            return;
        }
        for (Counter counter : counters.values()) {
            counter.section.durations.add((long) (counter.pending.sumThenReset() * NANOS_PER_MILLI));
        }
        if (compilationBean != null && compilationBean.isCompilationTimeMonitoringSupported()) {
            jitSampleTimes.add(System.nanoTime());
            jitSampleTotals.add(compilationBean.getTotalCompilationTime());
        }
        if (--remainingTicks > 0) {
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
     * A thread-safe event counter. Each client tick of a session records how many events happened in that tick.
     */
    public class Counter {
        private final Section section;
        private final LongAdder pending = new LongAdder();

        private Counter(Section section) {
            this.section = section;
        }

        /**
         * Counts events. Safe from any thread; does nothing when no session is recording.
         *
         * @param amount how many events to add
         */
        public void add(long amount) {
            if (recording) {
                pending.add(amount);
            }
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

        public long begin() {
            return recording ? System.nanoTime() : NOT_RECORDING;
        }

        public void end(long token) {
            if (token == NOT_RECORDING || !recording) {
                return;
            }
            durations.add(System.nanoTime() - token);
        }

        public void record(long durationNanos) {
            if (!recording) {
                return;
            }
            durations.add(durationNanos);
        }

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
