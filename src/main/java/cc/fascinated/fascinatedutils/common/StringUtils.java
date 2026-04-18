package cc.fascinated.fascinatedutils.common;

import lombok.experimental.UtilityClass;

import java.security.SecureRandom;
import java.util.regex.Pattern;

@UtilityClass
public class StringUtils {
    public static final String ALPHANUMERIC_LOWERCASE = "abcdefghijklmnopqrstuvwxyz0123456789";

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final Pattern LEGACY_COLOR_PATTERN = Pattern.compile("(?i)[&§][0-9A-FK-OR]");
    private static final Pattern LEGACY_HEX_PATTERN = Pattern.compile("(?i)(?:[&§]x)(?:[&§][0-9A-F]){6}");
    private static final Pattern HASH_HEX_PATTERN = Pattern.compile("(?i)[&§]#[0-9A-F]{6}");

    /**
     * Builds a random string by sampling characters from {@code alphabet} with a secure RNG.
     */
    public static String randomString(int length, String alphabet) {
        if (length <= 0 || alphabet == null || alphabet.isEmpty()) {
            return "";
        }
        int alphabetLength = alphabet.length();
        StringBuilder randomBuilder = new StringBuilder(length);
        for (int index = 0; index < length; index++) {
            randomBuilder.append(alphabet.charAt(SECURE_RANDOM.nextInt(alphabetLength)));
        }
        return randomBuilder.toString();
    }

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
