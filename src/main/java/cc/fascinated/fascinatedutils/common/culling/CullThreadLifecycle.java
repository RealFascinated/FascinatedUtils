package cc.fascinated.fascinatedutils.common.culling;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

/**
 * Mirrors a turbo setting and starts or stops its daemon cull thread when the setting changes.
 *
 * @param <T> the cull task type run on the thread
 */
@RequiredArgsConstructor
public class CullThreadLifecycle<T extends AbstractCullTask> {
    private static final long WORKER_EXIT_TIMEOUT_MILLIS = 100L;

    private final BooleanSupplier settingEnabled;
    private final Supplier<T> taskFactory;
    private final String threadName;
    private final Runnable onThreadStarted;
    private Thread thread;
    private boolean previousEnabledState = false;

    /**
     * Snapshot of the setting for hot paths on the render thread; refreshed each client tick.
     */
    @Getter
    private volatile boolean enabled = false;

    @Getter
    private volatile T task;

    public boolean isThreadAlive() {
        return task != null && thread != null && thread.isAlive();
    }

    public void onClientStarted() {
        previousEnabledState = settingEnabled.getAsBoolean();
        enabled = previousEnabledState;
        if (previousEnabledState && !startThread()) {
            previousEnabledState = false;
        }
    }

    public void onClientTick() {
        boolean nowEnabled = settingEnabled.getAsBoolean();
        enabled = nowEnabled;
        if (nowEnabled == previousEnabledState) {
            return;
        }

        previousEnabledState = nowEnabled;
        if (nowEnabled) {
            if (!startThread()) {
                // The previous worker has not exited yet; retry on the next tick.
                previousEnabledState = false;
            }
        }
        else {
            stopThread();
        }
    }

    public void onClientStopping() {
        enabled = false;
        stopThread();
    }

    private boolean startThread() {
        if (thread != null && thread.isAlive()) {
            if (task != null) {
                return true;
            }
            // A stopped worker may still be finishing its pass; never run two cull threads at once.
            try {
                thread.join(WORKER_EXIT_TIMEOUT_MILLIS);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                return false;
            }
            if (thread.isAlive()) {
                return false;
            }
        }
        task = taskFactory.get();
        onThreadStarted.run();
        thread = new Thread(task, threadName);
        thread.setDaemon(true);
        thread.start();
        return true;
    }

    private void stopThread() {
        if (task != null) {
            task.stop();
        }
        task = null;
    }
}
