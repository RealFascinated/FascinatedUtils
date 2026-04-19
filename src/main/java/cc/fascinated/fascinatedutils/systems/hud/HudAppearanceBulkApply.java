package cc.fascinated.fascinatedutils.systems.hud;

import cc.fascinated.fascinatedutils.common.setting.impl.BooleanSetting;
import cc.fascinated.fascinatedutils.common.setting.impl.ColorSetting;
import cc.fascinated.fascinatedutils.common.setting.impl.SliderSetting;
import cc.fascinated.fascinatedutils.systems.config.ModConfig;

import java.util.List;
import java.util.Optional;

public final class HudAppearanceBulkApply {

    private static final float SLIDER_CONSENSUS_EPSILON = 1e-4f;

    private HudAppearanceBulkApply() {
    }

    /**
     * Returns the shared boolean value when every HUD module that exposes the setting agrees; empty when none carry it
     * or values differ.
     *
     * @param hudWidgets        widgets to scan
     * @param booleanSettingId  persisted boolean setting key (for example {@code show_hud_background})
     * @return consensus value, or empty when mixed or absent
     */
    public static Optional<Boolean> booleanConsensus(List<HudModule> hudWidgets, String booleanSettingId) {
        Boolean consensus = null;
        int matchedWidgets = 0;
        for (HudModule widget : hudWidgets) {
            Optional<BooleanSetting> optionalSetting = widget.getSetting(BooleanSetting.class, booleanSettingId);
            if (optionalSetting.isEmpty()) {
                continue;
            }
            matchedWidgets++;
            boolean value = Boolean.TRUE.equals(optionalSetting.get().getValue());
            if (consensus == null) {
                consensus = value;
            }
            else if (consensus != value) {
                return Optional.empty();
            }
        }
        if (matchedWidgets == 0) {
            return Optional.empty();
        }
        return Optional.of(Boolean.TRUE.equals(consensus));
    }

    /**
     * Returns the shared slider value when every HUD module that exposes the setting agrees; empty when none carry it
     * or values differ.
     *
     * @param hudWidgets       widgets to scan
     * @param sliderSettingId  persisted slider setting key (for example {@code border_thickness})
     * @return consensus value, or empty when mixed or absent
     */
    public static Optional<Float> floatSliderConsensus(List<HudModule> hudWidgets, String sliderSettingId) {
        Float consensus = null;
        int matchedWidgets = 0;
        for (HudModule widget : hudWidgets) {
            Optional<SliderSetting> optionalSetting = widget.getSetting(SliderSetting.class, sliderSettingId);
            if (optionalSetting.isEmpty()) {
                continue;
            }
            matchedWidgets++;
            float value = optionalSetting.get().getValue().floatValue();
            if (consensus == null) {
                consensus = value;
            }
            else if (Math.abs(consensus - value) > SLIDER_CONSENSUS_EPSILON) {
                return Optional.empty();
            }
        }
        if (matchedWidgets == 0) {
            return Optional.empty();
        }
        return Optional.of(consensus);
    }

    /**
     * Writes the boolean to every HUD module that defines the setting, then persists all module settings.
     *
     * @param hudWidgets        widgets to update
     * @param booleanSettingId  persisted boolean setting key
     * @param newValue          value to assign
     */
    public static void applyBooleanToAllHudModules(List<HudModule> hudWidgets, String booleanSettingId, boolean newValue) {
        for (HudModule widget : hudWidgets) {
            widget.getSetting(BooleanSetting.class, booleanSettingId).ifPresent(setting -> setting.setValue(newValue));
        }
        HUDManager.INSTANCE.saveAll();
    }

    /**
     * Writes the slider value to every HUD module that defines the setting, then persists all module settings.
     *
     * @param hudWidgets       widgets to update
     * @param sliderSettingId  persisted slider setting key
     * @param newValue         snapped value to assign
     */
    public static void applySliderToAllHudModules(List<HudModule> hudWidgets, String sliderSettingId, float newValue) {
        for (HudModule widget : hudWidgets) {
            widget.getSetting(SliderSetting.class, sliderSettingId).ifPresent(setting -> setting.setValue(newValue));
        }
        HUDManager.INSTANCE.saveAll();
    }

    /**
     * Copies the staged color into a registry-backed {@link ColorSetting}, then persists global settings.
     *
     * @param registryColor  persisted color from the global settings registry
     * @param stagingColor   UI-only staging color whose value is copied
     */
    public static void applyRegistryColorFromStaging(ColorSetting registryColor, ColorSetting stagingColor) {
        registryColor.setValue(stagingColor.getValue().copy());
        ModConfig.saveSettings();
    }
}
