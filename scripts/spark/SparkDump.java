import me.lucko.spark.proto.SparkSamplerProtos.SamplerData;
import me.lucko.spark.proto.SparkSamplerProtos.SamplerMetadata;
import me.lucko.spark.proto.SparkSamplerProtos.StackTraceNode;
import me.lucko.spark.proto.SparkSamplerProtos.ThreadNode;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

/**
 * Prints the hot methods of a Spark sampler profile saved with {@code --save-to-file}, per thread.
 *
 * <p>Run it with the Spark Fabric jar on the class path, which provides the proto classes:
 * {@code java -cp run/mods/spark-1.10.172-fabric.jar scripts/spark/SparkDump.java <file.sparkprofile> [topN]}.
 *
 * <p>Each thread lists the top methods by self time, by inclusive time (each method counted once per stack), and by self
 * time inside mod packages. Idle frames such as {@code Unsafe.park} are left out of the rankings and summed as idle time.
 * CPU values are milliseconds; allocation profiles report bytes.
 */
public class SparkDump {
    private static final int DEFAULT_TOP_COUNT = 15;
    private static final int DETAILED_THREAD_LIMIT = 8;
    private static final Pattern NOTABLE_THREAD = Pattern.compile("(?i).*(render|cull|netty|server|worker|\\bio\\b).*");
    private static final List<String> MOD_PREFIXES = List.of("cc.fascinated.", "meteordevelopment.");
    private static final int CALLER_CHAIN_DEPTH = Integer.getInteger("sparkdump.depth", 4);
    private static String callerFilter;
    private static final Set<String> IDLE_FRAMES = Set.of(
            "Unsafe.park", "Unsafe.parkNanos", "LockSupport.park", "LockSupport.parkNanos",
            "Object.wait", "Thread.sleep", "EPoll.wait", "EPoll.epollWait", "EPollSelectorImpl.doSelect", "Net.poll"
    );
    // Async-profiler reports the native frames under a park or wait, so these mark idle subtrees too.
    private static final List<String> IDLE_NATIVE_MARKERS = List.of(
            "Unsafe_Park", "Parker::park", "PosixSemaphore::wait", "futex_abstimed_wait", "pthread_cond_wait",
            "pthread_cond_timedwait", "epoll_wait", "__poll", "nanosleep", "JVM_Sleep", "naked_short_sleep", "sem_wait"
    );

    /**
     * Runs the dump from the command line.
     *
     * @param args the profile file, then an optional number of rows per ranking
     * @throws IOException if the profile cannot be read or parsed
     */
    public static void main(String[] args) throws IOException {
        if (args.length < 1 || args.length > 3) {
            usage();
        }
        int topCount = DEFAULT_TOP_COUNT;
        if (args.length >= 2) {
            try {
                topCount = Integer.parseInt(args[1]);
            } catch (NumberFormatException exception) {
                usage();
            }
            if (topCount < 1) {
                usage();
            }
        }
        callerFilter = args.length == 3 ? args[2] : null;
        Path file = Path.of(args[0]);
        SamplerData data = load(file);
        SamplerMetadata metadata = data.getMetadata();
        boolean allocation = metadata.getSamplerMode() == SamplerMetadata.SamplerMode.ALLOCATION;

        List<ThreadTotals> threads = new ArrayList<>();
        for (ThreadNode threadNode : data.getThreadsList()) {
            threads.add(analyze(threadNode));
        }
        threads.sort(Comparator.comparingDouble((ThreadTotals threadTotals) -> threadTotals.total).reversed());

        System.out.printf(Locale.ROOT, "file %s%n", file);
        System.out.printf(Locale.ROOT, "mode %s unit %s interval %d ticks %d threads %d%n",
                allocation ? "ALLOCATION" : "EXECUTION", allocation ? "bytes" : "ms",
                metadata.getInterval(), metadata.getNumberOfTicks(), threads.size());
        System.out.println();
        System.out.println("threads by total time:");
        for (ThreadTotals threadTotals : threads) {
            System.out.printf(Locale.ROOT, "  thread %-40s total %-16s idle %s%n", threadTotals.name,
                    formatValue(threadTotals.total, allocation), percentOf(threadTotals.idle, threadTotals.total));
        }

        for (int index = 0; index < threads.size(); index++) {
            ThreadTotals threadTotals = threads.get(index);
            boolean notable = NOTABLE_THREAD.matcher(threadTotals.name).matches();
            if (index >= DETAILED_THREAD_LIMIT && !notable) {
                continue;
            }
            System.out.println();
            System.out.printf(Locale.ROOT, "== %s  total %s  idle %s (%s)%n", threadTotals.name,
                    formatValue(threadTotals.total, allocation), formatValue(threadTotals.idle, allocation),
                    percentOf(threadTotals.idle, threadTotals.total));
            printRanking("top " + topCount + " self (idle frames excluded):", "self", threadTotals.self, threadTotals.total, topCount, allocation);
            printRanking("top " + topCount + " inclusive (each method once per stack):", "incl", threadTotals.inclusive, threadTotals.total, topCount, allocation);
            printRanking("top " + topCount + " self in cc.fascinated. / meteordevelopment.:", "mod-self", modOnly(threadTotals.self), threadTotals.total, topCount, allocation);
            if (callerFilter != null) {
                printRanking("top " + topCount + " callers of methods matching '" + callerFilter + "':", "caller", threadTotals.callers, threadTotals.total, topCount, allocation);
            }
        }
    }

