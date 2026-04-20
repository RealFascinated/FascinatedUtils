package cc.fascinated.fascinatedutils.systems.hud;

import cc.fascinated.fascinatedutils.common.setting.impl.BooleanSetting;
import cc.fascinated.fascinatedutils.common.setting.impl.SliderSetting;

public class HudWidgetAppearanceBuilders {

    public static BooleanSetting.Builder showBackground() {
        return BooleanSetting.builder()
                .id(HudModule.SETTING_SHOW_BACKGROUND)
                .defaultValue(true)
                .translationKeyPath("fascinatedutils.module.show_hud_background")
                .categoryDisplayKey(HudModule.APPEARANCE_CATEGORY_DISPLAY_KEY);
    }

    public static BooleanSetting.Builder roundedCorners() {
        return BooleanSetting.builder()
                .id(HudModule.SETTING_ROUNDED_CORNERS)
                .defaultValue(false)
                .translationKeyPath("fascinatedutils.module.rounded_corners")
                .categoryDisplayKey(HudModule.APPEARANCE_CATEGORY_DISPLAY_KEY);
    }

    public static BooleanSetting.Builder showBorder() {
        return BooleanSetting.builder()
                .id(HudModule.SETTING_SHOW_BORDER)
                .defaultValue(false)
                .translationKeyPath("fascinatedutils.module.show_border")
                .categoryDisplayKey(HudModule.APPEARANCE_CATEGORY_DISPLAY_KEY);
    }

    public static SliderSetting.Builder roundingRadius() {
        return SliderSetting.builder()
                .id(HudModule.SETTING_ROUNDING_RADIUS)
                .defaultValue(4f)
                .minValue(1f)
                .maxValue(16f)
                .step(1f)
                .translationKeyPath("fascinatedutils.module.rounding_radius")
                .categoryDisplayKey(HudModule.APPEARANCE_CATEGORY_DISPLAY_KEY);
    }

    public static SliderSetting.Builder borderThickness() {
        return SliderSetting.builder()
                .id(HudModule.SETTING_BORDER_THICKNESS)
                .defaultValue(2f)
                .minValue(1f)
                .maxValue(3f)
                .step(1f)
                .translationKeyPath("fascinatedutils.module.border_thickness")
                .categoryDisplayKey(HudModule.APPEARANCE_CATEGORY_DISPLAY_KEY);
    }

    public static SliderSetting.Builder padding() {
        return SliderSetting.builder()
                .id(HudModule.SETTING_PADDING)
                .defaultValue(6f)
                .minValue(0f)
                .maxValue(16f)
                .step(1f)
                .translationKeyPath("fascinatedutils.module.padding")
                .categoryDisplayKey(HudModule.APPEARANCE_CATEGORY_DISPLAY_KEY);
    }
}
