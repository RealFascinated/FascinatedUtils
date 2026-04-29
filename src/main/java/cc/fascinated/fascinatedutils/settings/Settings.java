package cc.fascinated.fascinatedutils.settings;

import cc.fascinated.fascinatedutils.client.keybind.Keybinds;
import cc.fascinated.fascinatedutils.common.ClientUtils;
import cc.fascinated.fascinatedutils.common.setting.Setting;
import cc.fascinated.fascinatedutils.common.setting.impl.BooleanSetting;
import cc.fascinated.fascinatedutils.common.setting.impl.KeybindSetting;
import cc.fascinated.fascinatedutils.systems.config.GsonSerializable;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

@Getter
public class Settings implements GsonSerializable<Settings> {

    private final List<Setting<?>> settings = new ArrayList<>();

    private final KeybindSetting shellOpenKeybind = new KeybindSetting("shell_open_keybind", Keybinds::openMenuKeybind);
    private final KeybindSetting socialKeybind = new KeybindSetting("social_keybind", Keybinds::socialKeybind);

    private final BooleanSetting showSelfNameplate = BooleanSetting.builder()
            .id("show_self_nameplate")
            .defaultValue(true)
            .categoryDisplayKey("General")
            .build();

    private final BooleanSetting showServerListInPauseMenu = BooleanSetting.builder()
            .id("show_server_list_in_pause_menu")
            .defaultValue(true)
            .categoryDisplayKey("General")
            .build();

    private final BooleanSetting confirmDisconnect = BooleanSetting.builder()
            .id("confirm_disconnect")
            .defaultValue(false)
            .categoryDisplayKey("General")
            .build();

    private final BooleanSetting reduceMacOSResolution = BooleanSetting.builder()
            .id("reduce_mac_os_resolution")
            .defaultValue(false)
            .locked(() -> !ClientUtils.isMacOS()).lockedReason(() -> "This Setting Requires MacOS")
            .categoryDisplayKey("Performance")
            .build();

    private final BooleanSetting turboEntities = BooleanSetting.builder()
            .id("turbo_entities")
            .defaultValue(true)
            .categoryDisplayKey("Performance")
            .build();

    private final BooleanSetting turboParticles = BooleanSetting.builder()
            .id("turbo_particles")
            .defaultValue(true)
            .categoryDisplayKey("Performance")
            .build();

    public Settings() {
        addSetting(shellOpenKeybind);
        addSetting(socialKeybind);
        addSetting(showSelfNameplate);
        addSetting(showServerListInPauseMenu);
        addSetting(confirmDisconnect);
        addSetting(reduceMacOSResolution);
        addSetting(turboEntities);
        addSetting(turboParticles);

        // ensure it's only enabled on macOS
        if (!ClientUtils.isMacOS() && reduceMacOSResolution.isEnabled()) {
            reduceMacOSResolution.setValue(false);
        }
    }

    private void addSetting(Setting<?> setting) {
        setting.setTranslationKeyPrefix("fascinatedutils.setting");
        this.settings.add(setting);
    }

    @Override
    public JsonElement serialize(Gson gson) {
        JsonObject root = new JsonObject();
        for (Setting<?> setting : settings) {
            JsonElement serialized = setting.serialize(gson);
            if (!serialized.isJsonNull()) {
                root.add(setting.getSettingKey(), serialized);
            }
        }
        return root;
    }

    @Override
    public Settings deserialize(JsonElement data, Gson gson) {
        if (!data.isJsonObject()) {
            return this;
        }
        JsonObject root = data.getAsJsonObject();
        for (Setting<?> setting : settings) {
            JsonElement element = root.get(setting.getSettingKey());
            if (element != null && !element.isJsonNull()) {
                setting.deserialize(element, gson);
            }
        }
        return this;
    }
}
