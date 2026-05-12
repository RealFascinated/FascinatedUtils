package cc.fascinated.fascinatedutils.gui2.node.input;

/**
 * Converts between raw input text and a typed value {@code V}.
 *
 * <p>Implement this interface to support custom value types in {@link TextInputNode} and
 * {@link TextboxInputNode}. Several common parsers are provided as interface constants.
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

    TextParser<Integer> INTEGER = new TextParser<>() {
        @Override
        public Integer parse(String text) {
            try {
                return Integer.parseInt(text.trim());
            } catch (NumberFormatException ignored) {
                return null;
            }
        }

        @Override
        public String format(Integer value) {
            return String.valueOf(value);
        }
    };

    TextParser<Long> LONG = new TextParser<>() {
        @Override
        public Long parse(String text) {
            try {
                return Long.parseLong(text.trim());
            } catch (NumberFormatException ignored) {
                return null;
            }
        }

        @Override
        public String format(Long value) {
            return String.valueOf(value);
        }
    };

    TextParser<Float> FLOAT = new TextParser<>() {
        @Override
        public Float parse(String text) {
            try {
                return Float.parseFloat(text.trim());
            } catch (NumberFormatException ignored) {
                return null;
            }
        }

        @Override
        public String format(Float value) {
            return String.valueOf(value);
        }
    };

    TextParser<Double> DOUBLE = new TextParser<>() {
        @Override
        public Double parse(String text) {
            try {
                return Double.parseDouble(text.trim());
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
