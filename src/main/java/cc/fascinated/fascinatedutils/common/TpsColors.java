package cc.fascinated.fascinatedutils.common;

import lombok.experimental.UtilityClass;
import net.minecraft.util.Mth;

@UtilityClass
public class TpsColors {

    private static final float TPS_REFERENCE = 20f;

    private static final int COLOR_GREY = 0x535353;
    private static final int COLOR_START = Colors.GOOD_COLOR;
    private static final int COLOR_MID = Colors.WARNING_COLOR;
    private static final int COLOR_END = Colors.BAD_COLOR;

    /**
     * Resolve a packed RGB color (lower 24 bits only) for the given server tick rate.
     *
     * @param tps ticks per second from measurement, or non-finite when unavailable
     * @return RGB in the least significant 24 bits (alpha bits clear), suitable for {@link Colors#rgbHex(int)}
     */
    public static int getTpsColor(float tps) {
        if (!Float.isFinite(tps) || tps < 0f) {
            return COLOR_GREY;
        }
        if (tps >= TPS_REFERENCE) {
            return COLOR_START;
        }
        float normalized = tps / TPS_REFERENCE;
        float eased = smoothstepUnit(normalized);
        if (eased <= 0.5f) {
            return Colors.mixArgb(2f * eased, COLOR_END, COLOR_MID) & 0xFFFFFF;
        }
        return Colors.mixArgb(2f * (eased - 0.5f), COLOR_MID, COLOR_START) & 0xFFFFFF;
    }

    private static float smoothstepUnit(float interpolation) {
        float clamped = Mth.clamp(interpolation, 0f, 1f);
        return clamped * clamped * (3f - 2f * clamped);
    }
}
