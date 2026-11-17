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
    private final BooleanSupplier settingEnabled;
    private final Supplier<T> taskFactory;
    private final String threadName;
    private final Runnable onThreadStarted;
    private Thread thread;
    private boolean previousEnabledState = false;

    /**
     * Snapshot of the setting for hot paths on the render thread; refreshed each client tick.
     */
    private volatile boolean enabled = false;

    @Getter
    private volatile T task;

    /**
     * Whether the turbo system is on and has a running cull task, so culled flags can be trusted.
     *
     * @return true when mixins should apply culling
     */
    public boolean isEnabled() {
        return enabled && task != null;
    }

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
            if (nowEnabled && task != null && thread != null && !thread.isAlive()) {
                // The worker died; start a fresh one so nothing stays hidden forever.
                task = null;
                if (!startThread()) {
                    previousEnabledState = false;
                }
            }
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
            // A stopped worker may still be finishing its pass; never run two cull threads at once. Retry next tick
            // instead of blocking the client thread.
            return false;
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
