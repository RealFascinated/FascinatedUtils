package cc.fascinated.fascinatedutils.gui.node.input;

/**
 * Converts between raw input text and a typed value {@code V}.
 *
 * <p>Implement this interface to support custom value types in {@link TextInputNode}.
 * Common parsers are provided as interface constants.
 *
 * @param <V> the value type produced by this parser
 */
public interface TextParser<V> {

    /**
     * Parses the given input text into a value of type {@code V}.
     *
     * @param text the current input text
     * @return the parsed value, or {@code null} if the text is not a valid representation
     */
    V parse(String text);

    /**
     * Formats a value of type {@code V} back to its display string.
     *
     * @param value the value to format
     * @return the display string
     */
    String format(V value);

    /**
     * Returns whether the given text is a valid representation of {@code V}.
     *
     * <p>Empty text is always considered valid (the field is simply empty). Non-empty text
     * is valid when {@link #parse} returns non-null. Override for stricter validation
     * (e.g. disallowing empty input).
     *
     * @param text the current input text
     * @return {@code true} if the text is valid
     */
    default boolean isValid(String text) {
        return text.isEmpty() || parse(text) != null;
    }

    TextParser<String> STRING = new TextParser<>() {
        @Override
        public String parse(String text) {
            return text;
        }

        @Override
        public String format(String value) {
            return value == null ? "" : value;
        }

        @Override
        public boolean isValid(String text) {
            return true;
        }
    };

    TextParser<Double> DOUBLE = new TextParser<>() {
        @Override
        public Double parse(String text) {
            try {
                double parsed = Double.parseDouble(text.trim());
                // NaN and +/-Infinity parse fine but are not usable coordinates or amounts.
                return Double.isFinite(parsed) ? parsed : null;
            } catch (NumberFormatException ignored) {
                return null;
            }
        }

        @Override
        public String format(Double value) {
            return String.valueOf(value);
        }
    };
}
