package cc.fascinated.fascinatedutils.systems.config;

import cc.fascinated.fascinatedutils.common.setting.Setting;
import cc.fascinated.fascinatedutils.common.setting.impl.KeybindSetting;
import cc.fascinated.fascinatedutils.settings.Settings;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import lombok.experimental.UtilityClass;

@UtilityClass
public class SettingsPersistentState {

    /**
     * Merges JSON into live settings; a null {@code json} object is a no-op.
     *
     * @param settings the live settings registry to update
     * @param json     persisted values keyed by setting key
     */
    public static void apply(Settings settings, JsonObject json) {
        if (json == null) {
            return;
        }
        for (Setting<?> setting : settings.getSettings()) {
            JsonElement jsonElement = json.get(setting.getSettingKey());
            if (jsonElement == null || jsonElement.isJsonNull()) {
                continue;
            }
            applySettingValue(setting, jsonElement);
        }
    }

    /**
     * Serializes current setting values to JSON.
     *
     * @param settings the settings registry to read
     * @return a JSON object keyed by setting key
     */
    public static JsonObject capture(Settings settings) {
        JsonObject map = new JsonObject();
        for (Setting<?> setting : settings.getSettings()) {
            if (setting instanceof KeybindSetting) {
                continue;
            }
            JsonElement serialized = setting.serializeValue();
            if (serialized != null) {
                map.add(setting.getSettingKey(), serialized);
            }
        }
        return map;
    }

    private static void applySettingValue(Setting<?> setting, JsonElement jsonElement) {
        setting.deserializeValue(jsonElement);
    }
}
