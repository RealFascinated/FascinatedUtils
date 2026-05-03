package cc.fascinated.fascinatedutils.gui.modsettings;

import cc.fascinated.fascinatedutils.common.setting.Setting;
import cc.fascinated.fascinatedutils.common.setting.SettingCategory;
import cc.fascinated.fascinatedutils.common.setting.impl.*;
import cc.fascinated.fascinatedutils.gui.core.Align;
import cc.fascinated.fascinatedutils.gui.core.Callback;
import cc.fascinated.fascinatedutils.gui.core.Ref;
import cc.fascinated.fascinatedutils.gui.theme.ModSettingsTheme;
import cc.fascinated.fascinatedutils.gui.theme.SettingsUiMetrics;
import cc.fascinated.fascinatedutils.gui.themes.FascinatedGuiTheme;
import cc.fascinated.fascinatedutils.gui.widgets.*;
import cc.fascinated.fascinatedutils.gui.widgets.settings.*;
import cc.fascinated.fascinatedutils.systems.hud.HUDManager;
import cc.fascinated.fascinatedutils.systems.hud.HudPanel;
import cc.fascinated.fascinatedutils.systems.modules.Module;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

public class ModSettingsWidgetsTabBuilder {

    private static final float GRID_MIN_CELL_WIDTH_DESIGN = 90f;
    private static final float GRID_MARGIN_X_DESIGN = 8f;

    public static FWidget buildWidgetsTab(float paneWidth, float paneHeight, List<HudPanel> hudPanels, Ref<Float> widgetsPaneScrollYRef, HudPanel settingsPanel, Runnable onBackFromSettings, Callback<HudPanel> onOpenPanelSettings, Ref<Float> widgetSettingsPaneScrollYRef, Consumer<ColorSetting> openColorPicker) {
        if (settingsPanel != null) {
            return buildWidgetSettingsDetail(paneWidth, settingsPanel, onBackFromSettings, widgetSettingsPaneScrollYRef, openColorPicker);
        }
        return buildWidgetCardGrid(paneWidth, hudPanels, widgetsPaneScrollYRef, onOpenPanelSettings, openColorPicker);
    }

    public static FWidget editorForWidgetSetting(Setting<?> setting, float settingsInnerWidth, float sliderValueColumnStartX, Consumer<ColorSetting> openColorPicker) {
        Runnable onValueChanged = HUDManager.INSTANCE::saveAll;
        if (setting instanceof BooleanSetting booleanSetting && booleanSetting.hasSubSettings()) {
            float editorHeight = SettingsUiMetrics.booleanOuterHeight();
            return new FBooleanWithSubSettingsWidget(booleanSetting, onValueChanged, settingsInnerWidth, editorHeight, sliderValueColumnStartX, (sub, subW, subColX) -> editorForWidgetSetting(sub, subW, subColX, openColorPicker));
        }
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

    private static FWidget buildWidgetCardGrid(float paneWidth, List<HudPanel> hudPanels, Ref<Float> widgetsPaneScrollYRef, Callback<HudPanel> onOpenPanelSettings, Consumer<ColorSetting> openColorPicker) {
        float paddedInnerWidth = Math.max(0f, paneWidth - 2f * GRID_MARGIN_X_DESIGN);
        float settingsContentWidth = Math.max(28f, paddedInnerWidth);
        float settingsInnerWidth = Math.max(14f, settingsContentWidth - 2f * ModSettingsTheme.SIDEBAR_SEPARATOR_PAD_X);
        float gapY = 6f;
        float gapX = 3f;
        int columnCount = computeCardGridColumnCount(settingsContentWidth, gapX, GRID_MIN_CELL_WIDTH_DESIGN);
        float cellWidth = (settingsContentWidth - gapX * Math.max(0, columnCount - 1)) / Math.max(1, columnCount);
        float cellHeight = FHudWidgetVisibilityCardWidget.stackedCellOuterHeightPx();

        FColumnWidget scrollBody = new FColumnWidget(gapY, Align.CENTER);
        scrollBody.addChild(new FSpacerWidget(settingsContentWidth, 4f));

        if (hudPanels == null || hudPanels.isEmpty()) {
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
        List<HudPanel> sortedPanels = new ArrayList<>(hudPanels);
        sortedPanels.sort((left, right) -> String.CASE_INSENSITIVE_ORDER.compare(left.getName().toLowerCase(Locale.ROOT), right.getName().toLowerCase(Locale.ROOT)));
        for (int widgetIndex = 0; widgetIndex < sortedPanels.size(); widgetIndex += columnCount) {
            FRowWidget row = new FRowWidget(gapX, Align.START);
            for (int columnIndex = 0; columnIndex < columnCount; columnIndex++) {
                int index = widgetIndex + columnIndex;
                if (index >= sortedPanels.size()) {
                    break;
                }
                HudPanel panel = sortedPanels.get(index);
                row.addChild(FTheme.components().createHudWidgetVisibilityCard(panel, cellWidth, cellHeight, visibilityFlag -> HUDManager.INSTANCE.saveAll(), onOpenPanelSettings));
            }
            scrollBody.addChild(row);
        }
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

    private static FWidget buildWidgetSettingsDetail(float paneWidth, HudPanel settingsPanel, Runnable onBackFromSettings, Ref<Float> widgetSettingsPaneScrollYRef, Consumer<ColorSetting> openColorPicker) {
        float settingsContentWidth = Math.max(28f, paneWidth);
        float settingsInnerWidth = Math.max(14f, settingsContentWidth - 2f * ModSettingsTheme.SIDEBAR_SEPARATOR_PAD_X);
        float gap = 3f;
        FColumnWidget scrollBody = new FColumnWidget(gap, Align.START);
        scrollBody.addChild(new FSpacerWidget(settingsContentWidth, 4f));
        Module settingsHost = settingsPanel.hudHostModule();
        scrollBody.addChild(FModSettingsDetailHeaderCardWidget.centeredInContentRow(settingsContentWidth, settingsInnerWidth, onBackFromSettings, settingsHost.getDisplayName()));
        scrollBody.addChild(new FSpacerWidget(settingsContentWidth, 3f));
        appendWidgetSettingsDetailRows(scrollBody, settingsHost, settingsContentWidth, settingsInnerWidth, openColorPicker);
        return wrapScrollClip(scrollBody, gap, widgetSettingsPaneScrollYRef);
    }

    private static void appendWidgetSettingsDetailRows(FColumnWidget scrollBody, Module settingsHost, float settingsContentWidth, float settingsInnerWidth, Consumer<ColorSetting> openColorPicker) {
        List<Setting<?>> widgetAllSettings = settingsHost.getAllSettings();
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
        for (SettingCategory category : settingsHost.getSettingCategories()) {
            categoryBlocks.add(new ModSettingsCategoryRows.CategoryBlock(category.displayNameKey(), category.settings()));
        }
        ModSettingsCategoryRows.appendTopLevelThenCategories(scrollBody, settingsContentWidth, settingsInnerWidth, categoryBlocks, settingsHost.getSettings(), (setting, innerWidth, sliderStartX) -> editorForWidgetSetting(setting, innerWidth, sliderStartX, openColorPicker));
        scrollBody.addChild(new FSpacerWidget(settingsContentWidth, ModSettingsCategoryRows.SETTINGS_SCROLL_BOTTOM_INSET));
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
