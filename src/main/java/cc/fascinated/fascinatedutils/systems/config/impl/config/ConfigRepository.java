package cc.fascinated.fascinatedutils.systems.config.impl.config;

import cc.fascinated.fascinatedutils.common.setting.Setting;
import cc.fascinated.fascinatedutils.common.setting.impl.KeybindSetting;
import cc.fascinated.fascinatedutils.settings.SettingsRegistry;
import cc.fascinated.fascinatedutils.systems.config.ConfigManager;
import cc.fascinated.fascinatedutils.systems.config.ModConfig;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ConfigRepository {
    private final ConfigManager<FascinatedConfig> configManager;

    public void load() {
        JsonObject settingsJson = configManager.getCurrent().globalSettings();
        if (settingsJson == null) {
            return;
        }
        for (Setting<?> setting : SettingsRegistry.INSTANCE.getSettings().getSettings()) {
            JsonElement element = settingsJson.get(setting.getSettingKey());
            if (element != null && !element.isJsonNull()) {
                setting.deserialize(element, ModConfig.GSON);
            }
        }
    }

    public void save() {
        JsonObject captured = new JsonObject();
        for (Setting<?> setting : SettingsRegistry.INSTANCE.getSettings().getSettings()) {
            if (setting instanceof KeybindSetting) {
                continue;
            }
            JsonElement serialized = setting.serialize(ModConfig.GSON);
            if (!serialized.isJsonNull()) {
                captured.add(setting.getSettingKey(), serialized);
            }
        }
        FascinatedConfig current = configManager.getCurrent();
        configManager.updateAndSave(new FascinatedConfig(current.activeProfileId(), captured, current.uiState()));
    }
}
