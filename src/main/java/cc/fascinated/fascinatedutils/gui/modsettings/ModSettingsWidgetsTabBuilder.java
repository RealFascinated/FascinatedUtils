package cc.fascinated.fascinatedutils.gui.modsettings;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Consumer;

import cc.fascinated.fascinatedutils.common.color.SettingColor;
import cc.fascinated.fascinatedutils.common.setting.Setting;
import cc.fascinated.fascinatedutils.common.setting.SettingCategory;
import cc.fascinated.fascinatedutils.common.setting.impl.BooleanSetting;
import cc.fascinated.fascinatedutils.common.setting.impl.ColorSetting;
import cc.fascinated.fascinatedutils.common.setting.impl.EnumSetting;
import cc.fascinated.fascinatedutils.common.setting.impl.KeybindSetting;
import cc.fascinated.fascinatedutils.common.setting.impl.SliderSetting;
import cc.fascinated.fascinatedutils.gui.core.Align;
import cc.fascinated.fascinatedutils.gui.core.Callback;
import cc.fascinated.fascinatedutils.gui.core.Ref;
import cc.fascinated.fascinatedutils.gui.theme.ModSettingsTheme;
import cc.fascinated.fascinatedutils.gui.theme.SettingsUiMetrics;
import cc.fascinated.fascinatedutils.gui.themes.fascinated.FascinatedGuiTheme;
import cc.fascinated.fascinatedutils.gui.widgets.FColumnWidget;
import cc.fascinated.fascinatedutils.gui.widgets.FLabelWidget;
import cc.fascinated.fascinatedutils.gui.widgets.FMinWidthHostWidget;
import cc.fascinated.fascinatedutils.gui.widgets.FRowWidget;
import cc.fascinated.fascinatedutils.gui.widgets.FScrollColumnWidget;
import cc.fascinated.fascinatedutils.gui.widgets.FSpacerWidget;
import cc.fascinated.fascinatedutils.gui.widgets.FTheme;
import cc.fascinated.fascinatedutils.gui.widgets.FWidget;
import cc.fascinated.fascinatedutils.gui.widgets.settings.FBooleanSettingGridCellWidget;
import cc.fascinated.fascinatedutils.gui.widgets.settings.FBooleanSettingRowWidget;
import cc.fascinated.fascinatedutils.gui.widgets.settings.FColorSettingRowWidget;
import cc.fascinated.fascinatedutils.gui.widgets.settings.FEnumSettingRowWidget;
import cc.fascinated.fascinatedutils.gui.widgets.settings.FGlobalHudBooleanApplyRowWidget;
import cc.fascinated.fascinatedutils.gui.widgets.settings.FGlobalHudColorApplyRowWidget;
import cc.fascinated.fascinatedutils.gui.widgets.settings.FGlobalHudSliderApplyRowWidget;
import cc.fascinated.fascinatedutils.gui.widgets.settings.FHudWidgetVisibilityCardWidget;
import cc.fascinated.fascinatedutils.gui.widgets.settings.FKeybindSettingWidget;
import cc.fascinated.fascinatedutils.gui.widgets.settings.FModSettingsDetailHeaderCardWidget;
import cc.fascinated.fascinatedutils.gui.widgets.settings.FSliderSettingRowWidget;
import cc.fascinated.fascinatedutils.settings.SettingsRegistry;
import cc.fascinated.fascinatedutils.systems.hud.HUDManager;
import cc.fascinated.fascinatedutils.systems.hud.HudAppearanceBulkApply;
import cc.fascinated.fascinatedutils.systems.hud.HudModule;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;

public class ModSettingsWidgetsTabBuilder {

    private static final float GRID_MIN_CELL_WIDTH_DESIGN = 90f;
    private static final float GRID_MARGIN_X_DESIGN = 8f;

