package cc.fascinated.fascinatedutils.common;

import lombok.experimental.UtilityClass;

@UtilityClass
public class TpsColors {

    private static final float TPS_REFERENCE = 20f;

    /**
     * Resolve a packed RGB color (lower 24 bits only) for the given server tick rate.
     *
     * @param tps ticks per second from measurement, or non-finite when unavailable
     * @return RGB in the least significant 24 bits (alpha bits clear), suitable for {@link Colors#rgbHex(int)}
     */
    public static int getTpsColor(float tps) {
        if (!Float.isFinite(tps) || tps < 0f) {
            return Colors.GREY_COLOR;
        }
        return Colors.getGoodBadColor(tps / TPS_REFERENCE, false);
    }
}
