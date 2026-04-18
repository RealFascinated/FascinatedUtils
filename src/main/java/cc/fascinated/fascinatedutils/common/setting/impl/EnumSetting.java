package cc.fascinated.fascinatedutils.common.setting.impl;

import cc.fascinated.fascinatedutils.common.EnumUtils;
import cc.fascinated.fascinatedutils.common.setting.Setting;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import lombok.Setter;
import org.jspecify.annotations.Nullable;

import java.util.Objects;
import java.util.function.Function;

public class EnumSetting<T extends Enum<T>> extends Setting<T> {

    private final Class<T> enumType;
    @Setter
    private @Nullable Function<T, String> valueFormatter;

    private EnumSetting(Builder<T> builder) {
        super(builder);
        this.enumType = builder.enumType != null ? builder.enumType : inferEnumType(getDefaultValue());
        this.valueFormatter = builder.valueFormatter;
    }

    public static <E extends Enum<E>> Builder<E> builder() {
        return new Builder<>();
    }

    @SuppressWarnings("unchecked")
    private static <T extends Enum<T>> Class<T> inferEnumType(T defaultEnumValue) {
        Enum<?> typedDefaultValue = Objects.requireNonNull(defaultEnumValue, "default enum value is required");
        return (Class<T>) typedDefaultValue.getDeclaringClass();
    }

    /**
     * Formats the enum name for the UI to use.
     *
     * @param value the value to format
     * @return the formatted value
     */
    public String formatValueForDisplay(T value) {
        Function<T, String> formatter = valueFormatter;
        if (formatter != null) {
            return formatter.apply(value);
        }
        return EnumUtils.formatEnumName(value);
    }

    public String formatUntypedValueForDisplay(Enum<?> value) {
        return formatValueForDisplay(enumType.cast(value));
    }

    /**
     * Formats the current enum name for the UI to use.
     *
     * @return the formatted value
     */
    public String formatValueForDisplay() {
        return formatValueForDisplay(getValue());
    }

    /**
     * Enum class for deserialization from stored names.
     *
     * @return the enum class
     */
    public Class<T> enumType() {
        return enumType;
    }

    /**
     * Advance {@link #getValue()} to the next enum constant in declaration order (wraps after the last).
     */
    public void cycleNextConstant() {
        T[] constants = enumType.getEnumConstants();
        if (constants.length == 0) {
            return;
        }
        int nextOrdinal = (getValue().ordinal() + 1) % constants.length;
        setValue(constants[nextOrdinal]);
    }

    /**
     * Move {@link #getValue()} to the previous enum constant in declaration order (wraps before the first).
     */
    public void cyclePreviousConstant() {
        T[] constants = enumType.getEnumConstants();
        if (constants.length == 0) {
            return;
        }
        int previousOrdinal = (getValue().ordinal() - 1 + constants.length) % constants.length;
        setValue(constants[previousOrdinal]);
    }

    /**
     * Set the value from a persisted enum constant name; unknown names fall back to {@link #resetToDefault()}.
     *
     * @param name {@link Enum#name()} of the desired constant
     */
    public void setFromPersistentName(String name) {
        try {
            setValue(Enum.valueOf(enumType, name));
        } catch (IllegalArgumentException ignored) {
            resetToDefault();
        }
    }

    @Override
    public @Nullable JsonElement serializeValue() {
        return new JsonPrimitive(getValue().name());
    }

    @Override
    public void deserializeValue(@Nullable JsonElement json) {
        if (json != null && json.isJsonPrimitive() && json.getAsJsonPrimitive().isString()) {
            setFromPersistentName(json.getAsString());
        }
    }

    public static class Builder<T extends Enum<T>> extends Setting.Builder<T, Builder<T>> {
        private @Nullable Class<T> enumType;
        private @Nullable Function<T, String> valueFormatter;

        public Builder<T> enumType(Class<T> enumType) {
            this.enumType = enumType;
            return this;
        }

        public Builder<T> valueFormatter(@Nullable Function<T, String> valueFormatter) {
            this.valueFormatter = valueFormatter;
            return this;
        }

        @Override
        public EnumSetting<T> build() {
            return new EnumSetting<>(this);
        }

        @Override
        protected Builder<T> self() {
            return this;
        }
    }
}
