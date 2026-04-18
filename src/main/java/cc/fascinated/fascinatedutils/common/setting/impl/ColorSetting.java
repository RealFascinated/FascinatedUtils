package cc.fascinated.fascinatedutils.common.setting.impl;

import cc.fascinated.fascinatedutils.common.color.SettingColor;
import cc.fascinated.fascinatedutils.common.setting.Setting;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import lombok.Getter;

@Getter
public class ColorSetting extends Setting<SettingColor> {

    private ColorSetting(Builder builder) {
        super(builder);
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Returns the resolved ARGB color, applying rainbow if enabled.
     *
     * @return packed ARGB integer
     */
    public int getResolvedArgb() {
        return getValue().getResolvedArgb();
    }

    @Override
    public void setValue(SettingColor value) {
        if (value == null) {
            super.setValue(getDefaultValue().copy());
            return;
        }
        super.setValue(value);
    }

    @Override
    public void resetToDefault() {
        setValue(getDefaultValue().copy());
    }

    @Override
    public JsonElement serializeValue() {
        return getValue().toJson();
    }

    @Override
    public void deserializeValue(JsonElement json) {
        if (json != null && json.isJsonObject()) {
            JsonObject jsonObject = json.getAsJsonObject();
            SettingColor color = getValue().copy();
            color.fromJson(jsonObject);
            setValue(color);
        }
    }

    public static class Builder extends Setting.Builder<SettingColor, Builder> {

        @Override
        public ColorSetting build() {
            return new ColorSetting(this);
        }

        @Override
        protected Builder self() {
            return this;
        }
    }
}
