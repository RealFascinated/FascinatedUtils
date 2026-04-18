package cc.fascinated.fascinatedutils.common;

import java.util.Locale;

public class EnumUtils {
    /**
     * Title-case words split on underscores (fallback when no translation is wired).
     */
    public static String formatEnumName(Enum<?> choice) {
        if (choice == null) {
            return "";
        }
        String[] parts = choice.name().split("_");
        StringBuilder builder = new StringBuilder();
        for (int partIndex = 0; partIndex < parts.length; partIndex++) {
            if (partIndex > 0) {
                builder.append(' ');
            }
            String part = parts[partIndex];
            if (part.isEmpty()) {
                continue;
            }
            builder.append(Character.toUpperCase(part.charAt(0)));
            if (part.length() > 1) {
                builder.append(part.substring(1).toLowerCase(Locale.ROOT));
            }
        }
        return builder.toString();
    }
}
