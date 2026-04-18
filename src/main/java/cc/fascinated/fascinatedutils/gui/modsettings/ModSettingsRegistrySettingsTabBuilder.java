package cc.fascinated.fascinatedutils.gui.modsettings;

import cc.fascinated.fascinatedutils.common.setting.Setting;
import cc.fascinated.fascinatedutils.common.setting.SettingCategory;
import cc.fascinated.fascinatedutils.common.setting.SettingCategoryGrouper;
import cc.fascinated.fascinatedutils.common.setting.impl.*;
import cc.fascinated.fascinatedutils.gui.GuiDesignSpace;
import cc.fascinated.fascinatedutils.gui.core.Align;
import cc.fascinated.fascinatedutils.gui.core.Ref;
import cc.fascinated.fascinatedutils.gui.theme.ModSettingsTheme;
import cc.fascinated.fascinatedutils.gui.theme.SettingsUiMetrics;
import cc.fascinated.fascinatedutils.gui.theme.UITheme;
import cc.fascinated.fascinatedutils.gui.themes.fascinated.FascinatedGuiTheme;
import cc.fascinated.fascinatedutils.gui.widgets.*;
import cc.fascinated.fascinatedutils.gui.widgets.settings.*;
import cc.fascinated.fascinatedutils.settings.SettingsRegistry;
import cc.fascinated.fascinatedutils.systems.config.ModConfig;
import lombok.experimental.UtilityClass;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

@UtilityClass
public class ModSettingsRegistrySettingsTabBuilder {

    private static final float SETTINGS_SCROLL_BOTTOM_INSET = UITheme.PADDING_XS;

    public static FWidget buildSettingsTab(float paneWidth, float paneHeight, Ref<Float> scrollYRef, Consumer<ColorSetting> openColorPicker) {
        float settingsContentWidth = Math.max(GuiDesignSpace.pxX(40f), paneWidth);
        float settingsInnerWidth = Math.max(GuiDesignSpace.pxX(20f), settingsContentWidth - 2f * GuiDesignSpace.pxX(ModSettingsTheme.SIDEBAR_SEPARATOR_PAD_X));
        float gap = GuiDesignSpace.pxY(UITheme.GAP_SM);
        FColumnWidget scrollBody = new FColumnWidget(gap, Align.CENTER);
        scrollBody.addChild(new FSpacerWidget(settingsContentWidth, GuiDesignSpace.pxY(ModSettingsTheme.SIDEBAR_SEPARATOR_PAD_X)));
        List<Setting<?>> allSettings = SettingsRegistry.INSTANCE.getSettings().getSettings();
        if (allSettings.isEmpty()) {
            FLabelWidget empty = new FLabelWidget();
            empty.setText(Component.translatable("fascinatedutils.setting.shell.settings_empty").getString());
            empty.setColorArgb(FascinatedGuiTheme.INSTANCE.textMuted());
            empty.setAlignX(Align.CENTER);
            scrollBody.addChild(empty);
            scrollBody.addChild(new FSpacerWidget(settingsContentWidth, GuiDesignSpace.pxY(UITheme.PADDING_SM)));
            return wrapScrollClip(scrollBody, gap, scrollYRef);
        }
        List<ModSettingsCategoryRows.CategoryBlock> categoryBlocks = new ArrayList<>();
        for (SettingCategory category : SettingCategoryGrouper.categoriesInRegistrationOrder(allSettings)) {
            categoryBlocks.add(new ModSettingsCategoryRows.CategoryBlock(category.displayNameKey(), category.settings()));
        }
        ModSettingsCategoryRows.appendTopLevelThenCategories(scrollBody, settingsContentWidth, settingsInnerWidth, categoryBlocks, SettingCategoryGrouper.topLevelInRegistrationOrder(allSettings), (setting, innerWidth, sliderStartX) -> editorForRegistrySetting(setting, innerWidth, sliderStartX, openColorPicker));
        scrollBody.addChild(new FSpacerWidget(settingsContentWidth, GuiDesignSpace.pxY(SETTINGS_SCROLL_BOTTOM_INSET)));
        return wrapScrollClip(scrollBody, gap, scrollYRef);
    }

    private static FWidget editorForRegistrySetting(Setting<?> setting, float settingsInnerWidth, float sliderValueColumnStartX, Consumer<ColorSetting> openColorPicker) {
        if (setting instanceof KeybindSetting keybindSetting) {
            float editorHeight = SettingsUiMetrics.booleanOuterHeight();
            return new FKeybindSettingWidget(keybindSetting, settingsInnerWidth, editorHeight, ModConfig::saveSettings);
        }
        if (setting instanceof BooleanSetting booleanSetting) {
            float editorHeight = SettingsUiMetrics.booleanOuterHeight();
            return new FBooleanSettingRowWidget(booleanSetting, settingsInnerWidth, editorHeight, ModConfig::saveSettings, sliderValueColumnStartX);
        }
        if (setting instanceof SliderSetting sliderSetting) {
            float editorHeight = SettingsUiMetrics.floatOuterHeight();
            return new FSliderSettingRowWidget(sliderSetting, settingsInnerWidth, editorHeight, ModConfig::saveSettings, sliderValueColumnStartX);
        }
        if (setting instanceof EnumSetting<?> enumSetting) {
            float editorHeight = SettingsUiMetrics.booleanOuterHeight();
            return new FEnumSettingRowWidget(enumSetting, settingsInnerWidth, editorHeight, ModConfig::saveSettings, sliderValueColumnStartX);
        }
        if (setting instanceof ColorSetting colorSetting) {
            float editorHeight = SettingsUiMetrics.booleanOuterHeight();
            return new FColorSettingRowWidget(colorSetting, settingsInnerWidth, editorHeight, ModConfig::saveSettings, sliderValueColumnStartX, openColorPicker);
        }
        return null;
    }

    private static FWidget wrapScrollClip(FColumnWidget body, float gap, Ref<Float> scrollYRef) {
        FScrollColumnWidget clip = new FScrollColumnWidget(body, gap);
        clip.setFillVerticalInColumn(true);
        if (scrollYRef != null) {
            Float scrollOffsetY = scrollYRef.getValue();
            clip.setScrollOffsetY(scrollOffsetY == null ? 0f : scrollOffsetY);
            clip.setScrollOffsetChangeListener(scrollYRef::setValue);
        }
        return clip;
    }
}
