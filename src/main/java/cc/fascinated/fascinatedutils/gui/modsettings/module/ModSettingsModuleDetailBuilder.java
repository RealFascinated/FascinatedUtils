package cc.fascinated.fascinatedutils.gui.modsettings.module;

import cc.fascinated.fascinatedutils.common.setting.Setting;
import cc.fascinated.fascinatedutils.common.setting.SettingCategory;
import cc.fascinated.fascinatedutils.common.setting.impl.*;
import cc.fascinated.fascinatedutils.gui.core.Align;
import cc.fascinated.fascinatedutils.gui.core.FState;
import cc.fascinated.fascinatedutils.gui.modsettings.FModSettingsDetailHeaderCardWidget;
import cc.fascinated.fascinatedutils.gui.modsettings.ModSettingsCategoryRows;
import cc.fascinated.fascinatedutils.gui.theme.ModSettingsTheme;
import cc.fascinated.fascinatedutils.gui.theme.SettingsUiMetrics;
import cc.fascinated.fascinatedutils.gui.themes.FascinatedGuiTheme;
import cc.fascinated.fascinatedutils.gui.widgets.*;
import cc.fascinated.fascinatedutils.gui.widgets.settings.*;
import cc.fascinated.fascinatedutils.systems.config.ModConfig;
import cc.fascinated.fascinatedutils.systems.hud.HudHostModule;
import cc.fascinated.fascinatedutils.systems.hud.HudPanel;
import cc.fascinated.fascinatedutils.systems.modules.Module;
import lombok.experimental.UtilityClass;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

@UtilityClass
public class ModSettingsModuleDetailBuilder {

