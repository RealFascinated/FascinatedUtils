package cc.fascinated.fascinatedutils.gui.modsettings;

import cc.fascinated.fascinatedutils.common.setting.Setting;
import cc.fascinated.fascinatedutils.common.setting.SettingCategory;
import cc.fascinated.fascinatedutils.common.setting.impl.*;
import cc.fascinated.fascinatedutils.gui.GuiDesignSpace;
import cc.fascinated.fascinatedutils.gui.core.Align;
import cc.fascinated.fascinatedutils.gui.core.Callback;
import cc.fascinated.fascinatedutils.gui.core.Ref;
import cc.fascinated.fascinatedutils.gui.theme.ModSettingsTheme;
import cc.fascinated.fascinatedutils.gui.theme.SettingsUiMetrics;
import cc.fascinated.fascinatedutils.gui.theme.UITheme;
import cc.fascinated.fascinatedutils.gui.themes.fascinated.FascinatedGuiTheme;
import cc.fascinated.fascinatedutils.gui.widgets.*;
import cc.fascinated.fascinatedutils.gui.widgets.settings.*;
import cc.fascinated.fascinatedutils.systems.hud.HUDManager;
import cc.fascinated.fascinatedutils.systems.hud.HudModule;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

public class ModSettingsWidgetsTabBuilder {

    private static final float GRID_MIN_CELL_WIDTH_DESIGN = 128f;
    private static final float GRID_MARGIN_X_DESIGN = 12f;

    public static FWidget buildWidgetsTab(float paneWidth, float paneHeight, List<HudModule> hudWidgets, Ref<Float> widgetsPaneScrollYRef, HudModule settingsWidget, Runnable onBackFromSettings, Callback<HudModule> onOpenWidgetSettings, Ref<Float> widgetSettingsPaneScrollYRef, Consumer<ColorSetting> openColorPicker) {
        if (settingsWidget != null) {
            return buildWidgetSettingsDetail(paneWidth, settingsWidget, onBackFromSettings, widgetSettingsPaneScrollYRef, openColorPicker);
        }
        return buildWidgetCardGrid(paneWidth, hudWidgets, widgetsPaneScrollYRef, onOpenWidgetSettings);
    }

