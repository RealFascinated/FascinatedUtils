package cc.fascinated.fascinatedutils.settings;

import cc.fascinated.fascinatedutils.client.keybind.KeybindsWrapper;
import cc.fascinated.fascinatedutils.common.ClientUtils;
import cc.fascinated.fascinatedutils.common.setting.Setting;
import cc.fascinated.fascinatedutils.common.setting.impl.BooleanSetting;
import cc.fascinated.fascinatedutils.common.setting.impl.KeybindSetting;
import cc.fascinated.fascinatedutils.gui2.screens.impl.SocialScreen;
import cc.fascinated.fascinatedutils.gui2.screens.impl.screenshot.ScreenshotScreen;
import cc.fascinated.fascinatedutils.systems.config.GsonSerializable;
import cc.fascinated.fascinatedutils.systems.hud.HUDManager;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.blaze3d.platform.InputConstants;
import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

@Getter
public class Settings implements GsonSerializable<Settings> {

    private final List<Setting<?>> settings = new ArrayList<>();

    private final KeybindSetting shellOpenKeybind = new KeybindSetting("settings_keybind", () -> KeybindsWrapper.registerCallbackKeybind(
            "key.alumite.settings_keybind",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_RIGHT_SHIFT,
            KeybindsWrapper.CATEGORY,
            () -> HUDManager.INSTANCE.setEditMode(!HUDManager.INSTANCE.isEditMode())
    ));
    private final KeybindSetting socialKeybind = new KeybindSetting("social_keybind", () -> KeybindsWrapper.registerCallbackKeybind(
            "key.alumite.social_keybind",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_BACKSLASH,
            KeybindsWrapper.CATEGORY,
            () -> {
                Minecraft minecraftClient = Minecraft.getInstance();
                Screen currentScreen = minecraftClient.screen;
                if (currentScreen instanceof SocialScreen) {
                    minecraftClient.setScreen(null);
                }
                else {
                    minecraftClient.setScreen(new SocialScreen());
                }
            }
    ));
    private final KeybindSetting screenshotsKeybind = new KeybindSetting("screenshots_keybind", () -> KeybindsWrapper.registerCallbackKeybind(
            "key.alumite.screenshots_keybind",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_U,
            KeybindsWrapper.CATEGORY,
            () -> Minecraft.getInstance().setScreen(new ScreenshotScreen())
    ));

    private final BooleanSetting showSelfNameplate = BooleanSetting.builder().id("show_self_nameplate")
            .defaultValue(true)
            .categoryDisplayKey("alumite.setting.category.general")
            .build();

    private final BooleanSetting showServerListInPauseMenu = BooleanSetting.builder().id("show_server_list_in_pause_menu")
            .defaultValue(true)
            .categoryDisplayKey("alumite.setting.category.general")
            .build();

    private final BooleanSetting confirmDisconnect = BooleanSetting.builder().id("confirm_disconnect")
            .defaultValue(false)
            .categoryDisplayKey("alumite.setting.category.general")
            .build();

    private final BooleanSetting reduceMacOSResolution = BooleanSetting.builder().id("reduce_mac_os_resolution")
            .defaultValue(false)
            .locked(() -> !ClientUtils.isMacOS())
            .lockedReason(() -> I18n.get("alumite.setting.lock_reason.requires_mac_os"))
            .categoryDisplayKey("alumite.setting.category.performance")
            .build();

    private final BooleanSetting turboEntities = BooleanSetting.builder().id("turbo_entities")
            .defaultValue(true)
            .categoryDisplayKey("alumite.setting.category.performance")
            .build();

    private final BooleanSetting turboParticles = BooleanSetting.builder().id("turbo_particles")
            .defaultValue(true)
            .categoryDisplayKey("alumite.setting.category.performance")
            .build();

    private final BooleanSetting socialNotifications = BooleanSetting.builder().id("social_notifications")
            .defaultValue(true)
            .categoryDisplayKey("alumite.setting.category.social")
            .build();
    private final BooleanSetting socialSounds = BooleanSetting.builder().id("social_sounds")
            .defaultValue(true)
            .categoryDisplayKey("alumite.setting.category.social")
            .build();

    public Settings() {
        addSetting(shellOpenKeybind);
        addSetting(socialKeybind);
        addSetting(screenshotsKeybind);
        addSetting(showSelfNameplate);
        addSetting(showServerListInPauseMenu);
        addSetting(confirmDisconnect);
        addSetting(reduceMacOSResolution);
        addSetting(turboEntities);
        addSetting(turboParticles);

        addSetting(socialNotifications);
        addSetting(socialSounds);

        // ensure it's only enabled on macOS
        if (!ClientUtils.isMacOS() && reduceMacOSResolution.isEnabled()) {
            reduceMacOSResolution.setValue(false);
        }
    }

    private void addSetting(Setting<?> setting) {
        setting.setTranslationKeyPrefix("alumite.setting");
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
