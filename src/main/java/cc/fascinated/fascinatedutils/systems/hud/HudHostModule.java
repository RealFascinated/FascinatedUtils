package cc.fascinated.fascinatedutils.systems.hud;

import cc.fascinated.fascinatedutils.common.setting.Setting;
import cc.fascinated.fascinatedutils.common.setting.SettingCategory;
import cc.fascinated.fascinatedutils.common.setting.SettingCategoryGrouper;
import cc.fascinated.fascinatedutils.common.setting.impl.BooleanSetting;
import cc.fascinated.fascinatedutils.common.setting.impl.ColorSetting;
import cc.fascinated.fascinatedutils.common.setting.impl.SliderSetting;
import cc.fascinated.fascinatedutils.gui.renderer.GuiRenderer;
import cc.fascinated.fascinatedutils.systems.modules.Module;
import cc.fascinated.fascinatedutils.systems.modules.ModuleCategory;
import cc.fascinated.fascinatedutils.systems.modules.ModuleDefaults;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public abstract class HudHostModule extends Module {
    public static final float UTILITY_WIDGET_MIN_WIDTH = 90f;

    public static final String SETTING_BACKGROUND = "background";
    public static final String SETTING_ROUNDED_CORNERS = "rounded_corners";
    public static final String SETTING_ROUNDING_RADIUS = "rounding_radius";
    public static final String SETTING_SHOW_BORDER = "show_border";
    public static final String SETTING_BORDER_THICKNESS = "border_thickness";
    public static final String SETTING_BACKGROUND_COLOR = "hud_background_color";
    public static final String SETTING_BORDER_COLOR = "hud_border_color";
    public static final String SETTING_PADDING = "hud_padding";
    public static final String SETTING_TEXT_SHADOW = "text_shadow";
    public static final String SETTING_REMOVE_MIN_WIDTH = "remove_min_width";
    public static final String HUD_PANELS_CATEGORY_DISPLAY_KEY = "fascinatedutils.setting.category.hud_panels";

    private final HudDefaults hudDefaults;
    private final SliderSetting padding;
    private final BooleanSetting textShadow;
    private final String settingsTranslationSuffix;
    private final List<HudPanel> registeredHudPanelsList = new ArrayList<>();
    private final Map<String, BooleanSetting> hudPanelVisibilitySettings = new LinkedHashMap<>();

    protected HudHostModule(String settingsTranslationSuffix, String displayName, HudDefaults defaults) {
        super(displayName, ModuleCategory.HUD, ModuleDefaults.builder().defaultState(defaults.defaultState()).build());
        this.settingsTranslationSuffix = settingsTranslationSuffix;
        this.hudDefaults = defaults;
        this.padding = HudWidgetAppearanceBuilders.padding().defaultValue(defaults.defaultPadding()).build();
        this.textShadow = HudWidgetAppearanceBuilders.textShadow().build();
        addSetting(padding);
        addSetting(textShadow);
        setEnabled(defaults.defaultState());
    }

    public List<HudPanel> registeredHudPanels() {
        return Collections.unmodifiableList(registeredHudPanelsList);
    }

    protected void registerHudPanel(HudPanel panel) {
        registeredHudPanelsList.add(panel);
        if (registeredHudPanelsList.size() >= 2) {
            reconcileHudPanelVisibilitySettings();
        }
    }

    private void reconcileHudPanelVisibilitySettings() {
        for (HudPanel hudPanel : registeredHudPanelsList) {
            String panelId = hudPanel.getId();
            if (hudPanelVisibilitySettings.containsKey(panelId)) {
                continue;
            }
            BooleanSetting visible = BooleanSetting.builder()
                    .id("hud_panel_visible_" + sanitizePanelSettingId(panelId))
                    .defaultValue(true)
                    .categoryDisplayKey(HUD_PANELS_CATEGORY_DISPLAY_KEY)
                    .build();
            addSetting(visible);
            hudPanelVisibilitySettings.put(panelId, visible);
        }
    }

    /**
     * Exposes persisted per-panel visibility when this host registers more than one {@link HudPanel}.
     *
     * @param panelId stable panel identifier
     * @return the toggle setting when multi-panel mode is active
     */
    public java.util.Optional<BooleanSetting> hudPanelVisibilityToggle(String panelId) {
        return java.util.Optional.ofNullable(hudPanelVisibilitySettings.get(panelId));
    }

    private static String sanitizePanelSettingId(String panelId) {
        return panelId.replace('.', '_').replace('-', '_');
    }

    public boolean isHudPanelUserVisible(String panelId) {
        BooleanSetting toggle = hudPanelVisibilitySettings.get(panelId);
        if (toggle == null) {
            return true;
        }
        return toggle.isEnabled();
    }

    public HudDefaults getHudDefaults() {
        return hudDefaults;
    }

    public float getPadding() {
        return padding.getValue().floatValue();
    }

    public boolean isTextShadowEnabled() {
        return textShadow.isEnabled();
    }

    public float getCornerRadius() {
        boolean rounded = getSetting(BooleanSetting.class, SETTING_ROUNDED_CORNERS).map(BooleanSetting::isEnabled).orElse(false);
        if (!rounded) {
            return 0f;
        }
        return getSetting(SliderSetting.class, SETTING_ROUNDING_RADIUS).map(s -> s.getValue().floatValue()).orElse(4f);
    }

    public void drawHUDPanelBackground(GuiRenderer glRenderer, float layoutWidth, float layoutHeight, boolean editorMode) {
        boolean showBg = getSetting(BooleanSetting.class, SETTING_BACKGROUND).map(BooleanSetting::isEnabled).orElse(false);
        boolean showBorder = getSetting(BooleanSetting.class, SETTING_SHOW_BORDER).map(BooleanSetting::isEnabled).orElse(false);
        float thickness = getSetting(SliderSetting.class, SETTING_BORDER_THICKNESS).map(s -> s.getValue().floatValue()).orElse(2f);
        float cornerRadius = getCornerRadius();
        int backgroundArgb = getSetting(ColorSetting.class, SETTING_BACKGROUND_COLOR).map(ColorSetting::getResolvedArgb).orElse(0x55000000);
        int borderArgb = getSetting(ColorSetting.class, SETTING_BORDER_COLOR).map(ColorSetting::getResolvedArgb).orElse(0xC0D0D7E1);
        HUDPanelBackground.drawPanelChrome(glRenderer, layoutWidth, layoutHeight, showBg, thickness, showBorder, cornerRadius, backgroundArgb, borderArgb, editorMode);
    }

    @Override
    protected String settingTranslationKeyPrefix(Setting<?> setting) {
        return "fascinatedutils.module." + settingsTranslationSuffix;
    }

    @Override
    public void resetToDefault() {
        super.resetToDefault();
        setEnabled(hudDefaults.defaultState());
        if (registeredHudPanelsList.size() <= 1) {
            hudPanelVisibilitySettings.clear();
        }
        for (HudPanel hudPanel : registeredHudPanelsList) {
            hudPanel.resetPanelLayoutDefaults();
        }
    }

    @Override
    public JsonElement serialize(Gson gson) {
        JsonObject root = super.serialize(gson).getAsJsonObject();
        JsonObject hudPanelsJson = new JsonObject();
        for (HudPanel hudPanel : registeredHudPanelsList) {
            hudPanelsJson.add(hudPanel.getId(), hudPanel.serializeHudState(gson));
        }
        root.add("hudPanels", hudPanelsJson);
        return root;
    }

    @Override
    public Module deserialize(JsonElement data, Gson gson) {
        super.deserialize(data, gson);
        if (!data.isJsonObject()) {
            return this;
        }
        JsonObject root = data.getAsJsonObject();
        deserializeHudLayouts(root, gson);
        return this;
    }

    void deserializeHudLayouts(JsonObject root, Gson gson) {
        JsonObject hudPanelsJson = root.has("hudPanels") && root.get("hudPanels").isJsonObject()
                ? root.get("hudPanels").getAsJsonObject()
                : null;
        if (hudPanelsJson != null && !registeredHudPanelsList.isEmpty()) {
            for (HudPanel hudPanel : registeredHudPanelsList) {
                String panelKey = hudPanel.getId();
                JsonElement panelElement = hudPanelsJson.get(panelKey);
                if (panelElement != null && panelElement.isJsonObject()) {
                    hudPanel.deserializeHudState(panelElement.getAsJsonObject(), gson);
                }
            }
        }
    }

    @Override
    public List<Setting<?>> getSettings() {
        return SettingCategoryGrouper.topLevelInRegistrationOrder(getAllSettings());
    }

    @Override
    public List<SettingCategory> getSettingCategories() {
        return SettingCategoryGrouper.categoriesInRegistrationOrder(getAllSettings());
    }
}
