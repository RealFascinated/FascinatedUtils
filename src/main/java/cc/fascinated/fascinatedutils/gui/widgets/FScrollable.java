package cc.fascinated.fascinatedutils.gui.widgets;

public interface FScrollable {
    /**
     * Apply vertical scroll delta in logical pixels.
     *
     * @param delta raw wheel delta from the platform
     * @return true if this widget consumed the scroll amount
     */
    boolean applyScroll(float delta);
}
