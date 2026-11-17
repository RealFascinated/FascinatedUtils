package cc.fascinated.fascinatedutils.common.util;

import lombok.experimental.UtilityClass;

import java.util.regex.Pattern;

@UtilityClass
public class StringUtils {

    private static final Pattern LEGACY_COLOR_PATTERN = Pattern.compile("(?i)[&§][0-9A-FK-OR]");
    private static final Pattern LEGACY_HEX_PATTERN = Pattern.compile("(?i)(?:[&§]x)(?:[&§][0-9A-F]){6}");
    private static final Pattern HASH_HEX_PATTERN = Pattern.compile("(?i)[&§]#[0-9A-F]{6}");

    /**
     * Capitalizes the first letter of the string.
     */
    public static String capitalize(String string) {
        if (string == null || string.isEmpty()) {
            return "";
        }
        return string.substring(0, 1).toUpperCase() + string.substring(1);
    }

    /**
     * Removes legacy and modern Minecraft color/formatting codes from text.
     */
    public static String stripColors(String string) {
        if (string == null || string.isEmpty()) {
            return "";
        }
        String withoutHex = LEGACY_HEX_PATTERN.matcher(string).replaceAll("");
        String withoutHashHex = HASH_HEX_PATTERN.matcher(withoutHex).replaceAll("");
        return LEGACY_COLOR_PATTERN.matcher(withoutHashHex).replaceAll("");
    }
}
