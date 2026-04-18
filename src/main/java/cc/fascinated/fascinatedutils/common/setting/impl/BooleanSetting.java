package cc.fascinated.fascinatedutils.common.setting.impl;

import cc.fascinated.fascinatedutils.common.setting.Setting;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import org.jspecify.annotations.Nullable;

public class BooleanSetting extends Setting<Boolean> {
    private BooleanSetting(Builder builder) {
        super(builder);
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Whether this toggle is currently on ({@code true}).
     */
    public boolean isEnabled() {
        return Boolean.TRUE.equals(getValue());
    }

    /**
     * Whether this toggle is currently off ({@code false}) or unset.
     */
    public boolean isDisabled() {
        return !isEnabled();
    }

    @Override
    public @Nullable JsonElement serializeValue() {
        return new JsonPrimitive(getValue());
    }

    @Override
    public void deserializeValue(@Nullable JsonElement json) {
        if (json != null && json.isJsonPrimitive()) {
            setValue(json.getAsBoolean());
        }
    }

    public static class Builder extends Setting.Builder<Boolean, Builder> {
        @Override
        public BooleanSetting build() {
            return new BooleanSetting(this);
        }

        @Override
        protected Builder self() {
            return this;
        }
    }
}
