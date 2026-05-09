package cc.fascinated.fascinatedutils.gui2.layout;

/**
 * Single layout value with measure mode.
 */
public class LayoutValue {
    private final float value;
    private final Measure measure;

    public LayoutValue(float value, Measure measure) {
        this.value = value;
        this.measure = measure;
    }

    public static LayoutValue pixels(int value) {
        return new LayoutValue(value, Measure.PIXELS);
    }

    public static LayoutValue relative(float value) {
        return new LayoutValue(value, Measure.RELATIVE);
    }

    public float resolve(float parentSize) {
        if (measure == Measure.RELATIVE) {
            return parentSize * value;
        }
        return value;
    }

    public float value() {
        return value;
    }

    public Measure measure() {
        return measure;
    }
}
