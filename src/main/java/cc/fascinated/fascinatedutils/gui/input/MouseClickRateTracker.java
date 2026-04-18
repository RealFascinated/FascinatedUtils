package cc.fascinated.fascinatedutils.gui.input;

import cc.fascinated.fascinatedutils.event.FascinatedEventBus;
import cc.fascinated.fascinatedutils.event.impl.mouse.MouseClickEvent;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayDeque;

public class MouseClickRateTracker {
    public static final MouseClickRateTracker INSTANCE = new MouseClickRateTracker();

    private static final long WINDOW_MILLIS = 1000L;

    private final ArrayDeque<Long> leftClickTimesMillis = new ArrayDeque<>();
    private final ArrayDeque<Long> rightClickTimesMillis = new ArrayDeque<>();

    private MouseClickRateTracker() {
        FascinatedEventBus.INSTANCE.subscribe(this);
    }

    /**
     * Records one GLFW mouse button action.
     */
    public void recordButtonAction(int button, int action) {
        long nowMillis = System.currentTimeMillis();
        if (action == GLFW.GLFW_PRESS) {
            if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
                leftClickTimesMillis.addLast(nowMillis);
            }
            else if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
                rightClickTimesMillis.addLast(nowMillis);
            }
        }
        prune(nowMillis);
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
     */
    public int leftClicksPerSecond() {
        prune(System.currentTimeMillis());
        return leftClickTimesMillis.size();
    }

    /**
     * Right button clicks in the last one second.
     */
    public int rightClicksPerSecond() {
        prune(System.currentTimeMillis());
        return rightClickTimesMillis.size();
    }

    /**
     * Combined left and right button clicks in the last one second.
     */
    public int combinedClicksPerSecond() {
        prune(System.currentTimeMillis());
        return leftClickTimesMillis.size() + rightClickTimesMillis.size();
    }

    private void prune(long nowMillis) {
        long cutoffMillis = nowMillis - WINDOW_MILLIS;
        while (!leftClickTimesMillis.isEmpty() && leftClickTimesMillis.peekFirst() < cutoffMillis) {
            leftClickTimesMillis.removeFirst();
        }
        while (!rightClickTimesMillis.isEmpty() && rightClickTimesMillis.peekFirst() < cutoffMillis) {
            rightClickTimesMillis.removeFirst();
        }
    }
}
