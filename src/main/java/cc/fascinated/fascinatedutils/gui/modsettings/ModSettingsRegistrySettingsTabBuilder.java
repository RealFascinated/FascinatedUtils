package cc.fascinated.fascinatedutils.gui.modsettings;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import cc.fascinated.fascinatedutils.common.setting.Setting;
import cc.fascinated.fascinatedutils.common.setting.SettingCategory;
import cc.fascinated.fascinatedutils.common.setting.SettingCategoryGrouper;
import cc.fascinated.fascinatedutils.common.setting.impl.BooleanSetting;
import cc.fascinated.fascinatedutils.common.setting.impl.ColorSetting;
import cc.fascinated.fascinatedutils.common.setting.impl.EnumSetting;
import cc.fascinated.fascinatedutils.common.setting.impl.KeybindSetting;
import cc.fascinated.fascinatedutils.common.setting.impl.SliderSetting;
import cc.fascinated.fascinatedutils.gui.core.Align;
import cc.fascinated.fascinatedutils.gui.core.Ref;
import cc.fascinated.fascinatedutils.gui.theme.ModSettingsTheme;
import cc.fascinated.fascinatedutils.gui.theme.SettingsUiMetrics;
import cc.fascinated.fascinatedutils.gui.themes.FascinatedGuiTheme;
import cc.fascinated.fascinatedutils.gui.widgets.FColumnWidget;
import cc.fascinated.fascinatedutils.gui.widgets.FLabelWidget;
import cc.fascinated.fascinatedutils.gui.widgets.FScrollColumnWidget;
import cc.fascinated.fascinatedutils.gui.widgets.FSpacerWidget;
import cc.fascinated.fascinatedutils.gui.widgets.FWidget;
import cc.fascinated.fascinatedutils.gui.widgets.settings.FBooleanSettingRowWidget;
import cc.fascinated.fascinatedutils.gui.widgets.settings.FColorSettingRowWidget;
import cc.fascinated.fascinatedutils.gui.widgets.settings.FEnumSettingRowWidget;
import cc.fascinated.fascinatedutils.gui.widgets.settings.FKeybindSettingWidget;
import cc.fascinated.fascinatedutils.gui.widgets.settings.FSliderSettingRowWidget;
import cc.fascinated.fascinatedutils.settings.SettingsRegistry;
import cc.fascinated.fascinatedutils.systems.config.ModConfig;
import lombok.experimental.UtilityClass;
import net.minecraft.network.chat.Component;

@UtilityClass
public class ModSettingsRegistrySettingsTabBuilder {

    private static final String PERFORMANCE_CATEGORY_DISPLAY_KEY = "Performance";

    public static FWidget buildSettingsTab(float paneWidth, float paneHeight, Ref<Float> scrollYRef, Consumer<ColorSetting> openColorPicker, RegistrySettingsSubTab subTab) {
        float settingsContentWidth = Math.max(28f, paneWidth);
        float settingsInnerWidth = Math.max(14f, settingsContentWidth - 2f * ModSettingsTheme.SIDEBAR_SEPARATOR_PAD_X);
        float gap = 3f;
        FColumnWidget scrollBody = new FColumnWidget(gap, Align.CENTER);
        scrollBody.addChild(new FSpacerWidget(settingsContentWidth, ModSettingsTheme.SIDEBAR_SEPARATOR_PAD_X));
        List<Setting<?>> allSettings = SettingsRegistry.INSTANCE.getSettings().getSettings();
        if (allSettings.isEmpty()) {
            FLabelWidget empty = new FLabelWidget();
            empty.setText(Component.translatable("fascinatedutils.setting.shell.settings_empty").getString());
            empty.setColorArgb(FascinatedGuiTheme.INSTANCE.textMuted());
            empty.setAlignX(Align.CENTER);
            scrollBody.addChild(empty);
            scrollBody.addChild(new FSpacerWidget(settingsContentWidth, 4f));
            return wrapScrollClip(scrollBody, gap, scrollYRef);
        }
        List<Setting<?>> topLevelSettings = new ArrayList<>();
        List<ModSettingsCategoryRows.CategoryBlock> categoryBlocks = new ArrayList<>();
        partitionRegistrySettingsForSubTab(allSettings, subTab, topLevelSettings, categoryBlocks);
        if (topLevelSettings.isEmpty() && categoryBlocks.isEmpty()) {
            FLabelWidget empty = new FLabelWidget();
            empty.setText(Component.translatable("fascinatedutils.setting.shell.settings_empty").getString());
            empty.setColorArgb(FascinatedGuiTheme.INSTANCE.textMuted());
            empty.setAlignX(Align.CENTER);
            scrollBody.addChild(empty);
            scrollBody.addChild(new FSpacerWidget(settingsContentWidth, 4f));
            return wrapScrollClip(scrollBody, gap, scrollYRef);
        }
        ModSettingsCategoryRows.appendTopLevelThenCategories(scrollBody, settingsContentWidth, settingsInnerWidth, categoryBlocks, topLevelSettings, (setting, innerWidth, sliderStartX) -> editorForRegistrySetting(setting, innerWidth, sliderStartX, openColorPicker), (booleanSetting, cellWidth, cellHeight) -> new FBooleanSettingGridCellWidget(booleanSetting, cellWidth, cellHeight, ModConfig::saveSettings));
        scrollBody.addChild(new FSpacerWidget(settingsContentWidth, ModSettingsCategoryRows.SETTINGS_SCROLL_BOTTOM_INSET));
        return wrapScrollClip(scrollBody, gap, scrollYRef);
    }

    private static void partitionRegistrySettingsForSubTab(List<Setting<?>> allSettings, RegistrySettingsSubTab subTab, List<Setting<?>> topLevelOut, List<ModSettingsCategoryRows.CategoryBlock> categoryBlocksOut) {
        List<Setting<?>> topLevelAll = SettingCategoryGrouper.topLevelInRegistrationOrder(allSettings);
        List<SettingCategory> categories = SettingCategoryGrouper.categoriesInRegistrationOrder(allSettings);
        if (subTab == RegistrySettingsSubTab.GENERAL) {
            topLevelOut.addAll(topLevelAll);
            for (SettingCategory category : categories) {
                if (!PERFORMANCE_CATEGORY_DISPLAY_KEY.equals(category.displayNameKey())) {
                    categoryBlocksOut.add(new ModSettingsCategoryRows.CategoryBlock(category.displayNameKey(), category.settings()));
                }
            }
            return;
        }
        for (SettingCategory category : categories) {
            if (PERFORMANCE_CATEGORY_DISPLAY_KEY.equals(category.displayNameKey())) {
                categoryBlocksOut.add(new ModSettingsCategoryRows.CategoryBlock(category.displayNameKey(), category.settings()));
            }
        }
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

    /**
     * Sub-panes for the registry Settings tab: general options versus performance-related options.
     */
    public enum RegistrySettingsSubTab {
        GENERAL, PERFORMANCE
    }
}
