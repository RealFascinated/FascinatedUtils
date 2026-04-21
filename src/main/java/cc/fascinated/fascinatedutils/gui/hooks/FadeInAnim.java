package cc.fascinated.fascinatedutils.gui.hooks;

import cc.fascinated.fascinatedutils.gui.renderer.GuiRenderer;

/**
 * A simple fade-in/out animation backed by an {@link AnimHandle}.
 *
 * <p>Usage:
 * <pre>{@code
 * private final FadeInAnim anim = new FadeInAnim();
 *
 * // each frame:
 * anim.tick(deltaSeconds);
 * anim.show(); // or anim.hide()
 * anim.render(guiRenderer, () -> drawContent(...));
 * }</pre>
 */
public class FadeInAnim {

    private final AnimHandle progress;

    /**
     * Create a fade animation with the default easing speed.
     */
    public FadeInAnim() {
        this.progress = new AnimHandle(0f).speed(10f);
    }

    /**
     * Create a fade animation with a custom easing speed.
     *
     * @param speed easing speed passed to the underlying {@link AnimHandle}
     */
    public FadeInAnim(float speed) {
        this.progress = new AnimHandle(0f).speed(speed);
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
     * Snap immediately to the visible state with no easing.
     */
    public void snapVisible() {
        progress.snap(1f);
    }

    /**
     * Snap immediately to the hidden state with no easing.
     */
    public void snapHidden() {
        progress.snap(0f);
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
     * The underlying easing handle, useful for reading the raw progress value.
     *
     * @return the progress {@link AnimHandle}
     */
    public AnimHandle progress() {
        return progress;
    }

    /**
     * Advance the animation by one frame.
     *
     * @param deltaSeconds elapsed time in seconds since the last frame
     */
    public void tick(float deltaSeconds) {
        progress.tick(deltaSeconds);
    }

    /**
     * Render the animated content if visible, applying the current alpha.
     *
     * @param guiRenderer the active renderer for this frame
     * @param content     draws the actual content
     */
    public void render(GuiRenderer guiRenderer, Runnable content) {
        if (!isVisible()) {
            return;
        }
        guiRenderer.setMultiplyAlpha(progress.value());
        content.run();
        guiRenderer.resetMultiplyAlpha();
    }
}
