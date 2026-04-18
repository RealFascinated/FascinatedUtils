package cc.fascinated.fascinatedutils.common;

import lombok.experimental.UtilityClass;
import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;
import java.math.RoundingMode;

@UtilityClass
public class NumberUtils {

    public static String toRomanNumeral(int value) {
        if (value <= 0) {
            return "I";
        }
        if (value > 20) {
            return Integer.toString(value);
        }
        int[] arabicValues = {10, 9, 5, 4, 1};
        String[] romanSymbols = {"X", "IX", "V", "IV", "I"};
        int remainingValue = value;
        StringBuilder romanBuilder = new StringBuilder();
        for (int symbolIndex = 0; symbolIndex < arabicValues.length; symbolIndex++) {
            while (remainingValue >= arabicValues[symbolIndex]) {
                romanBuilder.append(romanSymbols[symbolIndex]);
                remainingValue -= arabicValues[symbolIndex];
            }
        }
        return romanBuilder.toString();
    }

    /**
     * Rounds a numeric value to a fixed number of fractional digits and returns a plain decimal string.
     *
     * @param num       value to format; when {@code null}, returns {@code "0"}
     * @param precision number of digits after the decimal point (clamped to at least zero)
     * @return decimal text without scientific notation, never using {@link String#format}
     */
    public static String formatNumber(@Nullable Number num, int precision) {
        if (num == null) {
            return "0";
        }
        double raw = num.doubleValue();
        if (Double.isNaN(raw)) {
            return "NaN";
        }
        if (Double.isInfinite(raw)) {
            return raw > 0d ? "Infinity" : "-Infinity";
        }
        int scale = Math.max(0, precision);
        BigDecimal value = toBigDecimal(num);
        if (value == null) {
            return num.toString();
        }
        BigDecimal rounded = value.setScale(scale, RoundingMode.HALF_UP);
        if (scale == 0) {
            return rounded.toBigInteger().toString();
        }
        return rounded.stripTrailingZeros().toPlainString();
    }

    /**
     * Compact text for a slider value, using a small number of fraction digits implied by {@code step}.
     *
     * @param value current slider value
     * @param step  slider step increment
     * @return plain decimal string suitable for inline labels
     */
    public static String formatCompactByStep(float value, float step) {
        int decimals = decimalPlacesForStep(step);
        return formatNumber(value, decimals);
    }

    private static @Nullable BigDecimal toBigDecimal(@Nullable Number num) {
        if (num == null) {
            return null;
        }
        Number number = num;
        if (number instanceof BigDecimal bigDecimal) {
            return bigDecimal;
        }
        if (number instanceof Long || number instanceof Integer || number instanceof Short || number instanceof Byte) {
            return BigDecimal.valueOf(number.longValue());
        }
        if (number instanceof Float || number instanceof Double) {
            return BigDecimal.valueOf(number.doubleValue());
        }
        try {
            return new BigDecimal(number.toString());
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private static int decimalPlacesForStep(float step) {
        if (step <= 0f || Float.isNaN(step) || Float.isInfinite(step)) {
            return 2;
        }
        BigDecimal stepDecimal = new BigDecimal(Float.toString(step));
        return Math.max(0, stepDecimal.stripTrailingZeros().scale());
    }
}
