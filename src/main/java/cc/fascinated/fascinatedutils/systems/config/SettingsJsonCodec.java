package cc.fascinated.fascinatedutils.systems.config;

import cc.fascinated.fascinatedutils.client.Client;
import cc.fascinated.fascinatedutils.common.setting.Setting;
import cc.fascinated.fascinatedutils.common.setting.impl.KeybindSetting;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.List;

public class SettingsJsonCodec {
    public static final SettingsJsonCodec INSTANCE = new SettingsJsonCodec();

    /**
     * Writes each setting under its key. Keybinds are skipped and settings that serialise to JSON null are omitted.
     *
     * @param settings the settings to write
     * @param gson     the Gson instance used to serialise values
     * @return a JSON object keyed by setting key
     */
    public JsonObject serialize(List<Setting<?>> settings, Gson gson) {
        JsonObject json = new JsonObject();
        for (Setting<?> setting : settings) {
            if (setting instanceof KeybindSetting) {
                continue;
            }
            JsonElement serialized = setting.serialize(gson);
            if (!serialized.isJsonNull()) {
                json.add(setting.getSettingKey(), serialized);
            }
        }
        return json;
    }

    /**
     * Applies every setting that has a value in the JSON object. A value that fails to load is logged and skipped
     * so one bad value does not stop the remaining settings from loading.
     *
     * @param json     the JSON object keyed by setting key
     * @param settings the settings to apply
     * @param gson     the Gson instance used to deserialise values
     * @param owner    what the settings belong to, used in the log message
     */
    public void deserialize(JsonObject json, List<Setting<?>> settings, Gson gson, String owner) {
        for (Setting<?> setting : settings) {
            JsonElement value = json.get(setting.getSettingKey());
            if (value == null || value.isJsonNull()) {
                continue;
            }
            try {
                setting.deserialize(value, gson);
            } catch (RuntimeException exception) {
                Client.LOG.warn("Failed to load setting {} of {}: {}", setting.getSettingKey(), owner, exception.toString());
            }
        }
    }
}
