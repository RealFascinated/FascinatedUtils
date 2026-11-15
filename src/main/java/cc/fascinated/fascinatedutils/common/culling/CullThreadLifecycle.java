package cc.fascinated.fascinatedutils.common.culling;

import lombok.Getter;

import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

/**
 * Mirrors a turbo setting and starts or stops its daemon cull thread when the setting changes.
 *
 * @param <T> the cull task type run on the thread
 */
public class CullThreadLifecycle<T extends AbstractCullTask> {
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
    private T task;

    public CullThreadLifecycle(BooleanSupplier settingEnabled, Supplier<T> taskFactory, String threadName, Runnable onThreadStarted) {
        this.settingEnabled = settingEnabled;
        this.taskFactory = taskFactory;
        this.threadName = threadName;
        this.onThreadStarted = onThreadStarted;
    }

    public boolean isThreadAlive() {
        return thread != null && thread.isAlive();
    }

    public void onClientStarted() {
        previousEnabledState = settingEnabled.getAsBoolean();
        enabled = previousEnabledState;
        if (previousEnabledState) {
            startThread();
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
            startThread();
        }
        else {
            stopThread();
        }
    }

    public void onClientStopping() {
        enabled = false;
        stopThread();
    }

    private void startThread() {
        if (thread != null && thread.isAlive()) {
            return;
        }
        task = taskFactory.get();
        onThreadStarted.run();
        thread = new Thread(task, threadName);
        thread.setDaemon(true);
        thread.start();
    }

    private void stopThread() {
        if (task != null) {
            task.stop();
        }
        thread = null;
        task = null;
    }
}