    public static FWidget buildWidgetsTab(float paneWidth, float paneHeight, List<HudModule> hudWidgets, Ref<Float> widgetsPaneScrollYRef, HudModule settingsWidget, Runnable onBackFromSettings, Callback<HudModule> onOpenWidgetSettings, Ref<Float> widgetSettingsPaneScrollYRef, Consumer<ColorSetting> openColorPicker) {
        if (settingsWidget != null) {
            return buildWidgetSettingsDetail(paneWidth, settingsWidget, onBackFromSettings, widgetSettingsPaneScrollYRef, openColorPicker);
        }
        return buildWidgetCardGrid(paneWidth, hudWidgets, widgetsPaneScrollYRef, onOpenWidgetSettings, openColorPicker);
    }

    public static FWidget editorForWidgetSetting(Setting<?> setting, float settingsInnerWidth, float sliderValueColumnStartX, Consumer<ColorSetting> openColorPicker) {
        Runnable onValueChanged = HUDManager.INSTANCE::saveAll;
        if (setting instanceof BooleanSetting booleanSetting) {
            float editorHeight = SettingsUiMetrics.booleanOuterHeight();
            return new FBooleanSettingRowWidget(booleanSetting, settingsInnerWidth, editorHeight, onValueChanged, sliderValueColumnStartX);
        }
        if (setting instanceof SliderSetting sliderSetting) {
            float editorHeight = SettingsUiMetrics.floatOuterHeight();
            return new FSliderSettingRowWidget(sliderSetting, settingsInnerWidth, editorHeight, onValueChanged, sliderValueColumnStartX);
        }
        if (setting instanceof KeybindSetting keybindSetting) {
            float editorHeight = SettingsUiMetrics.booleanOuterHeight();
            return new FKeybindSettingWidget(keybindSetting, settingsInnerWidth, editorHeight, HUDManager.INSTANCE::saveAll);
        }
        if (setting instanceof EnumSetting<?> enumSetting) {
            float editorHeight = SettingsUiMetrics.booleanOuterHeight();
            return new FEnumSettingRowWidget(enumSetting, settingsInnerWidth, editorHeight, HUDManager.INSTANCE::saveAll, sliderValueColumnStartX);
        }
        if (setting instanceof ColorSetting colorSetting) {
            float editorHeight = SettingsUiMetrics.booleanOuterHeight();
            return new FColorSettingRowWidget(colorSetting, settingsInnerWidth, editorHeight, HUDManager.INSTANCE::saveAll, sliderValueColumnStartX, openColorPicker);
        }
        return null;
    }