    public static FWidget moduleDetailViewportUi(float paneWidth, float paneHeight, Module module, Runnable onBack, FState<Float> settingsPaneScrollYRef, FState<String> settingsSearchRef, Runnable onSearchChanged, Consumer<ColorSetting> openColorPicker, FOutlinedTextInputWidget sharedSearchField) {
        float settingsContentWidth = Math.max(28f, paneWidth);
        float settingsInnerWidth = Math.max(14f, settingsContentWidth - 2f * ModSettingsTheme.SIDEBAR_SEPARATOR_PAD_X);

        String storedSearchText = settingsSearchRef.get();
        sharedSearchField.adoptExternalValueWithoutCallbacks(storedSearchText == null ? "" : storedSearchText);
        sharedSearchField.setOnChange(text -> {
            settingsSearchRef.setQuiet(text);
            onSearchChanged.run();
        });

        FWidget detailHeaderPacked = packDetailHeader(settingsContentWidth, settingsInnerWidth, module, sharedSearchField, onBack, onSearchChanged);

        float bodyColumnGap = 3f;
        FColumnWidget scrollBody = new FColumnWidget(bodyColumnGap, Align.START);

        String searchLower = (settingsSearchRef.get() == null ? "" : settingsSearchRef.get()).toLowerCase(Locale.ROOT);
        boolean isFiltering = !searchLower.isBlank();

        if (!isFiltering && module instanceof HudHostModule hudHost && hudHost.registeredHudPanels().size() > 1) {
            float paddedInnerHost = ModSettingsCategoryRows.settingsDetailPaddedInnerWidth(settingsInnerWidth);
            scrollBody.addChild(new FSpacerWidget(settingsContentWidth, 2f));
            for (HudPanel hudPanel : hudHost.registeredHudPanels()) {
                hudHost.hudPanelVisibilityToggle(hudPanel.getId()).ifPresent(panelToggle -> {
                    float toggleHeight = SettingsUiMetrics.booleanOuterHeight();
                    FBooleanSettingRowWidget rowWidget = new FBooleanSettingRowWidget(hudHost, panelToggle, settingsInnerWidth, toggleHeight, paddedInnerHost);
                    FWidget hudRowWrapped = ModSettingsCategoryRows.wrapSettingsDetailRowInShellMargin(settingsContentWidth, settingsInnerWidth, new FMinWidthHostWidget(paddedInnerHost, rowWidget));
                    scrollBody.addChild(hudRowWrapped);
                });
            }
            scrollBody.addChild(new FSpacerWidget(settingsContentWidth, 3f));
        }

        if (module.getAllSettings().isEmpty()) {
            FLabelWidget empty = new FLabelWidget();
            empty.setText(Component.translatable("alumite.setting.shell.no_settings").getString());
            empty.setColorArgb(FascinatedGuiTheme.INSTANCE.textMuted());
            empty.setAlignX(Align.START);
            scrollBody.addChild(ModSettingsCategoryRows.wrapSettingsDetailRowInShellMargin(settingsContentWidth, settingsInnerWidth, new FMinWidthHostWidget(ModSettingsCategoryRows.settingsDetailPaddedInnerWidth(settingsInnerWidth), empty)));
            scrollBody.addChild(new FSpacerWidget(settingsContentWidth, 4f));
        } else {
            List<ModSettingsCategoryRows.CategoryBlock> categoryBlocks = new ArrayList<>();
            for (SettingCategory category : module.getSettingCategories()) {
                List<Setting<?>> filtered = isFiltering ? category.settings().stream().filter(setting -> setting.getName().toLowerCase(Locale.ROOT).contains(searchLower)).toList() : List.copyOf(category.settings());
                categoryBlocks.add(new ModSettingsCategoryRows.CategoryBlock(category.displayNameKey(), filtered));
            }
            List<Setting<?>> filteredTopLevel = isFiltering ? module.getSettings().stream().filter(setting -> setting.getName().toLowerCase(Locale.ROOT).contains(searchLower)).toList() : module.getSettings();
            boolean anyVisible = !filteredTopLevel.isEmpty() || categoryBlocks.stream().anyMatch(block -> !block.settings().isEmpty());
            if (!anyVisible) {
                FLabelWidget empty = new FLabelWidget();
                empty.setText(Component.translatable("alumite.setting.shell.empty_modules").getString());
                empty.setColorArgb(FascinatedGuiTheme.INSTANCE.textMuted());
                empty.setAlignX(Align.START);
                scrollBody.addChild(ModSettingsCategoryRows.wrapSettingsDetailRowInShellMargin(settingsContentWidth, settingsInnerWidth, new FMinWidthHostWidget(ModSettingsCategoryRows.settingsDetailPaddedInnerWidth(settingsInnerWidth), empty)));
                scrollBody.addChild(new FSpacerWidget(settingsContentWidth, 4f));
            } else {
                ModSettingsCategoryRows.appendTopLevelThenCategories(scrollBody, settingsContentWidth, settingsInnerWidth, categoryBlocks, filteredTopLevel, (setting, innerWidth, sliderValueColumnStartX) -> editorForModuleSetting(module, setting, innerWidth, sliderValueColumnStartX, openColorPicker));
                scrollBody.addChild(new FSpacerWidget(settingsContentWidth, ModSettingsCategoryRows.SETTINGS_SCROLL_BOTTOM_INSET));
            }
        }

        FScrollColumnWidget scrollViewport = FTheme.components().createScrollColumn(scrollBody, bodyColumnGap);
        scrollViewport.setFillVerticalInColumn(true);
        Float scrollOffset = settingsPaneScrollYRef.get();
        scrollViewport.setScrollOffsetY(scrollOffset == null ? 0f : scrollOffset);
        scrollViewport.setScrollOffsetChangeListener(settingsPaneScrollYRef::setQuiet);

        float sectionGap = ModSettingsTheme.SIDEBAR_SEPARATOR_PAD_X;
        FColumnWidget outerColumn = new FColumnWidget(0f, Align.START) {
            @Override
            public boolean fillsHorizontalInRow() {
                return true;
            }

            @Override
            public boolean fillsVerticalInColumn() {
                return true;
            }
        };
        detailHeaderPacked.setCellConstraints(new FCellConstraints().setExpandHorizontal(true));
        outerColumn.addChild(detailHeaderPacked);
        outerColumn.addChild(new FSpacerWidget(settingsContentWidth, sectionGap));
        scrollViewport.setCellConstraints(new FCellConstraints().setExpandHorizontal(true).setExpandVertical(true));
        outerColumn.addChild(scrollViewport);
        return outerColumn;
    }

