package cc.fascinated.fascinatedutils.common;

import lombok.experimental.UtilityClass;
import net.minecraft.util.Mth;

@UtilityClass
public class PingColors {

    private static final int PING_START = 0;
    private static final int PING_MID = 150;
    private static final int PING_END = 300;

    private static final int ALPHA_MASK = 0xFF000000;

    private static final int COLOR_GREY = 0x535353;
    private static final int COLOR_START = Colors.GOOD_COLOR;
    private static final int COLOR_MID = Colors.WARNING_COLOR;
    private static final int COLOR_END = Colors.BAD_COLOR;

    /**
     * Resolve a packed ARGB text color for the given round-trip latency in milliseconds.
     *
     * @param latencyMs server-reported latency in milliseconds, or non-positive when unavailable
     * @return packed ARGB suitable for {@link net.minecraft.client.gui.Font} or MiniMessage hex via
     * {@link Colors#rgbHex(int)}
     */
    public static int getPingColor(int latencyMs) {
        return getPingColorInternal(latencyMs) | ALPHA_MASK;
    }

    private static int getPingColorInternal(int latencyMs) {
        if (latencyMs < PING_START) {
            return COLOR_GREY;
        }
        if (latencyMs < PING_MID) {
            return Colors.mixArgb(computeOffset(PING_START, PING_MID, latencyMs), ALPHA_MASK | COLOR_START, ALPHA_MASK | COLOR_MID);
        }
        return Colors.mixArgb(computeOffset(PING_MID, PING_END, Math.min(latencyMs, PING_END)), ALPHA_MASK | COLOR_MID, ALPHA_MASK | COLOR_END);
    }

    private static float computeOffset(int rangeStart, int rangeEnd, int value) {
        float offset = (value - rangeStart) / (float) (rangeEnd - rangeStart);
        return Mth.clamp(offset, 0f, 1f);
    }
}
