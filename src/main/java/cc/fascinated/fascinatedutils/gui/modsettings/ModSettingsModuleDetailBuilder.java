package cc.fascinated.fascinatedutils.gui.modsettings;

import cc.fascinated.fascinatedutils.common.setting.Setting;
import cc.fascinated.fascinatedutils.common.setting.SettingCategory;
import cc.fascinated.fascinatedutils.common.setting.impl.BooleanSetting;
import cc.fascinated.fascinatedutils.common.setting.impl.EnumSetting;
import cc.fascinated.fascinatedutils.common.setting.impl.KeybindSetting;
import cc.fascinated.fascinatedutils.common.setting.impl.SliderSetting;
import cc.fascinated.fascinatedutils.gui.GuiDesignSpace;
import cc.fascinated.fascinatedutils.gui.core.Align;
import cc.fascinated.fascinatedutils.gui.core.Ref;
import cc.fascinated.fascinatedutils.gui.theme.ModSettingsTheme;
import cc.fascinated.fascinatedutils.gui.theme.SettingsUiMetrics;
import cc.fascinated.fascinatedutils.gui.themes.fascinated.FascinatedGuiTheme;
import cc.fascinated.fascinatedutils.gui.widgets.*;
import cc.fascinated.fascinatedutils.gui.widgets.settings.*;
import cc.fascinated.fascinatedutils.systems.config.ModConfig;
import cc.fascinated.fascinatedutils.systems.modules.Module;
import lombok.experimental.UtilityClass;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

@UtilityClass
public class ModSettingsModuleDetailBuilder {

    public static FWidget buildModuleSettingsDetail(float paneWidth, float paneHeight, Module module, Runnable onBack, Ref<Float> settingsPaneScrollYRef) {
        float settingsContentWidth = Math.max(GuiDesignSpace.pxX(28f), paneWidth);
        float settingsInnerWidth = Math.max(GuiDesignSpace.pxX(14f), settingsContentWidth - 2f * GuiDesignSpace.pxX(ModSettingsTheme.SIDEBAR_SEPARATOR_PAD_X));
        float gap = GuiDesignSpace.pxY(3f);
        FColumnWidget scrollBody = new FColumnWidget(gap, Align.START);
        scrollBody.addChild(new FSpacerWidget(settingsContentWidth, GuiDesignSpace.pxY(4f)));
        scrollBody.addChild(FModSettingsDetailHeaderCardWidget.centeredInContentRow(settingsContentWidth, settingsInnerWidth, onBack, module.getDisplayName()));
        scrollBody.addChild(new FSpacerWidget(settingsContentWidth, GuiDesignSpace.pxY(3f)));
        if (module.getAllSettings().isEmpty()) {
            FLabelWidget empty = new FLabelWidget();
            empty.setText(Component.translatable("fascinatedutils.setting.shell.no_settings").getString());
            empty.setColorArgb(FascinatedGuiTheme.INSTANCE.textMuted());
            empty.setAlignX(Align.START);
            scrollBody.addChild(ModSettingsCategoryRows.wrapSettingsDetailRowInShellMargin(settingsContentWidth, settingsInnerWidth, new FMinWidthHostWidget(ModSettingsCategoryRows.settingsDetailPaddedInnerWidth(settingsInnerWidth), empty)));
            scrollBody.addChild(new FSpacerWidget(settingsContentWidth, GuiDesignSpace.pxY(4f)));
            return wrapScrollClip(scrollBody, gap, settingsPaneScrollYRef);
        }
        List<ModSettingsCategoryRows.CategoryBlock> categoryBlocks = new ArrayList<>();
        for (SettingCategory category : module.getSettingCategories()) {
            categoryBlocks.add(new ModSettingsCategoryRows.CategoryBlock(category.displayNameKey(), List.copyOf(category.settings())));
        }
        ModSettingsCategoryRows.appendTopLevelThenCategories(scrollBody, settingsContentWidth, settingsInnerWidth, categoryBlocks, module.getSettings(), (setting, innerWidth, sliderValueColumnStartX) -> editorForModuleSetting(module, setting, innerWidth, sliderValueColumnStartX), (booleanSetting, cellWidth, cellHeight) -> new FBooleanSettingGridCellWidget(booleanSetting, cellWidth, cellHeight, () -> ModConfig.saveActiveModule(module)));
        scrollBody.addChild(new FSpacerWidget(settingsContentWidth, GuiDesignSpace.pxY(ModSettingsCategoryRows.SETTINGS_SCROLL_BOTTOM_INSET)));
        return wrapScrollClip(scrollBody, gap, settingsPaneScrollYRef);
    }

    private static FWidget wrapScrollClip(FColumnWidget body, float gap, Ref<Float> settingsPaneScrollYRef) {
        FScrollColumnWidget clip = new FScrollColumnWidget(body, gap);
        clip.setFillVerticalInColumn(true);
        if (settingsPaneScrollYRef != null) {
            Float scrollOffsetY = settingsPaneScrollYRef.getValue();
            clip.setScrollOffsetY(scrollOffsetY == null ? 0f : scrollOffsetY);
            clip.setScrollOffsetChangeListener(settingsPaneScrollYRef::setValue);
        }
        return clip;
    }

    private static FWidget editorForModuleSetting(Module module, Setting<?> setting, float settingsInnerWidth, float sliderValueColumnStartX) {
        float bodyWidth = settingsInnerWidth;
        if (setting instanceof BooleanSetting booleanSetting) {
            float editorHeight = SettingsUiMetrics.booleanOuterHeight();
            return new FBooleanSettingRowWidget(module, booleanSetting, bodyWidth, editorHeight, sliderValueColumnStartX);
        }
        if (setting instanceof SliderSetting sliderSetting) {
            float editorHeight = SettingsUiMetrics.floatOuterHeight();
            return new FSliderSettingRowWidget(module, sliderSetting, bodyWidth, editorHeight, sliderValueColumnStartX);
        }
        if (setting instanceof KeybindSetting keybindSetting) {
            float editorHeight = SettingsUiMetrics.booleanOuterHeight();
            return new FKeybindSettingWidget(keybindSetting, bodyWidth, editorHeight, () -> ModConfig.saveActiveModule(module));
        }
        if (setting instanceof EnumSetting<?> enumSetting) {
            float editorHeight = SettingsUiMetrics.booleanOuterHeight();
            return new FEnumSettingRowWidget(module, enumSetting, bodyWidth, editorHeight, sliderValueColumnStartX);
        }
        return null;
    }
}
