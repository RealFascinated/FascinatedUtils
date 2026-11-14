package cc.fascinated.fascinatedutils.debug.perf;

import cc.fascinated.fascinatedutils.event.FascinatedEventBus;
import cc.fascinated.fascinatedutils.event.impl.ClientTickEvent;
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
 * <p>Usage at a hook site:
 * <pre>
 * long start = PerfProbe.INSTANCE.begin();
 * ... work ...
 * PerfProbe.INSTANCE.end("my.section", start);
 * </pre>
 */
public class PerfProbe {
    public static final PerfProbe INSTANCE = new PerfProbe();

    private static final long NOT_RECORDING = Long.MIN_VALUE;
    private static final String FRAME_SECTION = "frame";

    private final Map<String, NanoSamples> sections = new ConcurrentHashMap<>();
    private volatile boolean recording;
    private int remainingTicks;
    private long lastFrameNanos;
    private Consumer<List<PerfResult>> completion;

    private PerfProbe() {
        FascinatedEventBus.INSTANCE.subscribe(this);
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
        sections.clear();
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
     * Records the time since {@link #begin()} under a section name.
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
     * Same as {@link #end(String, long)} but joins the section name only while recording, so hook sites in hot loops
     * do not allocate strings during normal play.
     *
     * @param prefix the first part of the section name
     * @param suffix the second part of the section name
     * @param token  the value returned by {@link #begin()}
     */
    public void end(String prefix, String suffix, long token) {
        if (token == NOT_RECORDING || !recording) {
            return;
        }
        record(prefix + suffix, System.nanoTime() - token);
    }

    /**
     * Records an already-measured duration under a section name.
     *
     * @param section       the section name
     * @param durationNanos the duration in nanoseconds
     */
    public void record(String section, long durationNanos) {
        if (!recording) {
            return;
        }
        sections.computeIfAbsent(section, key -> new NanoSamples()).add(durationNanos);
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
            record(FRAME_SECTION, now - lastFrameNanos);
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
        sections.keySet().stream().sorted().forEach(section -> results.add(PerfResult.fromNanos(section, sections.get(section).toArray())));
        Consumer<List<PerfResult>> finished = completion;
        completion = null;
        if (finished != null) {
            finished.accept(results);
        }
    }
}
