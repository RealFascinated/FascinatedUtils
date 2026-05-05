package cc.fascinated.fascinatedutils.gui.core;

import lombok.experimental.UtilityClass;

import java.util.concurrent.atomic.AtomicInteger;

@UtilityClass
public class UiFocusIds {

    /**
     * Sentinel meaning no element currently holds keyboard focus.
     */
    public static final int NO_FOCUS_ID = Integer.MIN_VALUE;

    private static final AtomicInteger NEXT_ID = new AtomicInteger(1);

    /**
     * Allocates a new unique focus ID.
     *
     * @return a positive integer not equal to {@link #NO_FOCUS_ID}
     */
    public static int allocate() {
        return NEXT_ID.getAndIncrement();
    }
}
