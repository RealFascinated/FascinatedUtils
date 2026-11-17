package cc.fascinated.fascinatedutils.systems.modules.impl.cps;

import cc.fascinated.fascinatedutils.event.FascinatedEventBus;
import cc.fascinated.fascinatedutils.event.impl.mouse.MouseClickEvent;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

public class MouseClickRateTracker {
    public static final MouseClickRateTracker INSTANCE = new MouseClickRateTracker();

    private static final long WINDOW_MILLIS = 1000L;
    private static final int RING_CAPACITY = 256;

    private final ClickRing leftClicks = new ClickRing();
    private final ClickRing rightClicks = new ClickRing();
    private boolean registered;

    /**
     * Starts listening for mouse clicks. Safe to call more than once.
     */
    public void register() {
        if (registered) {
            return;
        }
        registered = true;
        FascinatedEventBus.INSTANCE.subscribe(this);
    }

    private void recordButtonAction(int button, int action) {
        if (action != GLFW.GLFW_PRESS) {
            return;
        }
        long nowMillis = System.currentTimeMillis();
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            leftClicks.add(nowMillis);
        }
        else if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
            rightClicks.add(nowMillis);
        }
    }

    @EventHandler
    private void onMouseClick(MouseClickEvent event) {
        // only track when no screen is showing (in-game)
        if (Minecraft.getInstance().screen == null) {
            recordButtonAction(event.mouseInput().button(), event.action());
        }
    }

    /**
     * Left button clicks in the last one second.
     *
     * @return the click count
     */
    public int leftClicksPerSecond() {
        return leftClicks.countSince(System.currentTimeMillis() - WINDOW_MILLIS);
    }

    /**
     * Right button clicks in the last one second.
     *
     * @return the click count
     */
    public int rightClicksPerSecond() {
        return rightClicks.countSince(System.currentTimeMillis() - WINDOW_MILLIS);
    }

    /**
     * Combined left and right button clicks in the last one second.
     *
     * @return the click count
     */
    public int combinedClicksPerSecond() {
        long cutoffMillis = System.currentTimeMillis() - WINDOW_MILLIS;
        return leftClicks.countSince(cutoffMillis) + rightClicks.countSince(cutoffMillis);
    }

    /**
     * Fixed-size ring of click timestamps; when full the oldest entry is overwritten.
     */
    private static class ClickRing {
        private final long[] timesMillis = new long[RING_CAPACITY];
        private int head;
        private int size;

        void add(long timeMillis) {
            int tail = (head + size) % RING_CAPACITY;
            timesMillis[tail] = timeMillis;
            if (size == RING_CAPACITY) {
                head = (head + 1) % RING_CAPACITY;
            }
            else {
                size++;
            }
        }

        int countSince(long cutoffMillis) {
            while (size > 0 && timesMillis[head] < cutoffMillis) {
                head = (head + 1) % RING_CAPACITY;
                size--;
            }
            return size;
        }
    }
}
