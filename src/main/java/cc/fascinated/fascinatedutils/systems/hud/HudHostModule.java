package cc.fascinated.fascinatedutils.systems.hud;

import cc.fascinated.fascinatedutils.common.setting.Setting;
import cc.fascinated.fascinatedutils.common.setting.SettingCategory;
import cc.fascinated.fascinatedutils.common.setting.SettingCategoryGrouper;
import cc.fascinated.fascinatedutils.common.setting.impl.BooleanSetting;
import cc.fascinated.fascinatedutils.common.setting.impl.ColorSetting;
import cc.fascinated.fascinatedutils.common.setting.impl.SliderSetting;
import cc.fascinated.fascinatedutils.gui.render.GuiRenderer;
import cc.fascinated.fascinatedutils.gui.render.RectCornerRoundMask;
import cc.fascinated.fascinatedutils.systems.modules.Module;
import cc.fascinated.fascinatedutils.systems.modules.ModuleCategory;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import lombok.Getter;
import lombok.Setter;

import java.util.*;

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
    public static final String HUD_PANELS_CATEGORY_DISPLAY_KEY = "alumite.setting.category.hud_panels";

    @Getter
    private final HudDefaults hudDefaults;
    /**
     * Corners drawn square for the panel currently being drawn; set by {@link HUDManager} around each draw.
     */
    @Setter
    private int squaredCornerMask;
    private final String settingsTranslationSuffix;
    private final List<HudPanel> registeredHudPanelsList = new ArrayList<>();
    private final Map<String, BooleanSetting> hudPanelVisibilitySettings = new LinkedHashMap<>();
    private BooleanSetting showBackground;
    private BooleanSetting roundedCorners;
    private SliderSetting roundingRadius;
    private BooleanSetting showBorder;
    private SliderSetting borderThickness;
    private ColorSetting backgroundColor;
    private ColorSetting borderColor;
    private SliderSetting padding;
    private BooleanSetting textShadow;
    private BooleanSetting removeMinimumWidth;

    protected HudHostModule(String settingsTranslationSuffix, String displayName, HudDefaults defaults) {
        super(displayName, ModuleCategory.HUD, defaults.defaultState());
        this.settingsTranslationSuffix = settingsTranslationSuffix;
        this.hudDefaults = defaults;
        setEnabled(defaults.defaultState());
    }

    /**
     * Default state of the background toggle; hosts that start without a background override this.
     *
     * @return the default background toggle state
     */
    protected boolean appearanceDefaultShowBackground() {
        return true;
    }

    /**
     * Default padding in pixels; hosts that sit flush against their content override this.
     *
     * @return the default padding value
     */
    protected float appearanceDefaultPadding() {
        return 5f;
    }

    /**
     * Registers the shared appearance settings. Call once from the host constructor, at the point where the block belongs in the settings list.
     *
     * @param includeRemoveMinimumWidth whether this host exposes the remove-minimum-width toggle
     */
    protected final void addAppearanceSettings(boolean includeRemoveMinimumWidth) {
        showBackground = HudWidgetAppearanceBuilders.showBackground().defaultValue(appearanceDefaultShowBackground()).build();
        roundedCorners = HudWidgetAppearanceBuilders.roundedCorners().build();
        roundingRadius = HudWidgetAppearanceBuilders.roundingRadius().build();
        showBorder = HudWidgetAppearanceBuilders.showBorder().build();
        borderThickness = HudWidgetAppearanceBuilders.borderThickness().build();
        backgroundColor = HudWidgetAppearanceBuilders.backgroundColor().build();
        borderColor = HudWidgetAppearanceBuilders.borderColor().build();
        padding = HudWidgetAppearanceBuilders.padding().defaultValue(appearanceDefaultPadding()).build();
        textShadow = HudWidgetAppearanceBuilders.textShadow().build();
        addSetting(showBackground);
        addSetting(roundedCorners);
        addSetting(showBorder);
        addSetting(roundingRadius);
        addSetting(borderThickness);
        addSetting(backgroundColor);
        addSetting(borderColor);
        showBackground.addSubSetting(backgroundColor);
        roundedCorners.addSubSetting(roundingRadius);
        showBorder.addSubSetting(borderThickness);
        showBorder.addSubSetting(borderColor);
        if (includeRemoveMinimumWidth) {
            removeMinimumWidth = HudWidgetAppearanceBuilders.removeMinimumWidth().build();
            addSetting(removeMinimumWidth);
        }
        addSetting(padding);
        addSetting(textShadow);
    }

    private static String sanitizePanelSettingId(String panelId) {
        return panelId.replace('.', '_').replace('-', '_');
    }

    public List<HudPanel> registeredHudPanels() {
        return Collections.unmodifiableList(registeredHudPanelsList);
    }

    /**
     * Counts the {@link HudPanel}s this host has registered, without wrapping the list.
     *
     * @return the number of registered panels
     */
    public int registeredHudPanelCount() {
        return registeredHudPanelsList.size();
    }

    /**
     * Whether the remove-minimum-width toggle is registered and enabled, which lets panels drop their base minimum width.
     *
     * @return {@code true} when the minimum width should be ignored
     */
    public boolean isRemoveMinimumWidthEnabled() {
        return removeMinimumWidth != null && removeMinimumWidth.isEnabled();
    }

    protected void registerHudPanel(HudPanel panel) {
        registeredHudPanelsList.add(panel);
        if (registeredHudPanelsList.size() >= 2 || hudDefaults.alwaysShowPanelToggles()) {
            reconcileHudPanelVisibilitySettings();
        }
    }

    private void reconcileHudPanelVisibilitySettings() {
        for (HudPanel hudPanel : registeredHudPanelsList) {
            String panelId = hudPanel.getId();
            if (hudPanelVisibilitySettings.containsKey(panelId)) {
                continue;
            }
            BooleanSetting visible = BooleanSetting.builder().id("hud_panel_visible_" + sanitizePanelSettingId(panelId)).defaultValue(hudPanel.defaultVisible()).categoryDisplayKey(HUD_PANELS_CATEGORY_DISPLAY_KEY).build();
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
    public Optional<BooleanSetting> hudPanelVisibilityToggle(String panelId) {
        return Optional.ofNullable(hudPanelVisibilitySettings.get(panelId));
    }

    public boolean isHudPanelUserVisible(String panelId) {
        BooleanSetting toggle = hudPanelVisibilitySettings.get(panelId);
        if (toggle == null) {
            return true;
        }
        return toggle.isEnabled();
    }

    public float getPaddingValue() {
        return padding.getValue().floatValue();
    }

    public boolean isTextShadowEnabled() {
        return textShadow.isEnabled();
    }

    public float getCornerRadius() {
        if (!roundedCorners.isEnabled()) {
            return 0f;
        }
        return roundingRadius.getValue().floatValue();
    }

    public void drawHUDPanelBackground(GuiRenderer glRenderer, float layoutWidth, float layoutHeight, boolean editorMode) {
        float cornerRadius = getCornerRadius();
        HUDPanelBackground.drawPanelChrome(glRenderer, layoutWidth, layoutHeight, showBackground.isEnabled(), borderThickness.getValue().floatValue(), showBorder.isEnabled(), cornerRadius, RectCornerRoundMask.ALL & ~squaredCornerMask, backgroundColor.getResolvedArgb(), borderColor.getResolvedArgb(), editorMode);
    }

    /**
     * Whether this module paints a background or border behind its HUD panels.
     *
     * @return true when a background or border is enabled
     */
    public boolean drawsPanelChrome() {
        return showBackground.isEnabled() || showBorder.isEnabled();
    }

    @Override
    protected String settingTranslationKeyPrefix(Setting<?> setting) {
        return "alumite.module." + settingsTranslationSuffix;
    }

    @Override
    public void resetToDefault() {
        super.resetToDefault();
        setEnabled(hudDefaults.defaultState());
        if (registeredHudPanelsList.size() <= 1 && !hudDefaults.alwaysShowPanelToggles()) {
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
        JsonObject hudPanelsJson = root.has("hudPanels") && root.get("hudPanels").isJsonObject() ? root.get("hudPanels").getAsJsonObject() : null;
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
        return SettingCategoryGrouper.categoriesInRegistrationOrder(getAllSettings()).stream()
                .filter(category -> !HUD_PANELS_CATEGORY_DISPLAY_KEY.equals(category.displayNameKey()))
                .toList();
    }
}
