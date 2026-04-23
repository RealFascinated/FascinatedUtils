package cc.fascinated.fascinatedutils.common;

import lombok.experimental.UtilityClass;
import net.minecraft.util.Mth;

import java.util.Locale;

@UtilityClass
public class Colors {

    public static final int GOOD_COLOR = 0x00E676;
    public static final int WARNING_COLOR = 0xD6CD30;
    public static final int BAD_COLOR = 0xE53935;

    /**
     * Resolves a packed RGB color on the good→warning→bad gradient for a given percentage.
     *
     * @param percentGood fraction of maximum, clamped to the inclusive unit interval; 1.0 = good (green), 0.0 = bad (red)
     * @param reversed reverse the color scheme
     * @return RGB in the least significant 24 bits (alpha bits clear)
     */
    public static int getGoodBadColor(float percentGood, boolean reversed) {
        if (reversed) {
            percentGood = 1f - percentGood;
        }
        float clamped = Mth.clamp(percentGood, 0f, 1f);
        float eased = clamped * clamped * (3f - 2f * clamped);
        if (eased <= 0.5f) {
            return mixArgb(2f * eased, BAD_COLOR, WARNING_COLOR) & 0xFFFFFF;
        }
        return mixArgb(2f * (eased - 0.5f), WARNING_COLOR, GOOD_COLOR) & 0xFFFFFF;
    }

    /**
     * Linearly interpolates two packed ARGB colors in channel space.
     *
     * @param interpolation blend factor between the two colors, clamped to the inclusive unit interval
     * @param colorFrom     packed ARGB color at interpolation zero
     * @param colorTo       packed ARGB color at interpolation one
     * @return the interpolated packed ARGB color
     */
    public static int mixArgb(float interpolation, int colorFrom, int colorTo) {
        interpolation = Mth.clamp(interpolation, 0f, 1f);
        int alphaFrom = (colorFrom >>> 24) & 0xFF;
        int redFrom = (colorFrom >> 16) & 0xFF;
        int greenFrom = (colorFrom >> 8) & 0xFF;
        int blueFrom = colorFrom & 0xFF;
        int alphaTo = (colorTo >>> 24) & 0xFF;
        int redTo = (colorTo >> 16) & 0xFF;
        int greenTo = (colorTo >> 8) & 0xFF;
        int blueTo = colorTo & 0xFF;
        int alphaOut = Mth.floor(Mth.lerpInt(interpolation, alphaFrom, alphaTo));
        int redOut = Mth.floor(Mth.lerpInt(interpolation, redFrom, redTo));
        int greenOut = Mth.floor(Mth.lerpInt(interpolation, greenFrom, greenTo));
        int blueOut = Mth.floor(Mth.lerpInt(interpolation, blueFrom, blueTo));
        return (alphaOut << 24) | (redOut << 16) | (greenOut << 8) | blueOut;
    }

    /**
     * Formats the RGB channels of a packed ARGB value as a hex string; alpha is not included.
     *
     * @param argbColor packed ARGB color
     * @return a {@code #RRGGBB} string in English locale
     */
    public static String rgbHex(int argbColor) {
        return String.format(Locale.ENGLISH, "#%06X", argbColor & 0xFFFFFF);
    }
}
