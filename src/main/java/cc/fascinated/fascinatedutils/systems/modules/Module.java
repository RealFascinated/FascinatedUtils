package cc.fascinated.fascinatedutils.systems.modules;

import cc.fascinated.fascinatedutils.common.setting.Setting;
import cc.fascinated.fascinatedutils.common.setting.SettingCategory;
import cc.fascinated.fascinatedutils.common.setting.SettingCategoryGrouper;
import cc.fascinated.fascinatedutils.event.FascinatedEventBus;
import cc.fascinated.fascinatedutils.event.impl.module.ModuleEnabledStateChangedEvent;
import lombok.Getter;

import java.util.*;

@Getter
public abstract class Module {
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
        setting.setTranslationKeyPrefix(settingTranslationKeyPrefix(setting));
        this.settings.add(setting);
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
        setEnabled(false);
        for (Setting<?> setting : settings) {
            setting.resetToDefault();
        }
    }

    protected String settingTranslationKeyPrefix(Setting<?> setting) {
        return "fascinatedutils.module." + moduleKey;
    }
}