    private static void usage() {
        System.err.println("usage: java -cp <spark-fabric.jar> scripts/spark/SparkDump.java <file.sparkprofile> [topN] [callerFilter]");
        System.exit(2);
    }

    /**
     * Reads a profile. Spark writes raw protobuf with {@code --save-to-file}; a gzip-compressed file is also accepted.
     *
     * @param file the {@code .sparkprofile} file
     * @return the parsed sampler data
     * @throws IOException if the file cannot be read or is not a sampler profile
     */
    private static SamplerData load(Path file) throws IOException {
        byte[] bytes = Files.readAllBytes(file);
        boolean gzipped = bytes.length >= 2 && (bytes[0] & 0xff) == 0x1f && (bytes[1] & 0xff) == 0x8b;
        if (gzipped) {
            try (InputStream input = new GZIPInputStream(new ByteArrayInputStream(bytes))) {
                bytes = input.readAllBytes();
            }
        }
        return SamplerData.parseFrom(bytes);
    }

    /**
     * Totals one thread. Spark stores a flat node table on the thread; each node lists its children as indices into that
     * table. A node's times are inclusive, so its self time is its time minus its children's time.
     *
     * @param threadNode the thread from the profile
     * @return the totals for the thread
     */
    private static ThreadTotals analyze(ThreadNode threadNode) {
        ThreadTotals totals = new ThreadTotals(threadNode.getName());
        totals.total = sum(threadNode.getTimesList());
        List<StackTraceNode> table = threadNode.getChildrenList();
        double rootTotal = 0;
        Set<String> path = new HashSet<>();
        for (int index : threadNode.getChildrenRefsList()) {
            StackTraceNode root = resolve(table, index);
            if (root == null) {
                continue;
            }
            rootTotal += sum(root.getTimesList());
            visit(root, table, path, totals, false, new ArrayDeque<>());
        }
        if (totals.total <= 0) {
            totals.total = rootTotal;
        }
        return totals;
    }

    private static void visit(StackTraceNode node, List<StackTraceNode> table, Set<String> path, ThreadTotals totals, boolean underIdle, Deque<String> callStack) {
        String key = node.getClassName() + "." + node.getMethodName();
        // Everything below a wait frame is waiting, including the native frames async-profiler adds under it.
        boolean idle = underIdle || isIdleFrame(node, key);
        double inclusive = sum(node.getTimesList());
        List<StackTraceNode> children = childrenOf(node, table);
        double childrenInclusive = 0;
        for (StackTraceNode child : children) {
            childrenInclusive += sum(child.getTimesList());
        }
        double self = Math.max(0, inclusive - childrenInclusive);
        if (idle) {
            totals.idle += self;
        } else {
            totals.self.merge(key, self, Double::sum);
        }
        // Callers are only recorded at the outermost matching frame, so recursion does not double count.
        String parentKey = callStack.isEmpty() ? "" : callStack.peekLast();
        if (!idle && callerFilter != null && key.contains(callerFilter) && !parentKey.contains(callerFilter)) {
            totals.callers.merge(callerChain(callStack) + "  ->  " + node.getMethodName(), inclusive, Double::sum);
        }
        callStack.addLast(key);
        boolean firstOnPath = path.add(key);
        if (firstOnPath && !idle) {
            totals.inclusive.merge(key, inclusive, Double::sum);
        }
        for (StackTraceNode child : children) {
            visit(child, table, path, totals, idle, callStack);
        }
        callStack.removeLast();
        if (firstOnPath) {
            path.remove(key);
        }
    }

