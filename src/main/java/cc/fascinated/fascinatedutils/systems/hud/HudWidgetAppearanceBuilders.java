package cc.fascinated.fascinatedutils.systems.hud;

import cc.fascinated.fascinatedutils.common.color.SettingColor;
import cc.fascinated.fascinatedutils.common.setting.Setting;
import cc.fascinated.fascinatedutils.common.setting.impl.BooleanSetting;
import cc.fascinated.fascinatedutils.common.setting.impl.ColorSetting;
import cc.fascinated.fascinatedutils.common.setting.impl.SliderSetting;
import cc.fascinated.fascinatedutils.systems.modules.Module;
import net.minecraft.client.resources.language.I18n;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

public class HudWidgetAppearanceBuilders {

    private static Supplier<String> displayName(String key) {
        return () -> I18n.get(key + ".display_name");
    }

    private static Supplier<@Nullable String> tooltip(String key) {
        return () -> {
            String desc = key + ".description";
            return I18n.exists(desc) ? I18n.get(desc) : null;
        };
    }

    private static <T, B extends Setting.Builder<T, B>> B appearance(B builder, String id, String translationKey) {
        return builder.id(id).displayName(displayName(translationKey)).tooltip(tooltip(translationKey)).categoryDisplayKey(Module.APPEARANCE_CATEGORY_DISPLAY_KEY);
    }

    public static BooleanSetting.Builder showBackground() {
        return appearance(BooleanSetting.builder(), HudHostModule.SETTING_BACKGROUND, "alumite.module.background");
    }

    public static ColorSetting.Builder backgroundColor() {
        return appearance(ColorSetting.builder(), HudHostModule.SETTING_BACKGROUND_COLOR, "alumite.module.hud_background_color").defaultValue(new SettingColor(0, 0, 0, 85));
    }

    public static BooleanSetting.Builder roundedCorners() {
        return appearance(BooleanSetting.builder(), HudHostModule.SETTING_ROUNDED_CORNERS, "alumite.module.rounded_corners").defaultValue(true);
    }

    public static SliderSetting.Builder roundingRadius() {
        return appearance(SliderSetting.builder(), HudHostModule.SETTING_ROUNDING_RADIUS, "alumite.module.rounding_radius").defaultValue(6f).minValue(1f).maxValue(12f).step(1f);
    }

    public static BooleanSetting.Builder showBorder() {
        return appearance(BooleanSetting.builder(), HudHostModule.SETTING_SHOW_BORDER, "alumite.module.show_border").defaultValue(false);
    }

    public static SliderSetting.Builder borderThickness() {
        return appearance(SliderSetting.builder(), HudHostModule.SETTING_BORDER_THICKNESS, "alumite.module.border_thickness").defaultValue(2f).minValue(1f).maxValue(3f).step(1f);
    }

    public static ColorSetting.Builder borderColor() {
        return appearance(ColorSetting.builder(), HudHostModule.SETTING_BORDER_COLOR, "alumite.module.hud_border_color").defaultValue(new SettingColor(208, 215, 225, 192));
    }

    public static SliderSetting.Builder padding() {
        return appearance(SliderSetting.builder(), HudHostModule.SETTING_PADDING, "alumite.module.hud_padding").minValue(0f).maxValue(16f).step(1f);
    }

    public static BooleanSetting.Builder textShadow() {
        return appearance(BooleanSetting.builder(), HudHostModule.SETTING_TEXT_SHADOW, "alumite.module.text_shadow").defaultValue(true);
    }

    public static BooleanSetting.Builder removeMinimumWidth() {
        return appearance(BooleanSetting.builder(), HudHostModule.SETTING_REMOVE_MIN_WIDTH, "alumite.module.remove_min_width").defaultValue(false);
    }

}
