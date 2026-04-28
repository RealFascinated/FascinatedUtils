package cc.fascinated.fascinatedutils.common;

import lombok.experimental.UtilityClass;

import java.util.Locale;

@UtilityClass
public class DateUtils {

    /**
     * Formats a duration in ticks (20 ticks per second) into a human-readable string.
     *
     * @param durationTicks the duration to format, in ticks
     * @return the formatted duration string
     */
    public static String formatDuration(int durationTicks) {
        int totalSeconds = Math.max(0, durationTicks / 20);
        int hours = totalSeconds / 3600;
        int remainingSeconds = totalSeconds % 3600;
        int minutes = remainingSeconds / 60;
        int seconds = remainingSeconds % 60;
        if (hours > 0) {
            return String.format(Locale.ROOT, "%02d:%02d:%02d", hours, minutes, seconds);
        }
        return String.format(Locale.ROOT, "%02d:%02d", minutes, seconds);
    }
}