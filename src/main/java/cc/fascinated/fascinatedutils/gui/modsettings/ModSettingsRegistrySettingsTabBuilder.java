package cc.fascinated.fascinatedutils.gui.modsettings;

import cc.fascinated.fascinatedutils.common.setting.Setting;
import cc.fascinated.fascinatedutils.common.setting.SettingCategoryGrouper;
import cc.fascinated.fascinatedutils.common.setting.impl.BooleanSetting;
import cc.fascinated.fascinatedutils.common.setting.impl.EnumSetting;
import cc.fascinated.fascinatedutils.common.setting.impl.KeybindSetting;
import cc.fascinated.fascinatedutils.common.setting.impl.SliderSetting;
import cc.fascinated.fascinatedutils.gui.core.Align;
import cc.fascinated.fascinatedutils.gui.core.FState;
import cc.fascinated.fascinatedutils.gui.theme.ModSettingsTheme;
import cc.fascinated.fascinatedutils.gui.theme.SettingsUiMetrics;
import cc.fascinated.fascinatedutils.gui.themes.FascinatedGuiTheme;
import cc.fascinated.fascinatedutils.gui.widgets.*;
import cc.fascinated.fascinatedutils.gui.widgets.settings.FBooleanSettingRowWidget;
import cc.fascinated.fascinatedutils.gui.widgets.settings.FEnumSettingRowWidget;
import cc.fascinated.fascinatedutils.gui.widgets.settings.FKeybindSettingWidget;
import cc.fascinated.fascinatedutils.gui.widgets.settings.FSliderSettingRowWidget;
import cc.fascinated.fascinatedutils.settings.SettingsRegistry;
import cc.fascinated.fascinatedutils.systems.config.ModConfig;
import lombok.experimental.UtilityClass;
import net.minecraft.network.chat.Component;

import java.util.List;

@UtilityClass
public class ModSettingsRegistrySettingsTabBuilder {

    public static FWidget buildSettingsTab(float paneWidth, float paneHeight, FState<Float> scrollYRef) {
        float settingsContentWidth = Math.max(28f, paneWidth);
        float settingsInnerWidth = Math.max(14f, settingsContentWidth - 2f * ModSettingsTheme.SIDEBAR_SEPARATOR_PAD_X);
        float gap = 3f;
        FColumnWidget scrollBody = new FColumnWidget(gap, Align.CENTER);
        scrollBody.addChild(new FSpacerWidget(settingsContentWidth, ModSettingsTheme.SIDEBAR_SEPARATOR_PAD_X));
        List<Setting<?>> allSettings = SettingsRegistry.INSTANCE.getSettings().getSettings();
        if (allSettings.isEmpty()) {
            FLabelWidget empty = new FLabelWidget();
            empty.setText(Component.translatable("alumite.setting.shell.settings_empty").getString());
            empty.setColorArgb(FascinatedGuiTheme.INSTANCE.textMuted());
            empty.setAlignX(Align.CENTER);
            scrollBody.addChild(empty);
            scrollBody.addChild(new FSpacerWidget(settingsContentWidth, 4f));
            return wrapScrollClip(scrollBody, gap, scrollYRef);
        }
        List<Setting<?>> topLevelSettings = SettingCategoryGrouper.topLevelInRegistrationOrder(allSettings);
        List<ModSettingsCategoryRows.CategoryBlock> categoryBlocks = SettingCategoryGrouper.categoriesInRegistrationOrder(allSettings).stream()
                .map(category -> new ModSettingsCategoryRows.CategoryBlock(category.displayNameKey(), category.settings()))
                .toList();
        if (topLevelSettings.isEmpty() && categoryBlocks.isEmpty()) {
            FLabelWidget empty = new FLabelWidget();
            empty.setText(Component.translatable("alumite.setting.shell.settings_empty").getString());
            empty.setColorArgb(FascinatedGuiTheme.INSTANCE.textMuted());
            empty.setAlignX(Align.CENTER);
            scrollBody.addChild(empty);
            scrollBody.addChild(new FSpacerWidget(settingsContentWidth, 4f));
            return wrapScrollClip(scrollBody, gap, scrollYRef);
        }
        ModSettingsCategoryRows.appendTopLevelThenCategories(scrollBody, settingsContentWidth, settingsInnerWidth, categoryBlocks, topLevelSettings, (setting, innerWidth, sliderStartX) -> editorForRegistrySetting(setting, innerWidth, sliderStartX));
        scrollBody.addChild(new FSpacerWidget(settingsContentWidth, ModSettingsCategoryRows.SETTINGS_SCROLL_BOTTOM_INSET));
        return wrapScrollClip(scrollBody, gap, scrollYRef);
    }

    private static FWidget editorForRegistrySetting(Setting<?> setting, float settingsInnerWidth, float sliderValueColumnStartX) {
        Runnable saveSettings = () -> ModConfig.config().save();
        if (setting instanceof KeybindSetting keybindSetting) {
            float editorHeight = SettingsUiMetrics.booleanOuterHeight();
            return new FKeybindSettingWidget(keybindSetting, settingsInnerWidth, editorHeight, saveSettings);
        }
        if (setting instanceof BooleanSetting booleanSetting) {
            float editorHeight = SettingsUiMetrics.booleanOuterHeight();
            return new FBooleanSettingRowWidget(booleanSetting, settingsInnerWidth, editorHeight, saveSettings, sliderValueColumnStartX);
        }
        if (setting instanceof SliderSetting sliderSetting) {
            float editorHeight = SettingsUiMetrics.floatOuterHeight();
            return new FSliderSettingRowWidget(sliderSetting, settingsInnerWidth, editorHeight, saveSettings, sliderValueColumnStartX);
        }
        if (setting instanceof EnumSetting<?> enumSetting) {
            float editorHeight = SettingsUiMetrics.booleanOuterHeight();
            return new FEnumSettingRowWidget(enumSetting, settingsInnerWidth, editorHeight, saveSettings, sliderValueColumnStartX);
        }
        return null;
    }

    private static FWidget wrapScrollClip(FColumnWidget body, float gap, FState<Float> scrollYRef) {
        FScrollColumnWidget clip = new FScrollColumnWidget(body, gap);
        clip.setFillVerticalInColumn(true);
        if (scrollYRef != null) {
            Float scrollOffsetY = scrollYRef.get();
            clip.setScrollOffsetY(scrollOffsetY == null ? 0f : scrollOffsetY);
            clip.setScrollOffsetChangeListener(scrollYRef::setQuiet);
        }
        return clip;
    }
}
