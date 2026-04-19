package cc.fascinated.fascinatedutils.gui.modsettings;

import cc.fascinated.fascinatedutils.common.setting.Setting;
import cc.fascinated.fascinatedutils.common.setting.impl.*;
import cc.fascinated.fascinatedutils.gui.GuiDesignSpace;
import cc.fascinated.fascinatedutils.gui.core.Align;
import cc.fascinated.fascinatedutils.gui.theme.SettingsUiMetrics;
import cc.fascinated.fascinatedutils.gui.themes.fascinated.FascinatedGuiTheme;
import cc.fascinated.fascinatedutils.gui.widgets.*;
import lombok.experimental.UtilityClass;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.language.I18n;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

@UtilityClass
public class ModSettingsCategoryRows {

    public static final float SETTINGS_SCROLL_BOTTOM_INSET = 2f;

    public static float settingsDetailPaddedInnerWidth(float settingsInnerWidth) {
        return settingsDetailPaddedInnerWidth(
                settingsInnerWidth,
                SettingsUiMetrics.SETTINGS_DETAIL_CONTENT_INSET_X_DESIGN,
                SettingsUiMetrics.SETTINGS_DETAIL_CONTENT_INSET_X_DESIGN);
    }

    public static float settingsDetailPaddedInnerWidth(
            float settingsInnerWidth, float leftInsetDesign, float rightInsetDesign) {
        float leftPx = GuiDesignSpace.pxX(leftInsetDesign);
        float rightPx = GuiDesignSpace.pxX(rightInsetDesign);
        return Math.max(GuiDesignSpace.pxX(14f), settingsInnerWidth - leftPx - rightPx);
    }

    public static FWidget wrapSettingsDetailRowInShellMargin(float settingsContentWidth, float settingsInnerWidth, FWidget inner) {
        return wrapSettingsDetailRowInShellMargin(
                settingsContentWidth,
                settingsInnerWidth,
                inner,
                SettingsUiMetrics.SETTINGS_DETAIL_CONTENT_INSET_X_DESIGN,
                SettingsUiMetrics.SETTINGS_DETAIL_CONTENT_INSET_X_DESIGN);
    }

    public static FWidget wrapSettingsDetailRowInShellMargin(
            float settingsContentWidth,
            float settingsInnerWidth,
            FWidget inner,
            float leftInsetDesign,
            float rightInsetDesign) {
        float leftInset = GuiDesignSpace.pxX(leftInsetDesign);
        float rightInset = GuiDesignSpace.pxX(rightInsetDesign);
        if (leftInset <= 0f && rightInset <= 0f) {
            return new FMinWidthHostWidget(settingsContentWidth, inner);
        }
        float paddedInnerWidth = settingsDetailPaddedInnerWidth(settingsInnerWidth, leftInsetDesign, rightInsetDesign);
        FRowWidget row = new FRowWidget(0f, Align.START) {
            @Override
            public boolean fillsHorizontalInRow() {
                return true;
            }
        };
        row.addChild(new FSpacerWidget(leftInset, 0f));
        FMinWidthHostWidget innerHost = new FMinWidthHostWidget(paddedInnerWidth, inner);
        innerHost.setCellConstraints(new FCellConstraints().setExpandHorizontal(true));
        row.addChild(innerHost);
        row.addChild(new FSpacerWidget(rightInset, 0f));
        return new FMinWidthHostWidget(settingsContentWidth, row);
    }

    public static boolean isRenderableSetting(Setting<?> setting) {
        return setting instanceof BooleanSetting || setting instanceof SliderSetting || setting instanceof KeybindSetting || setting instanceof EnumSetting<?> || setting instanceof ColorSetting;
    }