    /**
     * Builds the scrolled settings column for a HUD widget appearance panel: same header chrome pattern as the
     * widgets tab detail (without a back control), then the same category rows and
     * {@link #editorForWidgetSetting} row widgets as the widgets tab per-widget settings detail.
     *
     * @param model                HUD widget whose settings are edited
     * @param settingsContentWidth full content width for the panel column
     * @param headerTitle          title string for the header card
     * @param scrollYRef           optional ref used to persist vertical scroll offset, or {@code null}
     * @return scroll clip containing the column
     */
    public static FWidget buildHudWidgetAppearanceSettingsScroll(HudModule model, float settingsContentWidth, String headerTitle, Ref<Float> scrollYRef, Consumer<ColorSetting> openColorPicker) {
        float settingsInnerWidth = Math.max(GuiDesignSpace.pxX(20f), settingsContentWidth - 2f * GuiDesignSpace.pxX(ModSettingsTheme.SIDEBAR_SEPARATOR_PAD_X));
        float gap = GuiDesignSpace.pxY(UITheme.GAP_SM);
        FColumnWidget scrollBody = new FColumnWidget(gap, Align.START);
        scrollBody.addChild(new FSpacerWidget(settingsContentWidth, GuiDesignSpace.pxY(UITheme.PADDING_SM)));
        scrollBody.addChild(FModSettingsDetailHeaderCardWidget.centeredTitleOnlyInContentRow(settingsContentWidth, settingsInnerWidth, headerTitle));
        scrollBody.addChild(new FSpacerWidget(settingsContentWidth, GuiDesignSpace.pxY(UITheme.GAP_SM)));
        appendWidgetSettingsDetailRows(scrollBody, model, settingsContentWidth, settingsInnerWidth, openColorPicker);
        return wrapScrollClip(scrollBody, gap, scrollYRef);
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

    private static FWidget buildWidgetCardGrid(float paneWidth, List<HudModule> hudWidgets, Ref<Float> widgetsPaneScrollYRef, Callback<HudModule> onOpenWidgetSettings) {
        float gridMarginX = GuiDesignSpace.pxX(GRID_MARGIN_X_DESIGN);
        float paddedInnerWidth = Math.max(0f, paneWidth - 2f * gridMarginX);
        float settingsContentWidth = Math.max(GuiDesignSpace.pxX(40f), paddedInnerWidth);
        float gapY = GuiDesignSpace.pxY(UITheme.GAP_MD);
        float gapX = GuiDesignSpace.pxX(UITheme.GAP_SM);
        float minCellWidth = GuiDesignSpace.pxX(GRID_MIN_CELL_WIDTH_DESIGN);
        int columnCount = computeCardGridColumnCount(settingsContentWidth, gapX, minCellWidth);
        float cellWidth = (settingsContentWidth - gapX * Math.max(0, columnCount - 1)) / Math.max(1, columnCount);
        float cellHeight = FHudWidgetVisibilityCardWidget.stackedCellOuterHeightPx();

        FColumnWidget scrollBody = new FColumnWidget(gapY, Align.CENTER);
        scrollBody.addChild(new FSpacerWidget(settingsContentWidth, GuiDesignSpace.pxY(UITheme.PADDING_SM)));

        if (hudWidgets == null || hudWidgets.isEmpty()) {
            scrollBody.addChild(new FSpacerWidget(settingsContentWidth, GuiDesignSpace.pxY(UITheme.PADDING_XS)));
            FLabelWidget empty = new FLabelWidget();
            empty.setText(Component.translatable("fascinatedutils.setting.shell.empty_widgets").getString());
            empty.setColorArgb(FascinatedGuiTheme.INSTANCE.textMuted());
            empty.setAlignX(Align.CENTER);
            scrollBody.addChild(empty);
            scrollBody.addChild(new FSpacerWidget(settingsContentWidth, GuiDesignSpace.pxY(UITheme.PADDING_SM)));
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
                row.addChild(FTheme.components().createHudWidgetVisibilityCard(widget, cellWidth, cellHeight, newVisibility -> {
                    HUDManager.INSTANCE.setWidgetVisible(widget, newVisibility, true);
                }, onOpenWidgetSettings));
            }
            scrollBody.addChild(row);
        }
        scrollBody.addChild(new FSpacerWidget(settingsContentWidth, GuiDesignSpace.pxY(UITheme.PADDING_SM)));
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
        float settingsContentWidth = Math.max(GuiDesignSpace.pxX(40f), paneWidth);
        float settingsInnerWidth = Math.max(GuiDesignSpace.pxX(20f), settingsContentWidth - 2f * GuiDesignSpace.pxX(ModSettingsTheme.SIDEBAR_SEPARATOR_PAD_X));
        float gap = GuiDesignSpace.pxY(UITheme.GAP_SM);
        FColumnWidget scrollBody = new FColumnWidget(gap, Align.START);
        scrollBody.addChild(new FSpacerWidget(settingsContentWidth, GuiDesignSpace.pxY(UITheme.PADDING_SM)));
        scrollBody.addChild(FModSettingsDetailHeaderCardWidget.centeredInContentRow(settingsContentWidth, settingsInnerWidth, onBackFromSettings, settingsWidget.getName()));
        scrollBody.addChild(new FSpacerWidget(settingsContentWidth, GuiDesignSpace.pxY(UITheme.GAP_SM)));
        appendWidgetSettingsDetailRows(scrollBody, settingsWidget, settingsContentWidth, settingsInnerWidth, openColorPicker);
        return wrapScrollClip(scrollBody, gap, widgetSettingsPaneScrollYRef);
    }

    private static void appendWidgetSettingsDetailRows(FColumnWidget scrollBody, HudModule settingsWidget, float settingsContentWidth, float settingsInnerWidth, Consumer<ColorSetting> openColorPicker) {
        List<Setting<?>> widgetAllSettings = settingsWidget.getAllSettings();
        if (widgetAllSettings.isEmpty()) {
            scrollBody.addChild(new FSpacerWidget(settingsContentWidth, GuiDesignSpace.pxY(UITheme.PADDING_XS)));
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
        ModSettingsCategoryRows.appendTopLevelThenCategories(scrollBody, settingsContentWidth, settingsInnerWidth, categoryBlocks, settingsWidget.getSettings(), (setting, innerWidth, sliderStartX) -> editorForWidgetSetting(setting, innerWidth, sliderStartX, openColorPicker));
        scrollBody.addChild(new FSpacerWidget(settingsContentWidth, GuiDesignSpace.pxY(ModSettingsCategoryRows.SETTINGS_SCROLL_BOTTOM_INSET)));
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
