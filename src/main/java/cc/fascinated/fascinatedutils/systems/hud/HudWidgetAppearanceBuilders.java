package cc.fascinated.fascinatedutils.systems.hud;

import cc.fascinated.fascinatedutils.common.color.SettingColor;
import cc.fascinated.fascinatedutils.common.setting.impl.BooleanSetting;
import cc.fascinated.fascinatedutils.common.setting.impl.ColorSetting;
import cc.fascinated.fascinatedutils.common.setting.impl.SliderSetting;

public class HudWidgetAppearanceBuilders {

    public static BooleanSetting.Builder showBackground() {
        return BooleanSetting.builder()
                .id(HudModule.SETTING_BACKGROUND)
                .defaultValue(true)
                .translationKeyPath("fascinatedutils.module.background")
                .categoryDisplayKey(HudModule.APPEARANCE_CATEGORY_DISPLAY_KEY);
    }

    public static ColorSetting.Builder backgroundColor() {
        return ColorSetting.builder()
                .id(HudModule.SETTING_BACKGROUND_COLOR)
                .defaultValue(new SettingColor(0, 0, 0, 85))
                .translationKeyPath("fascinatedutils.module.hud_background_color")
                .categoryDisplayKey(HudModule.APPEARANCE_CATEGORY_DISPLAY_KEY);
    }

    public static BooleanSetting.Builder roundedCorners() {
        return BooleanSetting.builder()
                .id(HudModule.SETTING_ROUNDED_CORNERS)
                .defaultValue(true)
                .translationKeyPath("fascinatedutils.module.rounded_corners")
                .categoryDisplayKey(HudModule.APPEARANCE_CATEGORY_DISPLAY_KEY);
    }

    public static SliderSetting.Builder roundingRadius() {
        return SliderSetting.builder()
                .id(HudModule.SETTING_ROUNDING_RADIUS)
                .defaultValue(6f)
                .minValue(1f)
                .maxValue(12f)
                .step(1f)
                .translationKeyPath("fascinatedutils.module.rounding_radius")
                .categoryDisplayKey(HudModule.APPEARANCE_CATEGORY_DISPLAY_KEY);
    }

    public static BooleanSetting.Builder showBorder() {
        return BooleanSetting.builder()
                .id(HudModule.SETTING_SHOW_BORDER)
                .defaultValue(false)
                .translationKeyPath("fascinatedutils.module.show_border")
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

    public static ColorSetting.Builder borderColor() {
        return ColorSetting.builder()
                .id(HudModule.SETTING_BORDER_COLOR)
                .defaultValue(new SettingColor(208, 215, 225, 192))
                .translationKeyPath("fascinatedutils.module.hud_border_color")
                .categoryDisplayKey(HudModule.APPEARANCE_CATEGORY_DISPLAY_KEY);
    }

    public static SliderSetting.Builder padding() {
        return SliderSetting.builder()
                .id(HudModule.SETTING_PADDING)
                .defaultValue(6f)
                .minValue(0f)
                .maxValue(16f)
                .step(1f)
                .translationKeyPath("fascinatedutils.module.hud_padding")
                .categoryDisplayKey(HudModule.APPEARANCE_CATEGORY_DISPLAY_KEY);
    }

}

