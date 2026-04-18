package cc.fascinated.fascinatedutils.gui.widgets;

public interface FAnimatable {
    /**
     * Advance animations for this frame.
     *
     * @param deltaSeconds elapsed time since the last tick in seconds
     */
    void tickAnims(float deltaSeconds);
}
