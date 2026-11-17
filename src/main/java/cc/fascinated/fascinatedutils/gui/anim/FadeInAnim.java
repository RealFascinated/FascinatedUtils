package cc.fascinated.fascinatedutils.gui.anim;

import lombok.Getter;
import lombok.experimental.Accessors;

/**
 * A simple fade-in/out animation backed by an {@link AnimHandle}.
 *
 * <p>Usage:
 * <pre>{@code
 * private final FadeInAnim anim = new FadeInAnim(300f);
 *
 * // each frame:
 * anim.tick(deltaSeconds);
 * anim.show(); // or anim.hide()
 * if (anim.isVisible()) drawContent(anim.progress().value());
 * }</pre>
 */
public class FadeInAnim {

    /**
     * The underlying easing handle, useful for reading the raw progress value.
     */
    @Getter
    @Accessors(fluent = true)
    private final AnimHandle progress;

    /**
     * Create a fade animation that completes (to ~95%) in the given duration.
     *
     * @param durationMs time to reach ~95% of the target value, in milliseconds
     * @throws IllegalArgumentException when the duration is not positive
     */
    public FadeInAnim(float durationMs) {
        if (!(durationMs > 0f)) {
            throw new IllegalArgumentException("durationMs must be positive, got " + durationMs);
        }
        this.progress = new AnimHandle(0f).speed(3000f / durationMs);
    }

    /**
     * Begin animating toward the fully-visible state.
     */
    public void show() {
        progress.target(1f);
    }

    /**
     * Begin animating toward the fully-hidden state.
     */
    public void hide() {
        progress.target(0f);
    }

    /**
     * Whether any content should be rendered (progress is above the render threshold).
     *
     * @return true when content is at least partially visible
     */
    public boolean isVisible() {
        return progress.value() > 0.01f;
    }

    /**
     * Advance the animation by one frame.
     *
     * @param deltaSeconds elapsed time in seconds since the last frame
     */
    public void tick(float deltaSeconds) {
        progress.tick(deltaSeconds);
    }
}
