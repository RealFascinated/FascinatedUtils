package cc.fascinated.fascinatedutils.gui.core.overlay;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Ordered registry of {@link UiOverlay}s. It routes mouse input to overlays from the topmost down, before the game or
 * a screen sees it, and counts frames so overlays that stopped rendering stop receiving input.
 */
public class UiOverlays {
    public static final UiOverlays INSTANCE = new UiOverlays();

    /**
     * Order of overlays anchored to the world, such as waypoint labels.
     */
    public static final int ORDER_WORLD = 0;
    /**
     * Order of the HUD.
     */
    public static final int ORDER_HUD = 100;
    /**
     * Order of toasts, above everything else.
     */
    public static final int ORDER_TOASTS = 200;

    private record Entry(UiOverlay overlay, int order) {}

    private final List<Entry> entries = new ArrayList<>();
    private long frame;

    /**
     * Registers an overlay for input routing.
     *
     * @param overlay the overlay to register
     * @param order   stacking order; higher orders sit on top and receive input first
     * @return the overlay, for chaining
     */
    public synchronized UiOverlay register(UiOverlay overlay, int order) {
        entries.add(new Entry(overlay, order));
        entries.sort(Comparator.comparingInt(Entry::order).reversed());
        return overlay;
    }

    /**
     * The index of the frame being built. Overlays stamp it when rendered.
     *
     * @return the current frame index
     */
    long frame() {
        return frame;
    }

    /**
     * Ends the frame once every overlay has rendered, so the next input phase sees overlays rendered this frame as live.
     */
    public void endFrame() {
        frame++;
    }

    /**
     * Routes a mouse button to overlays from the topmost down.
     *
     * @param button  the GLFW mouse button
     * @param pressed true for a press, false for a release
     * @return true when an overlay consumed the event
     */
    public synchronized boolean dispatchMouseButton(int button, boolean pressed) {
        for (Entry entry : entries) {
            if (entry.overlay().handleMouseButton(button, pressed)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Routes a scroll to overlays from the topmost down.
     *
     * @param delta the vertical scroll amount
     * @return true when an overlay consumed the scroll
     */
    public synchronized boolean dispatchScroll(float delta) {
        for (Entry entry : entries) {
            if (entry.overlay().handleScroll(delta)) {
                return true;
            }
        }
        return false;
    }
}