    private static String callerChain(Deque<String> callStack) {
        List<String> frames = new ArrayList<>(callStack);
        int from = Math.max(0, frames.size() - CALLER_CHAIN_DEPTH);
        List<String> shortFrames = new ArrayList<>();
        for (String frame : frames.subList(from, frames.size())) {
            int methodDot = frame.lastIndexOf('.');
            int classDot = frame.lastIndexOf('.', methodDot - 1);
            shortFrames.add(frame.substring(classDot + 1));
        }
        return String.join(" < ", shortFrames.reversed());
    }

    private static boolean isIdleFrame(StackTraceNode node, String key) {
        if (IDLE_FRAMES.contains(simpleKey(node))) {
            return true;
        }
        for (String marker : IDLE_NATIVE_MARKERS) {
            if (key.contains(marker)) {
                return true;
            }
        }
        return false;
    }

    private static List<StackTraceNode> childrenOf(StackTraceNode node, List<StackTraceNode> table) {
        List<StackTraceNode> children = new ArrayList<>();
        for (int index : node.getChildrenRefsList()) {
            StackTraceNode child = resolve(table, index);
            if (child != null) {
                children.add(child);
            }
        }
        return children;
    }

    private static StackTraceNode resolve(List<StackTraceNode> table, int index) {
        return index >= 0 && index < table.size() ? table.get(index) : null;
    }

    private static String simpleKey(StackTraceNode node) {
        String className = node.getClassName();
        return className.substring(className.lastIndexOf('.') + 1) + "." + node.getMethodName();
    }

    private static Map<String, Double> modOnly(Map<String, Double> values) {
        Map<String, Double> modValues = new HashMap<>();
        for (Map.Entry<String, Double> entry : values.entrySet()) {
            for (String prefix : MOD_PREFIXES) {
                if (entry.getKey().startsWith(prefix)) {
                    modValues.put(entry.getKey(), entry.getValue());
                    break;
                }
            }
        }
        return modValues;
    }

    private static void printRanking(String heading, String rowLabel, Map<String, Double> values, double threadTotal, int topCount, boolean allocation) {
        System.out.println("  " + heading);
        values.entrySet().stream()
                .filter(entry -> entry.getValue() > 0)
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(topCount)
                .forEach(entry -> System.out.printf(Locale.ROOT, "    %-8s %-16s %-8s %s%n", rowLabel,
                        formatValue(entry.getValue(), allocation), percentOf(entry.getValue(), threadTotal), entry.getKey()));
    }

    private static String formatValue(double value, boolean allocation) {
        return allocation ? String.format(Locale.ROOT, "%,d B", Math.round(value)) : String.format(Locale.ROOT, "%.1f ms", value);
    }

    private static String percentOf(double value, double total) {
        return total <= 0 ? "0.00%" : String.format(Locale.ROOT, "%.2f%%", value * 100.0 / total);
    }

    private static double sum(List<Double> values) {
        double total = 0;
        for (double value : values) {
            total += value;
        }
        return total;
    }

    private static final class ThreadTotals {
        private final String name;
        private double total;
        private double idle;
        private final Map<String, Double> self = new HashMap<>();
        private final Map<String, Double> inclusive = new HashMap<>();
        private final Map<String, Double> callers = new HashMap<>();

        private ThreadTotals(String name) {
            this.name = name;
        }
    }
}
