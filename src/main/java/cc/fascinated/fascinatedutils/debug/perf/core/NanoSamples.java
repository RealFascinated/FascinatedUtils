package cc.fascinated.fascinatedutils.debug.perf.core;

import java.util.Arrays;

/**
 * Growable, thread-safe buffer of duration samples in nanoseconds.
 */
public class NanoSamples {
    private long[] values = new long[256];
    private int count;

    /**
     * Appends one sample.
     *
     * @param nanos the duration in nanoseconds
     */
    public synchronized void add(long nanos) {
        if (count == values.length) {
            values = Arrays.copyOf(values, values.length * 2);
        }
        values[count++] = nanos;
    }

    /**
     * Copies the recorded samples.
     *
     * @return an array holding exactly the recorded samples
     */
    public synchronized long[] toArray() {
        return Arrays.copyOf(values, count);
    }
}
