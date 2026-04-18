package cc.fascinated.fascinatedutils.common;

import lombok.experimental.UtilityClass;
import net.minecraft.util.Mth;

import java.util.Locale;

@UtilityClass
public class ColorUtils {

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
