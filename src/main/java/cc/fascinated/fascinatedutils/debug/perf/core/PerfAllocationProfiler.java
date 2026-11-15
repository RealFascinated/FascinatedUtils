package cc.fascinated.fascinatedutils.debug.perf.core;

import cc.fascinated.fascinatedutils.debug.perf.PerfIds;
import cc.fascinated.fascinatedutils.debug.perf.PerfOut;
import cc.fascinated.fascinatedutils.debug.perf.PerfProperties;
import jdk.jfr.Recording;
import jdk.jfr.consumer.RecordedClass;
import jdk.jfr.consumer.RecordedEvent;
import jdk.jfr.consumer.RecordedFrame;
import jdk.jfr.consumer.RecordedMethod;
import jdk.jfr.consumer.RecordedStackTrace;
import jdk.jfr.consumer.RecordedThread;
import jdk.jfr.consumer.RecordingFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Records the heap allocations made during one scene's measure window with JFR's {@code jdk.ObjectAllocationSample}
 * event, then prints the biggest allocation sites. Only used when {@code -Dalumite.perf.allocProfile=true}, so a
 * normal run never starts a recording.
 *
 * <p>Each sample carries an allocation weight in bytes. Sites are summed three ways: the first mod frame in the stack,
 * the top frame (the method that allocated, whatever its package) and the thread that allocated.
 */
public class PerfAllocationProfiler {
    public static final PerfAllocationProfiler INSTANCE = new PerfAllocationProfiler();

    private static final String ALLOCATION_EVENT = "jdk.ObjectAllocationSample";
    private static final String SAMPLE_THROTTLE = "150/s";
    private static final String WEIGHT_FIELD = "weight";
    private static final String THREAD_FIELD = "eventThread";
    private static final String[] MOD_PACKAGE_PREFIXES = {"cc.fascinated.", "meteordevelopment."};
    private static final String UNKNOWN_SITE = "unknown";
    private static final int TOP_SITE_COUNT = 15;
    private static final int TOP_THREAD_COUNT = 5;
    private static final double BYTES_PER_MB = 1024.0 * 1024.0;

    private Recording recording;

    /**
     * Reports whether allocation profiling was requested with {@code -Dalumite.perf.allocProfile=true}.
     *
     * @return true when profiling is requested
     */
    public boolean isRequested() {
        return Boolean.getBoolean(PerfProperties.ALLOC_PROFILE);
    }

    /**
     * Starts a recording for the measure window that is about to begin. A failure is printed and the run continues
     * without a profile.
     */
    public void start() {
        try {
            Recording started = new Recording();
            started.enable(ALLOCATION_EVENT).withStackTrace().with("throttle", SAMPLE_THROTTLE);
            started.start();
            recording = started;
        } catch (RuntimeException exception) {
            PerfOut.INSTANCE.printAndLog("FAILED allocProfile: " + exception);
        }
    }

    /**
     * Stops the running recording, saves it to {@code file} and prints its top allocation sites. Does nothing when no
     * recording is running.
     *
     * @param file    the {@code .jfr} file to write
     * @param sceneId the scene the recording covers
     */
    public void stop(Path file, String sceneId) {
        Recording finished = recording;
        if (finished == null) {
            return;
        }
        recording = null;
        String sceneName = PerfIds.SCENE_RESULT_PREFIX + sceneId;
        try {
            finished.stop();
            Files.createDirectories(file.getParent());
            finished.dump(file);
            printSites(file, sceneName);
        } catch (IOException | RuntimeException exception) {
            PerfOut.INSTANCE.printAndLog("FAILED allocProfile: " + exception);
        } finally {
            finished.close();
        }
    }

    private void printSites(Path file, String sceneName) throws IOException {
        Map<String, Long> modBytes = new HashMap<>();
        Map<String, Long> topBytes = new HashMap<>();
        Map<String, Long> threadBytes = new HashMap<>();
        long totalBytes = 0;
        for (RecordedEvent event : RecordingFile.readAllEvents(file)) {
            // The recording also holds JFR's own metadata events, which have no weight.
            if (!ALLOCATION_EVENT.equals(event.getEventType().getName()) || !event.hasField(WEIGHT_FIELD)) {
                continue;
            }
            long bytes = event.getLong(WEIGHT_FIELD);
            totalBytes += bytes;
            List<RecordedFrame> frames = frames(event);
            topBytes.merge(frames.isEmpty() ? UNKNOWN_SITE : describe(frames.getFirst()), bytes, Long::sum);
            for (RecordedFrame frame : frames) {
                if (isModFrame(frame)) {
                    modBytes.merge(describe(frame), bytes, Long::sum);
                    break;
                }
            }
            threadBytes.merge(threadName(event), bytes, Long::sum);
        }
        printRanking(sceneName, "mod", modBytes, totalBytes, TOP_SITE_COUNT);
        printRanking(sceneName, "top", topBytes, totalBytes, TOP_SITE_COUNT);
        printRanking(sceneName, "thread", threadBytes, totalBytes, TOP_THREAD_COUNT);
    }

    private void printRanking(String sceneName, String kind, Map<String, Long> bytesBySite, long totalBytes, int limit) {
        List<Map.Entry<String, Long>> ranked = bytesBySite.entrySet().stream().sorted(Map.Entry.<String, Long>comparingByValue().reversed()).limit(limit).toList();
        for (int index = 0; index < ranked.size(); index++) {
            Map.Entry<String, Long> entry = ranked.get(index);
            double percent = totalBytes == 0 ? 0 : entry.getValue() * 100.0 / totalBytes;
            PerfOut.INSTANCE.printAndLog(String.format(Locale.ROOT, "alloc %s %s %d %.2fMB %.1f%% %s", sceneName, kind, index + 1, entry.getValue() / BYTES_PER_MB, percent, entry.getKey()));
        }
    }

    private static List<RecordedFrame> frames(RecordedEvent event) {
        RecordedStackTrace stackTrace = event.getStackTrace();
        return stackTrace == null ? List.of() : stackTrace.getFrames();
    }

    private static boolean isModFrame(RecordedFrame frame) {
        String className = frame.getMethod().getType().getName();
        for (String prefix : MOD_PACKAGE_PREFIXES) {
            if (className.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    private static String threadName(RecordedEvent event) {
        RecordedThread thread = event.getThread(THREAD_FIELD);
        return thread == null || thread.getJavaName() == null ? UNKNOWN_SITE : thread.getJavaName();
    }

    private static String describe(RecordedFrame frame) {
        RecordedMethod method = frame.getMethod();
        RecordedClass type = method.getType();
        String className = type.getName();
        String simpleName = className.substring(className.lastIndexOf('.') + 1);
        return simpleName + "." + method.getName() + ":" + frame.getLineNumber();
    }
}