    private static FWidget buildWidgetCardGrid(float paneWidth, List<HudModule> hudWidgets, Ref<Float> widgetsPaneScrollYRef, Callback<HudModule> onOpenWidgetSettings, Consumer<ColorSetting> openColorPicker) {
        float gridMarginX = GRID_MARGIN_X_DESIGN;
        float paddedInnerWidth = Math.max(0f, paneWidth - 2f * gridMarginX);
        float settingsContentWidth = Math.max(28f, paddedInnerWidth);
        float settingsInnerWidth = Math.max(14f, settingsContentWidth - 2f * ModSettingsTheme.SIDEBAR_SEPARATOR_PAD_X);
        float gapY = 6f;
        float gapX = 3f;
        float minCellWidth = GRID_MIN_CELL_WIDTH_DESIGN;
        int columnCount = computeCardGridColumnCount(settingsContentWidth, gapX, minCellWidth);
        float cellWidth = (settingsContentWidth - gapX * Math.max(0, columnCount - 1)) / Math.max(1, columnCount);
        float cellHeight = FHudWidgetVisibilityCardWidget.stackedCellOuterHeightPx();

        FColumnWidget scrollBody = new FColumnWidget(gapY, Align.CENTER);
        scrollBody.addChild(new FSpacerWidget(settingsContentWidth, 4f));

        if (hudWidgets == null || hudWidgets.isEmpty()) {
            scrollBody.addChild(new FSpacerWidget(settingsContentWidth, 2f));
            FLabelWidget empty = new FLabelWidget();
            empty.setText(Component.translatable("fascinatedutils.setting.shell.empty_widgets").getString());
            empty.setColorArgb(FascinatedGuiTheme.INSTANCE.textMuted());
            empty.setAlignX(Align.CENTER);
            scrollBody.addChild(empty);
            scrollBody.addChild(new FSpacerWidget(settingsContentWidth, 4f));
            FScrollColumnWidget emptyClip = FTheme.components().createScrollColumn(scrollBody, gapY);
            emptyClip.setFillVerticalInColumn(true);
            if (widgetsPaneScrollYRef != null) {
                Float scrollOffsetY = widgetsPaneScrollYRef.getValue();
                emptyClip.setScrollOffsetY(scrollOffsetY == null ? 0f : scrollOffsetY);
                emptyClip.setScrollOffsetChangeListener(widgetsPaneScrollYRef::setValue);
            }
            return emptyClip;
        }
        List<HudModule> sortedWidgets = new ArrayList<>(hudWidgets);
        sortedWidgets.sort((left, right) -> String.CASE_INSENSITIVE_ORDER.compare(left.getName().toLowerCase(Locale.ROOT), right.getName().toLowerCase(Locale.ROOT)));
        for (int widgetIndex = 0; widgetIndex < sortedWidgets.size(); widgetIndex += columnCount) {
            FRowWidget row = new FRowWidget(gapX, Align.START);
            for (int columnIndex = 0; columnIndex < columnCount; columnIndex++) {
                int index = widgetIndex + columnIndex;
                if (index >= sortedWidgets.size()) {
                    break;
                }
                HudModule widget = sortedWidgets.get(index);
                row.addChild(FTheme.components().createHudWidgetVisibilityCard(widget, cellWidth, cellHeight, widget::setEnabled, onOpenWidgetSettings));
            }
            scrollBody.addChild(row);
        }
        appendGlobalHudAppearanceBulkSection(scrollBody, settingsContentWidth, settingsInnerWidth, sortedWidgets, openColorPicker);
        scrollBody.addChild(new FSpacerWidget(settingsContentWidth, 4f));
        FScrollColumnWidget clip = FTheme.components().createScrollColumn(scrollBody, gapY);
        clip.setFillVerticalInColumn(true);
        if (widgetsPaneScrollYRef != null) {
            Float scrollOffsetY = widgetsPaneScrollYRef.getValue();
            clip.setScrollOffsetY(scrollOffsetY == null ? 0f : scrollOffsetY);
            clip.setScrollOffsetChangeListener(widgetsPaneScrollYRef::setValue);
        }
        return clip;
    }

    private static FWidget buildWidgetSettingsDetail(float paneWidth, HudModule settingsWidget, Runnable onBackFromSettings, Ref<Float> widgetSettingsPaneScrollYRef, Consumer<ColorSetting> openColorPicker) {
        float settingsContentWidth = Math.max(28f, paneWidth);
        float settingsInnerWidth = Math.max(14f, settingsContentWidth - 2f * ModSettingsTheme.SIDEBAR_SEPARATOR_PAD_X);
        float gap = 3f;
        FColumnWidget scrollBody = new FColumnWidget(gap, Align.START);
        scrollBody.addChild(new FSpacerWidget(settingsContentWidth, 4f));
        scrollBody.addChild(FModSettingsDetailHeaderCardWidget.centeredInContentRow(settingsContentWidth, settingsInnerWidth, onBackFromSettings, settingsWidget.getName()));
        scrollBody.addChild(new FSpacerWidget(settingsContentWidth, 3f));
        appendWidgetSettingsDetailRows(scrollBody, settingsWidget, settingsContentWidth, settingsInnerWidth, openColorPicker);
        return wrapScrollClip(scrollBody, gap, widgetSettingsPaneScrollYRef);
    }

