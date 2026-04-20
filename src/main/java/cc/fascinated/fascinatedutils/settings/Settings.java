package cc.fascinated.fascinatedutils.settings;

import cc.fascinated.fascinatedutils.client.keybind.Keybinds;
import cc.fascinated.fascinatedutils.common.ClientUtils;
import cc.fascinated.fascinatedutils.common.setting.Setting;
import cc.fascinated.fascinatedutils.common.setting.impl.BooleanSetting;
import cc.fascinated.fascinatedutils.common.setting.impl.KeybindSetting;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

@Getter
public class Settings {

    private final List<Setting<?>> settings = new ArrayList<>();

    private final KeybindSetting shellOpenKeybind = new KeybindSetting("shell_open_keybind", Keybinds::openMenuKeybind);

    private final BooleanSetting showSelfNameplate = BooleanSetting.builder().id("show_self_nameplate").defaultValue(false).build();

    private final BooleanSetting showServerListInPauseMenu = BooleanSetting.builder().id("show_server_list_in_pause_menu").defaultValue(false).build();

    private final BooleanSetting confirmDisconnect = BooleanSetting.builder().id("confirm_disconnect").defaultValue(false).build();

    private final BooleanSetting reduceMacOSResolution = BooleanSetting.builder().id("reduce_mac_os_resolution").defaultValue(false).locked(() -> !ClientUtils.isMacOS()).lockedReason(() -> "This Setting Requires MacOS").categoryDisplayKey("Performance").build();

    private final BooleanSetting turboEntities = BooleanSetting.builder().id("turbo_entities").defaultValue(false).categoryDisplayKey("Performance").build();

    private final BooleanSetting turboParticles = BooleanSetting.builder().id("turbo_particles").defaultValue(false).categoryDisplayKey("Performance").build();

    public Settings() {
        addSetting(shellOpenKeybind);
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
}