    /**
     * Appends top-level settings, then each category block, batching consecutive {@link BooleanSetting} entries into a
     * two-column grid when at least two appear in a row.
     *
     * @param scrollBody               scroll column receiving rows
     * @param settingsContentWidth     full shell content width for horizontal spacers
     * @param settingsInnerWidth       inner width before shell horizontal inset
     * @param categories               category blocks after top-level settings
     * @param topLevelSettings         module/widget settings registered outside categories
     * @param editorFactory            editor for a single setting row
     * @param booleanGridCellFactory   compact cell used when two or more booleans are grouped
     */
    public static void appendTopLevelThenCategories(FColumnWidget scrollBody, float settingsContentWidth, float settingsInnerWidth, List<CategoryBlock> categories, List<Setting<?>> topLevelSettings, SettingRowEditorFactory editorFactory, ModSettingsBooleanTwoColumnGridBuilder.CellFactory booleanGridCellFactory) {
        List<Setting<?>> allForMeasure = new ArrayList<>(topLevelSettings);
        for (CategoryBlock category : categories) {
            allForMeasure.addAll(category.settings());
        }
        float sliderValueColumnStartX = computeValueColumnStartX(allForMeasure);
        float settingRowsPaddedInnerWidth = settingsDetailPaddedInnerWidth(
                settingsInnerWidth,
                SettingsUiMetrics.SETTINGS_DETAIL_CATEGORY_CONTENT_LEFT_INSET_X_DESIGN,
                SettingsUiMetrics.SETTINGS_DETAIL_CATEGORY_CONTENT_RIGHT_INSET_X_DESIGN);
        float categoryTitlePaddedInnerWidth = settingsDetailPaddedInnerWidth(
                settingsInnerWidth,
                SettingsUiMetrics.SETTINGS_DETAIL_CATEGORY_TITLE_LEFT_INSET_X_DESIGN,
                SettingsUiMetrics.SETTINGS_DETAIL_CATEGORY_CONTENT_RIGHT_INSET_X_DESIGN);
        boolean[] placedAnyRow = {false};
        appendSequencedSettings(scrollBody, settingsContentWidth, settingsInnerWidth, settingRowsPaddedInnerWidth, sliderValueColumnStartX, topLevelSettings, editorFactory, booleanGridCellFactory, placedAnyRow);
        for (CategoryBlock category : categories) {
            if (!categoryBlockHasRenderableSetting(category)) {
                continue;
            }
            if (placedAnyRow[0]) {
                scrollBody.addChild(new FSpacerWidget(settingsContentWidth, GuiDesignSpace.pxY(SettingsUiMetrics.CATEGORY_SECTION_GAP)));
            }
            if (categoryDisplaysSectionHeader(category.displayNameKey())) {
                FLabelWidget categoryLabel = new FLabelWidget();
                categoryLabel.setText(I18n.get(category.displayNameKey()));
                categoryLabel.setColorArgb(FascinatedGuiTheme.INSTANCE.sectionHeaderText());
                categoryLabel.setAlignX(Align.START);
                scrollBody.addChild(wrapSettingsDetailRowInShellMargin(
                        settingsContentWidth,
                        settingsInnerWidth,
                        new FMinWidthHostWidget(categoryTitlePaddedInnerWidth, categoryLabel),
                        SettingsUiMetrics.SETTINGS_DETAIL_CATEGORY_TITLE_LEFT_INSET_X_DESIGN,
                        SettingsUiMetrics.SETTINGS_DETAIL_CATEGORY_CONTENT_RIGHT_INSET_X_DESIGN));
                scrollBody.addChild(new FSpacerWidget(settingsContentWidth, GuiDesignSpace.pxY(SettingsUiMetrics.CATEGORY_AFTER_HEADER_ROW_GAP)));
            }
            appendSequencedSettings(scrollBody, settingsContentWidth, settingsInnerWidth, settingRowsPaddedInnerWidth, sliderValueColumnStartX, category.settings(), editorFactory, booleanGridCellFactory, placedAnyRow);
        }
    }

    public static float computeValueColumnStartX(List<Setting<?>> settings) {
        float maxLabelWidth = GuiDesignSpace.pxX(17f);
        for (Setting<?> setting : settings) {
            String labelText = setting.getTranslatedDisplayName();
            maxLabelWidth = Math.max(maxLabelWidth, measureSettingLabelWidth(labelText));
        }
        float baseOffset = GuiDesignSpace.pxX(35f);
        return maxLabelWidth + baseOffset;
    }

