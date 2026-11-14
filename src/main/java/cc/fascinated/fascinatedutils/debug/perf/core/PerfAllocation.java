package cc.fascinated.fascinatedutils.debug.perf.core;

import com.sun.management.ThreadMXBean;

import java.lang.management.ManagementFactory;

/**
 * Reads how many bytes the current thread has allocated, when the JVM supports it.
 */
public class PerfAllocation {
    public static final PerfAllocation INSTANCE = new PerfAllocation();

    private final ThreadMXBean threadBean;

    private PerfAllocation() {
        ThreadMXBean bean = ManagementFactory.getPlatformMXBean(ThreadMXBean.class);
        if (bean != null && bean.isThreadAllocatedMemorySupported()) {
            bean.setThreadAllocatedMemoryEnabled(true);
            this.threadBean = bean;
        } else {
            this.threadBean = null;
        }
    }

    /**
     * Bytes allocated so far by the calling thread.
     *
     * @return the byte count, or -1 when the JVM cannot report it
     */
    public long currentThreadBytes() {
        return threadBean == null ? -1 : threadBean.getCurrentThreadAllocatedBytes();
    }
}
