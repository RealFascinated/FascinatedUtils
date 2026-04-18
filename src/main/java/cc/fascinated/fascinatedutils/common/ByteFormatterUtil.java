package cc.fascinated.fascinatedutils.common;

import lombok.experimental.UtilityClass;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@UtilityClass
public class ByteFormatterUtil {
    private static final String[] UNITS = {"B", "KB", "MB", "GB", "TB", "PB", "EB", "ZB", "YB"};

    /**
     * Formats a byte count with a binary unit (B, KB, â€¦).
     */
    public static String formatBytes(long bytes, int precision) {
        int unitIndex = unitIndex(bytes);
        int digits = precision < 0 ? 2 : Math.min(precision, 15);
        return formatAmount(bytes, unitIndex, digits) + " " + UNITS[unitIndex];
    }

    /**
     * Formats one or more byte counts on the same scale and unit (scale follows the largest value).
     */
    public static ScaledByteComparison scaledByteComparison(int precision, long... byteCounts) {
        if (byteCounts.length == 0) {
            throw new IllegalArgumentException("At least one byte count is required");
        }
        long largest = 0L;
        for (long count : byteCounts) {
            largest = Math.max(largest, count);
        }
        int unitIndex = unitIndex(largest);
        int digits = precision < 0 ? 2 : Math.min(precision, 15);
        List<String> amounts = new ArrayList<>(byteCounts.length);
        for (long count : byteCounts) {
            amounts.add(formatAmount(count, unitIndex, digits));
        }
        return new ScaledByteComparison(List.copyOf(amounts), UNITS[unitIndex]);
    }

    private static int unitIndex(long bytes) {
        int unitIndex = 0;
        double scaled = bytes;
        while (scaled >= 1024 && unitIndex < UNITS.length - 1) {
            scaled /= 1024;
            unitIndex++;
        }
        return unitIndex;
    }

    private static String formatAmount(long bytes, int unitIndex, int fractionDigits) {
        double scaled = bytes;
        for (int index = 0; index < unitIndex; index++) {
            scaled /= 1024;
        }
        NumberFormat numberFormat = NumberFormat.getNumberInstance(Locale.ROOT);
        numberFormat.setGroupingUsed(false);
        numberFormat.setMinimumFractionDigits(fractionDigits);
        numberFormat.setMaximumFractionDigits(fractionDigits);
        return numberFormat.format(scaled);
    }

    public record ScaledByteComparison(List<String> amounts, String unit) {}
}
