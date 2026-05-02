package cc.fascinated.fascinatedutils.gui.modsettings;

import cc.fascinated.fascinatedutils.common.setting.Setting;
import cc.fascinated.fascinatedutils.common.setting.SettingCategory;
import cc.fascinated.fascinatedutils.common.setting.impl.*;
import cc.fascinated.fascinatedutils.gui.core.Align;
import cc.fascinated.fascinatedutils.gui.core.GuiFocusState;
import cc.fascinated.fascinatedutils.gui.core.Ref;
import cc.fascinated.fascinatedutils.gui.declare.Ui;
import cc.fascinated.fascinatedutils.gui.declare.UiSlot;
import cc.fascinated.fascinatedutils.gui.declare.UiView;
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

    private static final int MODULE_SETTINGS_SEARCH_FOCUS_ID = 5002;

    public static UiView moduleDetailViewportUi(float paneWidth, float paneHeight, Module module, Runnable onBack, Ref<Float> settingsPaneScrollYRef,
                                                Ref<String> settingsSearchRef, Runnable onSearchChanged, Consumer<ColorSetting> openColorPicker,
                                                FOutlinedTextInputWidget sharedSearchField) {
        float settingsContentWidth = Math.max(28f, paneWidth);
        float settingsInnerWidth = Math.max(14f, settingsContentWidth - 2f * ModSettingsTheme.SIDEBAR_SEPARATOR_PAD_X);

        String storedSearchText = settingsSearchRef.getValue();
        sharedSearchField.adoptExternalValueWithoutCallbacks(storedSearchText == null ? "" : storedSearchText);
        sharedSearchField.setOnChange(text -> {
            settingsSearchRef.setValue(text);
            onSearchChanged.run();
        });
        sharedSearchField.setExternalFocusIdSupplier(GuiFocusState::getFocusedId);

        FWidget detailHeaderPacked = packDetailHeader(settingsContentWidth, settingsInnerWidth, module, sharedSearchField, onBack, onSearchChanged);

        float bodyColumnGap = 3f;
        float clipRowGap = bodyColumnGap;
        List<UiSlot> scrollSlots = new ArrayList<>();

        String searchLower = (settingsSearchRef.getValue() == null ? "" : settingsSearchRef.getValue()).toLowerCase(Locale.ROOT);
        boolean isFiltering = !searchLower.isBlank();

        if (!isFiltering && module instanceof HudHostModule hudHost && hudHost.registeredHudPanels().size() > 1) {
            float paddedInnerHost = ModSettingsCategoryRows.settingsDetailPaddedInnerWidth(settingsInnerWidth);
            scrollSlots.add(UiSlot.of(Ui.spacer(settingsContentWidth, 2f)));
            for (HudPanel hudPanel : hudHost.registeredHudPanels()) {
                hudHost.hudPanelVisibilityToggle(hudPanel.getId()).ifPresent(panelToggle -> {
                    float toggleHeight = SettingsUiMetrics.booleanOuterHeight();
                    FBooleanSettingRowWidget rowWidget = new FBooleanSettingRowWidget(hudHost, panelToggle, settingsInnerWidth, toggleHeight, paddedInnerHost);
                    FWidget hudRowWrapped = ModSettingsCategoryRows.wrapSettingsDetailRowInShellMargin(settingsContentWidth, settingsInnerWidth,
                            new FMinWidthHostWidget(paddedInnerHost, rowWidget));
                    scrollSlots.add(Ui.slot(Ui.widgetSlot("hudVis:" + hudPanel.getId(), hudRowWrapped)));
                });
            }
            scrollSlots.add(UiSlot.of(Ui.spacer(settingsContentWidth, 3f)));
        }

        if (module.getAllSettings().isEmpty()) {
            FLabelWidget empty = new FLabelWidget();
            empty.setText(Component.translatable("fascinatedutils.setting.shell.no_settings").getString());
            empty.setColorArgb(FascinatedGuiTheme.INSTANCE.textMuted());
            empty.setAlignX(Align.START);
            scrollSlots.add(Ui.slot(Ui.widgetSlot("detailEmptyNoSettings",
                    ModSettingsCategoryRows.wrapSettingsDetailRowInShellMargin(settingsContentWidth, settingsInnerWidth,
                            new FMinWidthHostWidget(ModSettingsCategoryRows.settingsDetailPaddedInnerWidth(settingsInnerWidth), empty)))));
            scrollSlots.add(UiSlot.of(Ui.spacer(settingsContentWidth, 4f)));
        }
        else {
            List<ModSettingsCategoryRows.CategoryBlock> categoryBlocks = new ArrayList<>();
            for (SettingCategory category : module.getSettingCategories()) {
                List<Setting<?>> filtered = isFiltering
                        ? category.settings().stream().filter(setting -> setting.getName().toLowerCase(Locale.ROOT).contains(searchLower)).toList()
                        : List.copyOf(category.settings());
                categoryBlocks.add(new ModSettingsCategoryRows.CategoryBlock(category.displayNameKey(), filtered));
            }
            List<Setting<?>> filteredTopLevel = isFiltering
                    ? module.getSettings().stream().filter(setting -> setting.getName().toLowerCase(Locale.ROOT).contains(searchLower)).toList()
                    : module.getSettings();
            boolean anyVisible = !filteredTopLevel.isEmpty() || categoryBlocks.stream().anyMatch(block -> !block.settings().isEmpty());
            if (!anyVisible) {
                FLabelWidget empty = new FLabelWidget();
                empty.setText(Component.translatable("fascinatedutils.setting.shell.empty_modules").getString());
                empty.setColorArgb(FascinatedGuiTheme.INSTANCE.textMuted());
                empty.setAlignX(Align.START);
                scrollSlots.add(Ui.slot(Ui.widgetSlot("detailEmptyFiltered",
                        ModSettingsCategoryRows.wrapSettingsDetailRowInShellMargin(settingsContentWidth, settingsInnerWidth,
                                new FMinWidthHostWidget(ModSettingsCategoryRows.settingsDetailPaddedInnerWidth(settingsInnerWidth), empty)))));
                scrollSlots.add(UiSlot.of(Ui.spacer(settingsContentWidth, 4f)));
            }
            else {
                scrollSlots.addAll(ModSettingsCategoryRows.declarativeSlotsTopLevelThenCategories(settingsContentWidth, settingsInnerWidth,
                        categoryBlocks, filteredTopLevel, (setting, innerWidth, sliderValueColumnStartX) -> editorForModuleSetting(module, setting,
                                innerWidth, sliderValueColumnStartX, openColorPicker)));
                scrollSlots.add(UiSlot.of(Ui.spacer(settingsContentWidth, ModSettingsCategoryRows.SETTINGS_SCROLL_BOTTOM_INSET)));
            }
        }

        UiView scrollViewport = Ui.scrollTracked(clipRowGap, bodyColumnGap, true, settingsPaneScrollYRef::setValue,
                settingsPaneScrollYRef, scrollSlots);

        float sectionGap = ModSettingsTheme.SIDEBAR_SEPARATOR_PAD_X;
        return Ui.column(0f, Align.START, List.of(
                Ui.slot(new FCellConstraints().setExpandHorizontal(true), Ui.widgetSlot("detailHeaderPacked", detailHeaderPacked)),
                Ui.slot(Ui.spacer(settingsContentWidth, sectionGap)),
                Ui.slot(new FCellConstraints().setExpandHorizontal(true).setExpandVertical(true), scrollViewport)));
    }

    private static FWidget packDetailHeader(float settingsContentWidth, float settingsInnerWidth, Module module, FOutlinedTextInputWidget searchInput,
                                           Runnable onBack, Runnable onSearchChanged) {
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
        return new FOutlinedTextInputWidget(MODULE_SETTINGS_SEARCH_FOCUS_ID, 180, SettingsUiMetrics.SHELL_CONTROL_HEIGHT_DESIGN,
                () -> Component.translatable("fascinatedutils.setting.shell.search_settings").getString());
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