    private static void appendWidgetSettingsDetailRows(FColumnWidget scrollBody, HudModule settingsWidget, float settingsContentWidth, float settingsInnerWidth, Consumer<ColorSetting> openColorPicker) {
        List<Setting<?>> widgetAllSettings = settingsWidget.getAllSettings();
        if (widgetAllSettings.isEmpty()) {
            scrollBody.addChild(new FSpacerWidget(settingsContentWidth, 2f));
            FLabelWidget empty = new FLabelWidget();
            empty.setText(Component.translatable("fascinatedutils.setting.shell.widget_settings.empty").getString());
            empty.setColorArgb(FascinatedGuiTheme.INSTANCE.textMuted());
            empty.setAlignX(Align.START);
            scrollBody.addChild(ModSettingsCategoryRows.wrapSettingsDetailRowInShellMargin(settingsContentWidth, settingsInnerWidth, new FMinWidthHostWidget(ModSettingsCategoryRows.settingsDetailPaddedInnerWidth(settingsInnerWidth), empty)));
            return;
        }
        List<ModSettingsCategoryRows.CategoryBlock> categoryBlocks = new ArrayList<>();
        for (SettingCategory category : settingsWidget.getSettingCategories()) {
            categoryBlocks.add(new ModSettingsCategoryRows.CategoryBlock(category.displayNameKey(), category.settings()));
        }
        ModSettingsCategoryRows.appendTopLevelThenCategories(scrollBody, settingsContentWidth, settingsInnerWidth, categoryBlocks, settingsWidget.getSettings(), (setting, innerWidth, sliderStartX) -> editorForWidgetSetting(setting, innerWidth, sliderStartX, openColorPicker), (booleanSetting, cellWidth, cellHeight) -> new FBooleanSettingGridCellWidget(booleanSetting, cellWidth, cellHeight, HUDManager.INSTANCE::saveAll));
        scrollBody.addChild(new FSpacerWidget(settingsContentWidth, ModSettingsCategoryRows.SETTINGS_SCROLL_BOTTOM_INSET));
    }

    private static void appendGlobalHudAppearanceBulkSection(FColumnWidget scrollBody, float settingsContentWidth, float settingsInnerWidth, List<HudModule> sortedWidgets, Consumer<ColorSetting> openColorPicker) {
        float paddedInnerWidth = ModSettingsCategoryRows.settingsDetailPaddedInnerWidth(settingsInnerWidth);
        float booleanRowHeight = SettingsUiMetrics.booleanOuterHeight();
        scrollBody.addChild(new FSpacerWidget(settingsContentWidth, SettingsUiMetrics.CATEGORY_SECTION_GAP));
        FLabelWidget heading = new FLabelWidget();
        heading.setText(I18n.get("fascinatedutils.setting.shell.global_hud_appearance_section"));
        heading.setColorArgb(FascinatedGuiTheme.INSTANCE.sectionHeaderText());
        heading.setAlignX(Align.START);
        scrollBody.addChild(ModSettingsCategoryRows.wrapSettingsDetailRowInShellMargin(settingsContentWidth, settingsInnerWidth, new FMinWidthHostWidget(paddedInnerWidth, heading)));
        scrollBody.addChild(new FSpacerWidget(settingsContentWidth, SettingsUiMetrics.CATEGORY_AFTER_HEADER_ROW_GAP));
        appendGlobalHudAppearanceApplyRow(scrollBody, settingsContentWidth, settingsInnerWidth, paddedInnerWidth, sortedWidgets, booleanRowHeight, false, "show_hud_background", "fascinatedutils.module.show_hud_background", true);
        appendGlobalHudAppearanceApplyRow(scrollBody, settingsContentWidth, settingsInnerWidth, paddedInnerWidth, sortedWidgets, booleanRowHeight, true, "rounded_corners", "fascinatedutils.module.rounded_corners", false);
        appendGlobalHudAppearanceApplyRow(scrollBody, settingsContentWidth, settingsInnerWidth, paddedInnerWidth, sortedWidgets, booleanRowHeight, true, "show_border", "fascinatedutils.module.show_border", false);
        appendGlobalHudAppearanceApplySliderRow(scrollBody, settingsContentWidth, settingsInnerWidth, paddedInnerWidth, sortedWidgets, true, "border_thickness", "fascinatedutils.module.border_thickness");
        appendGlobalHudAppearanceApplySliderRow(scrollBody, settingsContentWidth, settingsInnerWidth, paddedInnerWidth, sortedWidgets, true, "rounding_radius", "fascinatedutils.module.rounding_radius");
        ColorSetting hudBackgroundRegistry = SettingsRegistry.INSTANCE.getSettings().getHudBackgroundColor();
        appendGlobalHudAppearanceApplyRegistryColorRow(scrollBody, settingsContentWidth, settingsInnerWidth, paddedInnerWidth, hudBackgroundRegistry, openColorPicker, true);
        ColorSetting hudBorderRegistry = SettingsRegistry.INSTANCE.getSettings().getHudBorderColor();
        appendGlobalHudAppearanceApplyRegistryColorRow(scrollBody, settingsContentWidth, settingsInnerWidth, paddedInnerWidth, hudBorderRegistry, openColorPicker, true);
    }

