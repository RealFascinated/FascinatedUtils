package cc.fascinated.fascinatedutils.common;

public class MathUtils {
    /**
     * Clamps the given value between the given minimum and maximum values (inclusive).
     *
     * @param value    the value to clamp
     * @param minValue the minimum value to clamp to
     * @param maxValue the maximum value to clamp to
     * @return the clamped value
     */
    public static float clamp(float value, float minValue, float maxValue) {
        return Math.max(minValue, Math.min(maxValue, value));
    }
}
