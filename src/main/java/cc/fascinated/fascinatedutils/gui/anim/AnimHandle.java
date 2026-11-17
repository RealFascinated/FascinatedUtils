package cc.fascinated.fascinatedutils.gui.anim;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.util.Mth;

@Getter
@Setter
@Accessors(fluent = true)
public class AnimHandle {
    private static final float SNAP_EPSILON = 0.001f;

    private float value;
    private float target;
    private float speed = 22f;

    /**
     * Create a handle whose animated value and target both start at the given amount.
     */
    public AnimHandle(float initialValue) {
        this.value = initialValue;
        this.target = initialValue;
    }

    /**
     * Set the current value and target immediately so no further easing occurs until they diverge again.
     */
    public void snap(float newValue) {
        this.value = newValue;
        this.target = newValue;
    }

    /**
     * Advance {@code value} one step toward {@code target} using exponential ease with rate {@code speed} over
     * {@code deltaSeconds}, snapping onto the target once within a thousandth. Negative deltas count as zero.
     *
     * @param deltaSeconds elapsed time this frame for the ease step, in seconds
     */
    public void tick(float deltaSeconds) {
        float clampedDelta = Math.max(0f, deltaSeconds);
        float interpolation = 1f - (float) Math.exp(-speed * clampedDelta);
        value = Mth.lerp(interpolation, value, target);
        if (Math.abs(target - value) < SNAP_EPSILON) {
            value = target;
        }
    }
}