    private static void appendGlobalHudAppearanceApplyRow(FColumnWidget scrollBody, float settingsContentWidth, float settingsInnerWidth, float paddedInnerWidth, List<HudModule> sortedWidgets, float rowHeight, boolean addGapAbove, String persistedSettingId, String translationKeyPath, boolean whenUnknown) {
        if (addGapAbove) {
            scrollBody.addChild(new FSpacerWidget(settingsContentWidth, SettingsUiMetrics.SETTING_GROUP_GAP));
        }
        BooleanSetting staging = globalStagingBooleanForHudWidgets(sortedWidgets, persistedSettingId, translationKeyPath, whenUnknown);
        FGlobalHudBooleanApplyRowWidget row = new FGlobalHudBooleanApplyRowWidget(staging, paddedInnerWidth, rowHeight, () -> HudAppearanceBulkApply.applyBooleanToAllHudModules(sortedWidgets, persistedSettingId, Boolean.TRUE.equals(staging.getValue())));
        scrollBody.addChild(ModSettingsCategoryRows.wrapSettingsDetailRowInShellMargin(settingsContentWidth, settingsInnerWidth, new FMinWidthHostWidget(paddedInnerWidth, row)));
    }

    private static void appendGlobalHudAppearanceApplySliderRow(FColumnWidget scrollBody, float settingsContentWidth, float settingsInnerWidth, float paddedInnerWidth, List<HudModule> sortedWidgets, boolean addGapAbove, String persistedSettingId, String translationKeyPath) {
        if (addGapAbove) {
            scrollBody.addChild(new FSpacerWidget(settingsContentWidth, SettingsUiMetrics.SETTING_GROUP_GAP));
        }
        SliderSetting staging = globalStagingSliderForHudWidgets(sortedWidgets, persistedSettingId, translationKeyPath);
        float sliderValueColumnStartX = ModSettingsCategoryRows.computeValueColumnStartX(List.of(staging));
        float rowHeight = SettingsUiMetrics.floatOuterHeight();
        FGlobalHudSliderApplyRowWidget row = new FGlobalHudSliderApplyRowWidget(staging, paddedInnerWidth, rowHeight, sliderValueColumnStartX, () -> HudAppearanceBulkApply.applySliderToAllHudModules(sortedWidgets, persistedSettingId, staging.getValue().floatValue()));
        scrollBody.addChild(ModSettingsCategoryRows.wrapSettingsDetailRowInShellMargin(settingsContentWidth, settingsInnerWidth, new FMinWidthHostWidget(paddedInnerWidth, row)));
    }

