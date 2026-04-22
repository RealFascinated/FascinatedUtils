package cc.fascinated.fascinatedutils.systems.modules;

import cc.fascinated.fascinatedutils.common.setting.Setting;
import cc.fascinated.fascinatedutils.common.setting.SettingCategory;
import cc.fascinated.fascinatedutils.common.setting.SettingCategoryGrouper;
import cc.fascinated.fascinatedutils.common.setting.impl.KeybindSetting;
import cc.fascinated.fascinatedutils.event.FascinatedEventBus;
import cc.fascinated.fascinatedutils.event.impl.module.ModuleEnabledStateChangedEvent;
import cc.fascinated.fascinatedutils.systems.config.ConfigVersion;
import cc.fascinated.fascinatedutils.systems.config.GsonSerializable;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Getter
public abstract class Module implements GsonSerializable<Module> {
    private final String moduleKey;
    private final String displayName;
    private final List<Setting<?>> settings = new ArrayList<>();
    private final ModuleCategory category;

    private final ModuleDefaults defaults;

    // settings
    private boolean enabled;

    protected Module(String displayName, ModuleCategory category, ModuleDefaults defaults) {
        this.moduleKey = idFromModuleClass(getClass());
        this.displayName = displayName;
        this.category = category;
        this.defaults = defaults;
    }

    protected Module(String displayName, ModuleCategory category) {
        this(displayName, category, ModuleDefaults.builder().build());
    }

    /**
     * Returns the config version of this module.
     * Reads the {@link ConfigVersion} annotation on the concrete class; defaults to {@code 1} if absent.
     *
     * @return the module config version
     */
    public int getVersion() {
        ConfigVersion annotation = getClass().getAnnotation(ConfigVersion.class);
        return annotation != null ? annotation.value() : 1;
    }

    /**
     * Derives a stable module key from the concrete module class.
     *
     * @param moduleClass the module implementation class
     * @return a lowercase key suitable for persistence
     */
    public static String idFromModuleClass(Class<?> moduleClass) {
        String simple = moduleClass.getSimpleName();
        if (simple.endsWith("Module") && simple.length() > "Module".length()) {
            simple = simple.substring(0, simple.length() - "Module".length());
        }
        return simple.toLowerCase(Locale.ROOT);
    }

    /**
     * Registers a setting for this module. Use {@link Setting#getCategoryDisplayKey()} on the setting for a section
     * header in the module settings UI; omit or blank the key for top-level rows before categorized sections.
     *
     * @param setting the setting definition to append
     */
    public void addSetting(Setting<?> setting) {
        applyTranslationPrefix(setting);
        this.settings.add(setting);
    }

    private void applyTranslationPrefix(Setting<?> setting) {
        setting.setTranslationKeyPrefix(settingTranslationKeyPrefix(setting));
        for (Setting<?> sub : setting.getSubSettings()) {
            applyTranslationPrefix(sub);
        }
    }

    /**
     * Lists every setting on this module in registration order (suitable for persistence).
     *
     * @return an unmodifiable view of all settings
     */
    public List<Setting<?>> getAllSettings() {
        return List.copyOf(settings);
    }

    /**
     * Top-level settings: those with no {@link Setting#getCategoryDisplayKey()} (or a blank key), in registration order.
     *
     * @return an unmodifiable list of those settings
     */
    public List<Setting<?>> getSettings() {
        return SettingCategoryGrouper.topLevelInRegistrationOrder(settings);
    }

    /**
     * Setting categories derived from non-blank {@link Setting#getCategoryDisplayKey()} on each setting.
     *
     * @return an unmodifiable list of categories
     */
    public List<SettingCategory> getSettingCategories() {
        return SettingCategoryGrouper.categoriesInRegistrationOrder(settings);
    }

    /**
     * Looks up a setting by runtime type and setting key.
     *
     * @param settingType expected concrete setting class
     * @param settingId   persisted setting key
     * @param <S>         setting implementation type
     * @return the matching setting, or empty when not found
     */
    @SuppressWarnings("unchecked")
    public <S extends Setting<?>> Optional<S> getSetting(Class<S> settingType, String settingId) {
        for (Setting<?> setting : settings) {
            if (settingType.isInstance(setting) && settingId.equals(setting.getSettingKey())) {
                return Optional.of((S) setting);
            }
        }
        return Optional.empty();
    }

    public void setEnabled(boolean enabled) {
        if (this.enabled == enabled) {
            return;
        }
        this.enabled = enabled;
        if (ModuleRegistry.INSTANCE.isInitialized()) {
            FascinatedEventBus.INSTANCE.post(new ModuleEnabledStateChangedEvent(this, enabled));
        }
    }

    public void resetToDefault() {
        setEnabled(defaults.defaultState());
        for (Setting<?> setting : settings) {
            if (setting instanceof KeybindSetting) {
                continue;
            }
            setting.resetToDefault();
        }
    }

    @Override
    public JsonElement serialize(Gson gson) {
        JsonObject root = new JsonObject();
        root.addProperty("enabled", enabled);
        JsonObject settingsJson = new JsonObject();
        for (Setting<?> setting : settings) {
            if (setting instanceof KeybindSetting) {
                continue;
            }
            JsonElement serialized = setting.serialize(gson);
            if (!serialized.isJsonNull()) {
                settingsJson.add(setting.getSettingKey(), serialized);
            }
        }
        root.add("settings", settingsJson);
        return root;
    }

    @Override
    public Module deserialize(JsonElement data, Gson gson) {
        if (!data.isJsonObject()) {
            return this;
        }
        JsonObject root = data.getAsJsonObject();
        if (root.has("enabled")) {
            JsonElement enabledEl = root.get("enabled");
            if (enabledEl.isJsonPrimitive()) {
                setEnabled(enabledEl.getAsBoolean());
            }
        }
        JsonObject settingsJson = root.has("settings") && root.get("settings").isJsonObject() ? root.get("settings").getAsJsonObject() : new JsonObject();
        for (Setting<?> setting : settings) {
            JsonElement value = settingsJson.get(setting.getSettingKey());
            if (value != null && !value.isJsonNull()) {
                setting.deserialize(value, gson);
            }
        }
        return this;
    }

    protected String settingTranslationKeyPrefix(Setting<?> setting) {
        return "fascinatedutils.module." + moduleKey;
    }
}
