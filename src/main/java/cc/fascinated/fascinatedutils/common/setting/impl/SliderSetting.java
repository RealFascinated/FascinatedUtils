package cc.fascinated.fascinatedutils.common.setting.impl;

import cc.fascinated.fascinatedutils.common.NumberUtils;
import cc.fascinated.fascinatedutils.common.setting.Setting;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.util.Mth;
import org.jspecify.annotations.Nullable;

import java.util.function.Function;

@Getter
public class SliderSetting extends Setting<Number> {
    private final float min;
    private final float max;
    private final float step;
    @Setter
    private @Nullable Function<Number, String> valueFormatter;

    private SliderSetting(SliderSetting.Builder builder) {
        super(builder);
        this.min = builder.minValue;
        this.max = builder.maxValue;
        this.step = builder.step;
        this.valueFormatter = builder.valueFormatter;
        setValue(getValue());
    }

    public static SliderSetting.Builder builder() {
        return new SliderSetting.Builder();
    }

    /**
     * Snaps a scalar to the nearest {@code step} increment within {@code min} and {@code max}.
     */
    public static float snapValue(float rawValue, float min, float max, float step) {
        if (step <= 0f || Float.isNaN(step)) {
            return Mth.clamp(rawValue, min, max);
        }
        float stepIndex = (rawValue - min) / step;
        float snapped = min + Math.round(stepIndex) * step;
        if (snapped < min) {
            snapped = min;
        }
        if (snapped > max) {
            snapped = max;
        }
        return snapped;
    }

    /**
     * Human-readable string for the slider value in settings UI; uses {@link #valueFormatter} when set, otherwise
     * {@link NumberUtils#formatCompactByStep(float, float)} with a multiplier suffix.
     */
    public String formatValueForDisplay() {
        if (valueFormatter != null) {
            return valueFormatter.apply(getValue());
        }
        return NumberUtils.formatCompactByStep(getValue().floatValue(), step) + "×";
    }

    /**
     * Assigns a slider value snapped to {@link #step} and clamped between {@link #min} and {@link #max}.
     * {@code null} resets to the snapped default.
     */
    @Override
    public void setValue(Number value) {
        if (value == null) {
            super.setValue(snapValue(getDefaultValue().floatValue(), min, max, step));
            return;
        }
        super.setValue(snapValue(Mth.clamp(value.floatValue(), min, max), min, max, step));
    }

    /**
     * Is the current value the same as the default value?
     */
    public boolean isDefault() {
        return getValue().equals(getDefaultValue());
    }

    /**
     * Restores the slider to its snapped registration default.
     */
    @Override
    public void resetToDefault() {
        setValue(getDefaultValue());
    }

    @Override
    public @Nullable JsonElement serializeValue() {
        return new JsonPrimitive(getValue());
    }

    @Override
    public void deserializeValue(@Nullable JsonElement json) {
        if (json != null && json.isJsonPrimitive() && json.getAsJsonPrimitive().isNumber()) {
            setValue(json.getAsFloat());
        }
    }

    public static class Builder extends Setting.Builder<Number, SliderSetting.Builder> {
        private float minValue;
        private float maxValue;
        private float step = 1f;
        private @Nullable Function<Number, String> valueFormatter;

        public SliderSetting.Builder minValue(float minValue) {
            this.minValue = minValue;
            return this;
        }

        public SliderSetting.Builder maxValue(float maxValue) {
            this.maxValue = maxValue;
            return this;
        }

        public SliderSetting.Builder step(float step) {
            this.step = step;
            return this;
        }

        public SliderSetting.Builder valueFormatter(@Nullable Function<Number, String> valueFormatter) {
            this.valueFormatter = valueFormatter;
            return this;
        }

        @Override
        public SliderSetting build() {
            return new SliderSetting(this);
        }

        @Override
        protected SliderSetting.Builder self() {
            return this;
        }
    }
}
