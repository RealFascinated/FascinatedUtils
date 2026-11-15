package cc.fascinated.fascinatedutils.gui.anim;

public class Easing {
    private static final float BACK_OVERSHOOT = 1.70158f;

    /**
     * Decelerating cubic curve.
     *
     * @param progress linear progress in the unit interval
     * @return eased progress in the unit interval
     */
    public static float easeOutCubic(float progress) {
        float inverse = 1f - clampUnit(progress);
        return 1f - inverse * inverse * inverse;
    }

    /**
     * Curve that overshoots the end value slightly before settling on it.
     *
     * @param progress linear progress in the unit interval
     * @return eased progress that briefly exceeds one
     */
    public static float easeOutBack(float progress) {
        float shifted = clampUnit(progress) - 1f;
        return 1f + (BACK_OVERSHOOT + 1f) * shifted * shifted * shifted + BACK_OVERSHOOT * shifted * shifted;
    }

    private static float clampUnit(float value) {
        return Math.max(0f, Math.min(1f, value));
    }
}
