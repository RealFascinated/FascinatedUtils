package cc.fascinated.fascinatedutils.gui.hooks;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.util.Mth;

@Getter
@Setter
@Accessors(fluent = true)
public class AnimHandle {
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
     * {@code deltaSeconds}.
     *
     * @param deltaSeconds elapsed time this frame for the ease step, in seconds
     */
    public void tick(float deltaSeconds) {
        float rate = speed;
        float interpolation = 1f - (float) Math.exp(-rate * deltaSeconds);
        value = Mth.lerp(interpolation, value, target);
    }
}
