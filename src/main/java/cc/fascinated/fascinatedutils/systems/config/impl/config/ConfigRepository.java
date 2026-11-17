package cc.fascinated.fascinatedutils.systems.config.impl.config;

import cc.fascinated.fascinatedutils.Constants;
import cc.fascinated.fascinatedutils.systems.settings.Settings;
import cc.fascinated.fascinatedutils.systems.config.ConfigManager;
import cc.fascinated.fascinatedutils.systems.config.io.SettingsJsonCodec;
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
        SettingsJsonCodec.INSTANCE.deserialize(settingsJson, Settings.INSTANCE.getSettings(), Constants.GSON, "global config");
    }

    public void save() {
        JsonObject captured = SettingsJsonCodec.INSTANCE.serialize(Settings.INSTANCE.getSettings(), Constants.GSON);
        FascinatedConfig current = configManager.getCurrent();
        configManager.updateAndSave(new FascinatedConfig(current.activeProfileId(), captured));
    }
}
