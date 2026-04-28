package cc.fascinated.fascinatedutils.common;

import lombok.*;
import lombok.experimental.UtilityClass;

import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@UtilityClass
public final class TimeUtils {
    /**
     * Format a time in millis to a readable time format.
     *
     * @param millis the millis to format
     * @return the formatted time
     */
    @NonNull
    public static String format(long millis) {
        return format(millis, 2);
    }

    /**
     * Format a time in millis to a readable time format with multiple units.
     *
     * @param millis   the millis to format
     * @param maxUnits maximum number of units to display
     * @return the formatted time
     */
    @NonNull
    public static String format(long millis, int maxUnits) {
        if (millis == -1L) {
            return "Permanent";
        }

        if (millis == 0L) {
            return "0ms";
        }

        return formatDuration(millis, maxUnits);
    }

    /**
     * Returns a human-readable "time ago" string for a timestamp.
     * Examples:
     *  - just now
     *  - 5s ago
     *  - 3m, 12s ago
     *  - 2d, 4h ago
     *
     * @param inputMillis timestamp in millis
     * @param maxUnits maximum number of units to display
     * @return formatted time-ago string
     */
    public static String timeAgo(long inputMillis, int maxUnits) {
        long now = System.currentTimeMillis();
        long remaining = now - inputMillis;
        return formatDuration(remaining, maxUnits) + " ago";
    }

    /**
     * Internal helper to format a duration in millis into a readable string.
     *
     * @param millis   the duration in millis
     * @param maxUnits maximum number of units to display
     * @return formatted duration string
     */
    private static String formatDuration(long millis, int maxUnits) {
        long remaining = millis;
        StringBuilder result = new StringBuilder();
        int usedUnits = 0;

        for (AlumiteTimeUnit unit : AlumiteTimeUnit.VALUES) {
            long unitMillis = unit.getMillis();
            long count = remaining / unitMillis;

            if (count > 0) {
                if (usedUnits > 0) {
                    result.append(", ");
                }
                result.append(count).append(unit.getSuffix());
                remaining -= count * unitMillis;
                usedUnits++;
            }

            if (usedUnits >= maxUnits) {
                break;
            }
        }

        return result.toString();
    }

    /**
     * Convert the given input into a time in millis.
     * <p>
     * E.g: 1d, 1h, 1d1h, etc
     * </p>
     *
     * @param input the input to parse
     * @return the time in millis
     */
    public static long fromString(@NonNull String input) {
        Matcher matcher = AlumiteTimeUnit.SUFFIX_PATTERN.matcher(input);
        long millis = 0;

        while (matcher.find()) {
            int amount = Integer.parseInt(matcher.group(1));
            String suffix = matcher.group(2);
            AlumiteTimeUnit timeUnit = AlumiteTimeUnit.fromSuffix(suffix);
            if (timeUnit != null) {
                millis += amount * timeUnit.getMillis();
            }
        }
        return millis;
    }

    /**
     * Represents a unit of time.
     */
    @NoArgsConstructor
    @AllArgsConstructor
    @Getter(AccessLevel.PRIVATE)
    @ToString
    public enum AlumiteTimeUnit {
        YEARS("y", TimeUnit.DAYS.toMillis(365L)),
        MONTHS("mo", TimeUnit.DAYS.toMillis(30L)),
        WEEKS("w", TimeUnit.DAYS.toMillis(7L)),
        DAYS("d", TimeUnit.DAYS.toMillis(1L)),
        HOURS("h", TimeUnit.HOURS.toMillis(1L)),
        MINUTES("m", TimeUnit.MINUTES.toMillis(1L)),
        SECONDS("s", TimeUnit.SECONDS.toMillis(1L)),
        MILLISECONDS("ms", 1L);

        /**
         * Our cached unit values.
         */
        public static final AlumiteTimeUnit[] VALUES = values();

        /**
         * Our cached suffix pattern.
         */
        public static final Pattern SUFFIX_PATTERN = Pattern.compile("(\\d+)(mo|ms|[ywdhms])");

        /**
         * The suffix of this time unit.
         */
        private String suffix;

        /**
         * The amount of millis in this time unit.
         */
        private long millis;

        /**
         * Get the time unit with the given suffix.
         *
         * @param suffix the time unit suffix
         * @return the time unit, null if not found
         */
        public static TimeUtils.AlumiteTimeUnit fromSuffix(@NonNull String suffix) {
            for (AlumiteTimeUnit unit : VALUES) {
                if (unit.getSuffix().equals(suffix)) {
                    return unit;
                }
            }
            return null;
        }
    }
}