    private static void appendSequencedSettings(FColumnWidget scrollBody, float settingsContentWidth, float settingsInnerWidth, float widthForEditors, float sliderValueColumnStartX, List<Setting<?>> sequence, SettingRowEditorFactory editorFactory, ModSettingsBooleanTwoColumnGridBuilder.CellFactory booleanGridCellFactory, boolean[] placedAnyRow) {
        List<BooleanSetting> booleanRun = new ArrayList<>();
        for (Setting<?> setting : sequence) {
            if (setting instanceof BooleanSetting booleanSetting && isRenderableSetting(booleanSetting)) {
                booleanRun.add(booleanSetting);
                continue;
            }
            flushBooleanRun(scrollBody, settingsContentWidth, settingsInnerWidth, widthForEditors, sliderValueColumnStartX, booleanRun, editorFactory, booleanGridCellFactory, placedAnyRow);
            booleanRun.clear();
            if (!isRenderableSetting(setting)) {
                continue;
            }
            FWidget editor = editorFactory.create(setting, widthForEditors, sliderValueColumnStartX);
            appendSingleWrappedEditor(scrollBody, settingsContentWidth, settingsInnerWidth, editor, placedAnyRow);
        }
        flushBooleanRun(scrollBody, settingsContentWidth, settingsInnerWidth, widthForEditors, sliderValueColumnStartX, booleanRun, editorFactory, booleanGridCellFactory, placedAnyRow);
    }

    private static void flushBooleanRun(FColumnWidget scrollBody, float settingsContentWidth, float settingsInnerWidth, float widthForEditors, float sliderValueColumnStartX, List<BooleanSetting> booleanRun, SettingRowEditorFactory editorFactory, ModSettingsBooleanTwoColumnGridBuilder.CellFactory booleanGridCellFactory, boolean[] placedAnyRow) {
        if (booleanRun.isEmpty()) {
            return;
        }
        float cellHeight = SettingsUiMetrics.booleanOuterHeight();
        if (booleanRun.size() == 1) {
            FWidget editor = editorFactory.create(booleanRun.get(0), widthForEditors, sliderValueColumnStartX);
            appendSingleWrappedEditor(scrollBody, settingsContentWidth, settingsInnerWidth, editor, placedAnyRow);
            return;
        }
        FWidget grid = ModSettingsBooleanTwoColumnGridBuilder.build(widthForEditors, booleanRun, cellHeight, booleanGridCellFactory);
        if (placedAnyRow[0]) {
            scrollBody.addChild(new FSpacerWidget(settingsContentWidth, GuiDesignSpace.pxY(SettingsUiMetrics.SETTING_GROUP_GAP)));
        }
        placedAnyRow[0] = true;
        scrollBody.addChild(wrapSettingRowShellMargin(settingsContentWidth, settingsInnerWidth, grid));
    }

    private static void appendSingleWrappedEditor(FColumnWidget scrollBody, float settingsContentWidth, float settingsInnerWidth, @Nullable FWidget editor, boolean[] placedAnyRow) {
        if (editor == null) {
            return;
        }
        if (placedAnyRow[0]) {
            scrollBody.addChild(new FSpacerWidget(settingsContentWidth, GuiDesignSpace.pxY(SettingsUiMetrics.SETTING_GROUP_GAP)));
        }
        placedAnyRow[0] = true;
        scrollBody.addChild(wrapSettingRowShellMargin(settingsContentWidth, settingsInnerWidth, editor));
    }

    private static FWidget wrapSettingRowShellMargin(float settingsContentWidth, float settingsInnerWidth, FWidget inner) {
        return wrapSettingsDetailRowInShellMargin(
                settingsContentWidth,
                settingsInnerWidth,
                inner,
                SettingsUiMetrics.SETTINGS_DETAIL_CATEGORY_CONTENT_LEFT_INSET_X_DESIGN,
                SettingsUiMetrics.SETTINGS_DETAIL_CATEGORY_CONTENT_RIGHT_INSET_X_DESIGN);
    }

    private static boolean categoryDisplaysSectionHeader(String displayNameKey) {
        if (displayNameKey == null || displayNameKey.isBlank()) {
            return false;
        }
        String resolved = I18n.get(displayNameKey);
        return resolved != null && !resolved.isBlank();
    }

    private static boolean categoryBlockHasRenderableSetting(CategoryBlock category) {
        for (Setting<?> setting : category.settings()) {
            if (isRenderableSetting(setting)) {
                return true;
            }
        }
        return false;
    }

    private static float measureSettingLabelWidth(String labelText) {
        if (labelText == null || labelText.isEmpty()) {
            return 0f;
        }
        Minecraft minecraftClient = Minecraft.getInstance();
        if (minecraftClient == null) {
            return GuiDesignSpace.pxX(17f);
        }
        return GuiDesignSpace.pxX(minecraftClient.font.width(labelText));
    }

    @FunctionalInterface
    public interface SettingRowEditorFactory {
        @Nullable FWidget create(Setting<?> setting, float innerWidth, float sliderValueColumnStartX);
    }

    public record CategoryBlock(String displayNameKey, List<Setting<?>> settings) {
    }
}
