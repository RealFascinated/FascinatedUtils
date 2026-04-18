package cc.fascinated.fascinatedutils.gui.theme;

import lombok.experimental.UtilityClass;

@UtilityClass
public class UiColor {

    /**
     * Parse a CSS-style hex color into packed ARGB (alpha in the high byte, matching Minecraft {@code 0xAARRGGBB}).
     */
    public static int argb(String hexWithOptionalHash) {
        String digits = hexWithOptionalHash.trim();
        if (digits.startsWith("#")) {
            digits = digits.substring(1);
        }
        if (digits.length() != 6 && digits.length() != 8) {
            throw new IllegalArgumentException("UiColor.argb expected #RRGGBB (6) or #AARRGGBB (8) hex digits, got: " + hexWithOptionalHash);
        }
        long parsed = Long.parseLong(digits, 16);
        if (digits.length() == 6) {
            parsed |= 0xFF000000L;
        }
        return (int) parsed;
    }
}
