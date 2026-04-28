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

    /**
     * Formats a timestamp into a human-readable "time ago" string.
     *
     * @param timestamp the timestamp to format, in milliseconds since epoch
     * @return the formatted "time ago" string
     */
    private static String formatSecsAgo(long secsAgo) {
        if (secsAgo < 60) { return "just now"; }
        if (secsAgo < 3600) { return (secsAgo / 60) + "m ago"; }
        if (secsAgo < 86400) { return (secsAgo / 3600) + "h ago"; }
        return (secsAgo / 86400) + "d ago";
    }
}