    private static void appendGlobalHudAppearanceApplyRegistryColorRow(FColumnWidget scrollBody, float settingsContentWidth, float settingsInnerWidth, float paddedInnerWidth, ColorSetting registryColor, Consumer<ColorSetting> openColorPicker, boolean addGapAbove) {
        if (addGapAbove) {
            scrollBody.addChild(new FSpacerWidget(settingsContentWidth, SettingsUiMetrics.SETTING_GROUP_GAP));
        }
        ColorSetting staging = globalStagingRegistryColor(registryColor);
        float sliderValueColumnStartX = ModSettingsCategoryRows.computeValueColumnStartX(List.of(staging));
        float rowHeight = SettingsUiMetrics.booleanOuterHeight();
        FGlobalHudColorApplyRowWidget row = new FGlobalHudColorApplyRowWidget(staging, paddedInnerWidth, rowHeight, sliderValueColumnStartX, () -> HudAppearanceBulkApply.applyRegistryColorFromStaging(registryColor, staging), openColorPicker);
        scrollBody.addChild(ModSettingsCategoryRows.wrapSettingsDetailRowInShellMargin(settingsContentWidth, settingsInnerWidth, new FMinWidthHostWidget(paddedInnerWidth, row)));
    }

    private static BooleanSetting globalStagingBooleanForHudWidgets(List<HudModule> hudWidgets, String persistedSettingId, String translationKeyPath, boolean whenUnknown) {
        Optional<Boolean> consensus = HudAppearanceBulkApply.booleanConsensus(hudWidgets, persistedSettingId);
        boolean initial = consensus.orElse(whenUnknown);
        return BooleanSetting.builder().id("widgets_global_staging_" + persistedSettingId).defaultValue(initial).value(initial).translationKeyPath(translationKeyPath).build();
    }

    private static SliderSetting globalStagingSliderForHudWidgets(List<HudModule> sortedWidgets, String persistedSettingId, String translationKeyPath) {
        SliderSetting template = sortedWidgets.get(0).getSetting(SliderSetting.class, persistedSettingId).orElseThrow();
        Optional<Float> consensus = HudAppearanceBulkApply.floatSliderConsensus(sortedWidgets, persistedSettingId);
        float initial = consensus.orElse(template.getDefaultValue().floatValue());
        SliderSetting.Builder builder = SliderSetting.builder().id("widgets_global_staging_" + persistedSettingId).minValue(template.getMin()).maxValue(template.getMax()).step(template.getStep()).defaultValue(initial).value(initial).translationKeyPath(translationKeyPath);
        if (template.getValueFormatter() != null) {
            builder.valueFormatter(template.getValueFormatter());
        }
        return builder.build();
    }

    private static ColorSetting globalStagingRegistryColor(ColorSetting registryColor) {
        SettingColor initial = registryColor.getValue().copy();
        return ColorSetting.builder().id("widgets_global_staging_" + registryColor.getSettingKey()).defaultValue(initial.copy()).value(initial.copy()).translationKeyPath(registryColor.getTranslationKeyPath()).build();
    }

    private static FScrollColumnWidget wrapScrollClip(FColumnWidget body, float gap, Ref<Float> scrollYRef) {
        FScrollColumnWidget clip = FTheme.components().createScrollColumn(body, gap);
        clip.setFillVerticalInColumn(true);
        if (scrollYRef != null) {
            Float scrollOffsetY = scrollYRef.getValue();
            clip.setScrollOffsetY(scrollOffsetY == null ? 0f : scrollOffsetY);
            clip.setScrollOffsetChangeListener(scrollYRef::setValue);
        }
        return clip;
    }

    private static int computeCardGridColumnCount(float contentWidth, float gapX, float minCellWidth) {
        if (contentWidth <= 0f || minCellWidth <= 0f) {
            return 1;
        }
        int maximumColumns = Math.max(1, (int) Math.floor((contentWidth + gapX) / (minCellWidth + gapX)));
        int chosenColumns = 1;
        for (int candidate = maximumColumns; candidate >= 1; candidate--) {
            float widthPerCell = (contentWidth - gapX * Math.max(0, candidate - 1)) / candidate;
            if (widthPerCell >= minCellWidth - 0.5f) {
                chosenColumns = candidate;
                break;
            }
        }
        return chosenColumns;
    }
}
