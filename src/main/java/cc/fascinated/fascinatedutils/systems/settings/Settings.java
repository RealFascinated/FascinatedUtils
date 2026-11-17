package cc.fascinated.fascinatedutils.systems.settings;

import cc.fascinated.fascinatedutils.client.keybind.KeybindsWrapper;
import cc.fascinated.fascinatedutils.common.util.ClientUtils;
import cc.fascinated.fascinatedutils.common.setting.Setting;
import cc.fascinated.fascinatedutils.common.setting.impl.BooleanSetting;
import cc.fascinated.fascinatedutils.common.setting.impl.EnumSetting;
import cc.fascinated.fascinatedutils.common.setting.impl.KeybindSetting;
import cc.fascinated.fascinatedutils.gui.screens.impl.screenshot.ScreenshotScreen;
import cc.fascinated.fascinatedutils.gui.screens.impl.settings.ModuleSort;
import cc.fascinated.fascinatedutils.systems.hud.HudManager;
import com.mojang.blaze3d.platform.InputConstants;
import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.language.I18n;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

@Getter
public class Settings {
    public static final Settings INSTANCE = new Settings();

    private static final String CATEGORY_GENERAL = "alumite.setting.category.general";
    private static final String CATEGORY_PERFORMANCE = "alumite.setting.category.performance";
    private static final String CATEGORY_TURBO_RENDERING = "alumite.setting.category.turbo_rendering";

    private final List<Setting<?>> settings = new ArrayList<>();

    private final KeybindSetting shellOpenKeybind = new KeybindSetting("settings_keybind", () -> KeybindsWrapper.registerCallbackKeybind(
            "key.alumite.settings_keybind",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_RIGHT_SHIFT,
            KeybindsWrapper.CATEGORY,
            () -> HudManager.INSTANCE.setEditMode(!HudManager.INSTANCE.isEditMode())
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
            .categoryDisplayKey(CATEGORY_GENERAL)
            .build();

    private final BooleanSetting showServerListInPauseMenu = BooleanSetting.builder().id("show_server_list_in_pause_menu")
            .defaultValue(true)
            .categoryDisplayKey(CATEGORY_GENERAL)
            .build();

    private final BooleanSetting confirmDisconnect = BooleanSetting.builder().id("confirm_disconnect")
            .defaultValue(false)
            .categoryDisplayKey(CATEGORY_GENERAL)
            .build();

    private final BooleanSetting seamlessPanels = BooleanSetting.builder().id("seamless_panels")
            .defaultValue(true)
            .categoryDisplayKey(CATEGORY_GENERAL)
            .build();

    private final BooleanSetting reduceMacOSResolution = BooleanSetting.builder().id("reduce_mac_os_resolution")
            .defaultValue(false)
            .locked(() -> !ClientUtils.isMacOS())
            .lockedReason(() -> I18n.get("alumite.setting.lock_reason.requires_mac_os"))
            .categoryDisplayKey(CATEGORY_PERFORMANCE)
            .build();

    private final BooleanSetting turboEntities = BooleanSetting.builder().id("turbo_entities")
            .defaultValue(true)
            .categoryDisplayKey(CATEGORY_TURBO_RENDERING)
            .build();

    private final BooleanSetting turboParticles = BooleanSetting.builder().id("turbo_particles")
            .defaultValue(true)
            .categoryDisplayKey(CATEGORY_TURBO_RENDERING)
            .build();

    private final BooleanSetting turboWeather = BooleanSetting.builder().id("turbo_weather")
            .defaultValue(true)
            .categoryDisplayKey(CATEGORY_TURBO_RENDERING)
            .build();

    private final EnumSetting<ModuleSort> moduleSort = EnumSetting.<ModuleSort>builder().id("module_sort")
            .defaultValue(ModuleSort.DEFAULT)
            .valueFormatter(ModuleSort::displayName)
            .build();

    public Settings() {
        addSetting(shellOpenKeybind);
        addSetting(screenshotsKeybind);
        addSetting(showSelfNameplate);
        addSetting(showServerListInPauseMenu);
        addSetting(confirmDisconnect);
        addSetting(seamlessPanels);
        addSetting(reduceMacOSResolution);
        addSetting(turboEntities);
        addSetting(turboParticles);
        addSetting(turboWeather);
        addSetting(moduleSort);

        if (!ClientUtils.isMacOS() && reduceMacOSResolution.isEnabled()) {
            reduceMacOSResolution.setValue(false);
        }
    }

    private void addSetting(Setting<?> setting) {
        setting.setTranslationKeyPrefix("alumite.setting");
        this.settings.add(setting);
    }
}