    private static FWidget packDetailHeader(float settingsContentWidth, float settingsInnerWidth, Module module, FOutlinedTextInputWidget searchInput, Runnable onBack, Runnable onSearchChanged) {
        FColumnWidget headerColumn = new FColumnWidget(0f, Align.START);
        headerColumn.addChild(FModSettingsDetailHeaderCardWidget.centeredWithSearchAndResetInContentRow(settingsContentWidth, settingsInnerWidth, onBack, module.getDisplayName(), searchInput, () -> {
            module.resetToDefault();
            ModConfig.profiles().saveModule(module);
            onSearchChanged.run();
        }));
        headerColumn.addChild(new FSpacerWidget(settingsContentWidth, 3f));
        return headerColumn;
    }

    /**
     * Search field sizing for module detail pane; callers should construct one instance per mods tab session while a module is pinned.
     */
    public static FOutlinedTextInputWidget createSharedModuleDetailSearchField() {
        return new FOutlinedTextInputWidget(180, SettingsUiMetrics.SHELL_CONTROL_HEIGHT_DESIGN, () -> Component.translatable("alumite.setting.shell.search_settings").getString());
    }

    private static FWidget editorForModuleSetting(Module module, Setting<?> setting, float settingsInnerWidth, float sliderValueColumnStartX, Consumer<ColorSetting> openColorPicker) {
        if (setting instanceof BooleanSetting booleanSetting && booleanSetting.hasSubSettings()) {
            float editorHeight = SettingsUiMetrics.booleanOuterHeight();
            return new FBooleanWithSubSettingsWidget(booleanSetting, () -> ModConfig.profiles().saveModule(module), settingsInnerWidth, editorHeight, sliderValueColumnStartX, (sub, subW, subColX) -> editorForModuleSetting(module, sub, subW, subColX, openColorPicker));
        }
        if (setting instanceof BooleanSetting booleanSetting) {
            float editorHeight = SettingsUiMetrics.booleanOuterHeight();
            return new FBooleanSettingRowWidget(module, booleanSetting, settingsInnerWidth, editorHeight, sliderValueColumnStartX);
        }
        if (setting instanceof SliderSetting sliderSetting) {
            float editorHeight = SettingsUiMetrics.floatOuterHeight();
            return new FSliderSettingRowWidget(module, sliderSetting, settingsInnerWidth, editorHeight, sliderValueColumnStartX);
        }
        if (setting instanceof KeybindSetting keybindSetting) {
            float editorHeight = SettingsUiMetrics.booleanOuterHeight();
            return new FKeybindSettingWidget(keybindSetting, settingsInnerWidth, editorHeight, () -> ModConfig.profiles().saveModule(module));
        }
        if (setting instanceof EnumSetting<?> enumSetting) {
            float editorHeight = SettingsUiMetrics.booleanOuterHeight();
            return new FEnumSettingRowWidget(module, enumSetting, settingsInnerWidth, editorHeight, sliderValueColumnStartX);
        }
        if (setting instanceof ColorSetting colorSetting) {
            float editorHeight = SettingsUiMetrics.booleanOuterHeight();
            return new FColorSettingRowWidget(colorSetting, settingsInnerWidth, editorHeight, () -> ModConfig.profiles().saveModule(module), sliderValueColumnStartX, openColorPicker);
        }
        return null;
    }
}
