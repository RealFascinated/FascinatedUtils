package cc.fascinated.fascinatedutils.common;

/**
 * Exponential moving average smoother.
 *
 * <p>Each call to {@link #smooth(double, double)} blends the new sample toward the running average
 * using a time-corrected alpha so the feel is consistent regardless of call frequency.
 */
public class ValueSmoother {
    private final double smoothTimeSeconds;
    private double smoothed = Double.NaN;

    /**
     * @param smoothTimeMillis half-life in milliseconds — roughly how long it takes the average to
     *                         reach ~63% of a step change
     */
    public ValueSmoother(long smoothTimeMillis) {
        this.smoothTimeSeconds = smoothTimeMillis / 1000.0;
    }

    /**
     * Feed a new sample and return the smoothed value.
     *
     * @param value        the new raw sample
     * @param deltaSeconds elapsed seconds since the last call
     * @return the smoothed value
     */
    public double smooth(double value, double deltaSeconds) {
        if (Double.isNaN(smoothed)) {
            smoothed = value;
            return smoothed;
        }
        double alpha = 1.0 - Math.exp(-deltaSeconds / smoothTimeSeconds);
        smoothed += alpha * (value - smoothed);
        return smoothed;
    }

    /**
     * Returns the last smoothed value without updating it.
     */
    public double get() {
        return Double.isNaN(smoothed) ? 0.0 : smoothed;
    }

    /**
     * Resets the smoother so the next sample is adopted instantly.
     */
    public void reset() {
        smoothed = Double.NaN;
    }
}
