package meteordevelopment.orbit;

public interface ICancellable {
    /**
     * Cancels this event.
     */
    default void cancel() {
        setCancelled(true);
    }

    /**
     * @return True if this event is cancelled.
     */
    boolean isCancelled();

    /**
     * Sets if this event is cancelled.
     *
     * @param cancelled Is cancelled
     */
    void setCancelled(boolean cancelled);
}
