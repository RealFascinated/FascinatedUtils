package cc.fascinated.fascinatedutils.gui.modsettings;

import cc.fascinated.fascinatedutils.common.setting.Setting;
import cc.fascinated.fascinatedutils.common.setting.impl.*;
import cc.fascinated.fascinatedutils.gui.core.Align;
import cc.fascinated.fascinatedutils.gui.declare.Ui;
import cc.fascinated.fascinatedutils.gui.declare.UiSlot;
import cc.fascinated.fascinatedutils.gui.theme.SettingsUiMetrics;
import cc.fascinated.fascinatedutils.gui.themes.FascinatedGuiTheme;
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
        return settingsDetailPaddedInnerWidth(settingsInnerWidth, SettingsUiMetrics.SETTINGS_DETAIL_CONTENT_INSET_X_DESIGN, SettingsUiMetrics.SETTINGS_DETAIL_CONTENT_INSET_X_DESIGN);
    }

    public static float settingsDetailPaddedInnerWidth(float settingsInnerWidth, float leftInsetDesign, float rightInsetDesign) {
        return Math.max(14f, settingsInnerWidth - leftInsetDesign - rightInsetDesign);
    }

    public static FWidget wrapSettingsDetailRowInShellMargin(float settingsContentWidth, float settingsInnerWidth, FWidget inner) {
        return wrapSettingsDetailRowInShellMargin(settingsContentWidth, settingsInnerWidth, inner, SettingsUiMetrics.SETTINGS_DETAIL_CONTENT_INSET_X_DESIGN, SettingsUiMetrics.SETTINGS_DETAIL_CONTENT_INSET_X_DESIGN);
    }

    public static FWidget wrapSettingsDetailRowInShellMargin(float settingsContentWidth, float settingsInnerWidth, FWidget inner, float leftInsetDesign, float rightInsetDesign) {
        if (leftInsetDesign <= 0f && rightInsetDesign <= 0f) {
            return new FMinWidthHostWidget(settingsContentWidth, inner);
        }
        float paddedInnerWidth = settingsDetailPaddedInnerWidth(settingsInnerWidth, leftInsetDesign, rightInsetDesign);
        FRowWidget row = new FRowWidget(0f, Align.START) {
            @Override
            public boolean fillsHorizontalInRow() {
                return true;
            }
        };
        row.addChild(new FSpacerWidget(leftInsetDesign, 0f));
        FMinWidthHostWidget innerHost = new FMinWidthHostWidget(paddedInnerWidth, inner);
        innerHost.setCellConstraints(new FCellConstraints().setExpandHorizontal(true));
        row.addChild(innerHost);
        row.addChild(new FSpacerWidget(rightInsetDesign, 0f));
        return new FMinWidthHostWidget(settingsContentWidth, row);
    }

    public static boolean isRenderableSetting(Setting<?> setting) {
        return setting instanceof BooleanSetting || setting instanceof SliderSetting || setting instanceof KeybindSetting || setting instanceof EnumSetting<?> || setting instanceof ColorSetting;
    }

    /**
     * Appends top-level settings, then each category block, batching consecutive {@link BooleanSetting} entries into a
     * two-column grid when at least two appear in a row.
     *
     * @param scrollBody             scroll column receiving rows
     * @param settingsContentWidth   full shell content width for horizontal spacers
     * @param settingsInnerWidth     inner width before shell horizontal inset
     * @param categories             category blocks after top-level settings
     * @param topLevelSettings       module/widget settings registered outside categories
     * @param editorFactory          editor for a single setting row
     */
    /**
     * Declarative equivalent of {@link #appendTopLevelThenCategories}; each row gets a keyed {@link UiSlot} for stable reconcile.
     */
    public static List<UiSlot> declarativeSlotsTopLevelThenCategories(float settingsContentWidth, float settingsInnerWidth, List<CategoryBlock> categories, List<Setting<?>> topLevelSettings, SettingRowEditorFactory editorFactory) {
        List<UiSlot> slots = new ArrayList<>();
        List<Setting<?>> allForMeasure = new ArrayList<>(topLevelSettings);
        for (CategoryBlock category : categories) {
            allForMeasure.addAll(category.settings());
        }
        float sliderValueColumnStartX = computeValueColumnStartX(allForMeasure);
        float settingRowsPaddedInnerWidth = settingsDetailPaddedInnerWidth(settingsInnerWidth, SettingsUiMetrics.SETTINGS_DETAIL_CATEGORY_CONTENT_LEFT_INSET_X_DESIGN, SettingsUiMetrics.SETTINGS_DETAIL_CATEGORY_CONTENT_RIGHT_INSET_X_DESIGN);
        float categoryTitlePaddedInnerWidth = settingsDetailPaddedInnerWidth(settingsInnerWidth, SettingsUiMetrics.SETTINGS_DETAIL_CATEGORY_TITLE_LEFT_INSET_X_DESIGN, SettingsUiMetrics.SETTINGS_DETAIL_CATEGORY_CONTENT_RIGHT_INSET_X_DESIGN);
        boolean[] placedAnyRow = {false};
        declarativeAppendSequencedSettings(slots, settingsContentWidth, settingsInnerWidth, settingRowsPaddedInnerWidth, sliderValueColumnStartX, topLevelSettings, editorFactory, placedAnyRow);
        for (CategoryBlock category : categories) {
            if (!categoryBlockHasRenderableSetting(category)) {
                continue;
            }
            if (placedAnyRow[0]) {
                slots.add(UiSlot.of(Ui.spacer(settingsContentWidth, SettingsUiMetrics.CATEGORY_SECTION_GAP)));
            }
            if (categoryDisplaysSectionHeader(category.displayNameKey())) {
                FLabelWidget categoryLabel = new FLabelWidget();
                categoryLabel.setText(I18n.get(category.displayNameKey()));
                categoryLabel.setColorArgb(FascinatedGuiTheme.INSTANCE.sectionHeaderText());
                categoryLabel.setAlignX(Align.START);
                FWidget titleHost = wrapSettingsDetailRowInShellMargin(settingsContentWidth, settingsInnerWidth, new FMinWidthHostWidget(categoryTitlePaddedInnerWidth, categoryLabel), SettingsUiMetrics.SETTINGS_DETAIL_CATEGORY_TITLE_LEFT_INSET_X_DESIGN, SettingsUiMetrics.SETTINGS_DETAIL_CATEGORY_CONTENT_RIGHT_INSET_X_DESIGN);
                slots.add(Ui.slot(Ui.widgetSlot("catTitle:" + category.displayNameKey(), titleHost)));
                slots.add(UiSlot.of(Ui.spacer(settingsContentWidth, SettingsUiMetrics.CATEGORY_AFTER_HEADER_ROW_GAP)));
            }
            declarativeAppendSequencedSettings(slots, settingsContentWidth, settingsInnerWidth, settingRowsPaddedInnerWidth, sliderValueColumnStartX, category.settings(), editorFactory, placedAnyRow);
        }
        return slots;
    }

    public static void appendTopLevelThenCategories(FColumnWidget scrollBody, float settingsContentWidth, float settingsInnerWidth, List<CategoryBlock> categories, List<Setting<?>> topLevelSettings, SettingRowEditorFactory editorFactory) {
        List<Setting<?>> allForMeasure = new ArrayList<>(topLevelSettings);
        for (CategoryBlock category : categories) {
            allForMeasure.addAll(category.settings());
        }
        float sliderValueColumnStartX = computeValueColumnStartX(allForMeasure);
        float settingRowsPaddedInnerWidth = settingsDetailPaddedInnerWidth(settingsInnerWidth, SettingsUiMetrics.SETTINGS_DETAIL_CATEGORY_CONTENT_LEFT_INSET_X_DESIGN, SettingsUiMetrics.SETTINGS_DETAIL_CATEGORY_CONTENT_RIGHT_INSET_X_DESIGN);
        float categoryTitlePaddedInnerWidth = settingsDetailPaddedInnerWidth(settingsInnerWidth, SettingsUiMetrics.SETTINGS_DETAIL_CATEGORY_TITLE_LEFT_INSET_X_DESIGN, SettingsUiMetrics.SETTINGS_DETAIL_CATEGORY_CONTENT_RIGHT_INSET_X_DESIGN);
        boolean[] placedAnyRow = {false};
        appendSequencedSettings(scrollBody, settingsContentWidth, settingsInnerWidth, settingRowsPaddedInnerWidth, sliderValueColumnStartX, topLevelSettings, editorFactory, placedAnyRow);
        for (CategoryBlock category : categories) {
            if (!categoryBlockHasRenderableSetting(category)) {
                continue;
            }
            if (placedAnyRow[0]) {
                scrollBody.addChild(new FSpacerWidget(settingsContentWidth, SettingsUiMetrics.CATEGORY_SECTION_GAP));
            }
            if (categoryDisplaysSectionHeader(category.displayNameKey())) {
                FLabelWidget categoryLabel = new FLabelWidget();
                categoryLabel.setText(I18n.get(category.displayNameKey()));
                categoryLabel.setColorArgb(FascinatedGuiTheme.INSTANCE.sectionHeaderText());
                categoryLabel.setAlignX(Align.START);
                scrollBody.addChild(wrapSettingsDetailRowInShellMargin(settingsContentWidth, settingsInnerWidth, new FMinWidthHostWidget(categoryTitlePaddedInnerWidth, categoryLabel), SettingsUiMetrics.SETTINGS_DETAIL_CATEGORY_TITLE_LEFT_INSET_X_DESIGN, SettingsUiMetrics.SETTINGS_DETAIL_CATEGORY_CONTENT_RIGHT_INSET_X_DESIGN));
                scrollBody.addChild(new FSpacerWidget(settingsContentWidth, SettingsUiMetrics.CATEGORY_AFTER_HEADER_ROW_GAP));
            }
            appendSequencedSettings(scrollBody, settingsContentWidth, settingsInnerWidth, settingRowsPaddedInnerWidth, sliderValueColumnStartX, category.settings(), editorFactory, placedAnyRow);
        }
    }

    public static float computeValueColumnStartX(List<Setting<?>> settings) {
        float maxLabelWidth = 17f;
        for (Setting<?> setting : settings) {
            String labelText = setting.getName();
            maxLabelWidth = Math.max(maxLabelWidth, measureSettingLabelWidth(labelText));
            for (Setting<?> subSetting : setting.getSubSettings()) {
                String subLabel = subSetting.getName();
                maxLabelWidth = Math.max(maxLabelWidth, measureSettingLabelWidth(subLabel));
            }
        }
        float baseOffset = 35f;
        return maxLabelWidth + baseOffset;
    }

    private static void declarativeAppendSequencedSettings(List<UiSlot> slots, float settingsContentWidth, float settingsInnerWidth, float widthForEditors, float sliderValueColumnStartX, List<Setting<?>> sequence, SettingRowEditorFactory editorFactory, boolean[] placedAnyRow) {
        for (Setting<?> setting : sequence) {
            if (setting.isSubSetting() || !isRenderableSetting(setting)) {
                continue;
            }
            FWidget editor = editorFactory.create(setting, widthForEditors, sliderValueColumnStartX);
            declarativeAppendSingleWrappedEditor(slots, settingsContentWidth, settingsInnerWidth, editor, placedAnyRow, setting.getSettingKey());
        }
    }

    private static void declarativeAppendSingleWrappedEditor(List<UiSlot> slots, float settingsContentWidth, float settingsInnerWidth, @Nullable FWidget editor, boolean[] placedAnyRow, String reconcileKeySuffix) {
        if (editor == null) {
            return;
        }
        if (placedAnyRow[0]) {
            slots.add(UiSlot.of(Ui.spacer(settingsContentWidth, SettingsUiMetrics.SETTING_GROUP_GAP)));
        }
        placedAnyRow[0] = true;
        FWidget wrappedRow = wrapSettingRowShellMargin(settingsContentWidth, settingsInnerWidth, editor);
        slots.add(Ui.slot(Ui.widgetSlot("setting:" + reconcileKeySuffix, wrappedRow)));
    }

    private static void appendSequencedSettings(FColumnWidget scrollBody, float settingsContentWidth, float settingsInnerWidth, float widthForEditors, float sliderValueColumnStartX, List<Setting<?>> sequence, SettingRowEditorFactory editorFactory, boolean[] placedAnyRow) {
        for (Setting<?> setting : sequence) {
            if (setting.isSubSetting() || !isRenderableSetting(setting)) {
                continue;
            }
            FWidget editor = editorFactory.create(setting, widthForEditors, sliderValueColumnStartX);
            appendSingleWrappedEditor(scrollBody, settingsContentWidth, settingsInnerWidth, editor, placedAnyRow);
        }
    }

    private static void appendSingleWrappedEditor(FColumnWidget scrollBody, float settingsContentWidth, float settingsInnerWidth, @Nullable FWidget editor, boolean[] placedAnyRow) {
        if (editor == null) {
            return;
        }
        if (placedAnyRow[0]) {
            scrollBody.addChild(new FSpacerWidget(settingsContentWidth, SettingsUiMetrics.SETTING_GROUP_GAP));
        }
        placedAnyRow[0] = true;
        scrollBody.addChild(wrapSettingRowShellMargin(settingsContentWidth, settingsInnerWidth, editor));
    }

    private static FWidget wrapSettingRowShellMargin(float settingsContentWidth, float settingsInnerWidth, FWidget inner) {
        return wrapSettingsDetailRowInShellMargin(settingsContentWidth, settingsInnerWidth, inner, SettingsUiMetrics.SETTINGS_DETAIL_CATEGORY_CONTENT_LEFT_INSET_X_DESIGN, SettingsUiMetrics.SETTINGS_DETAIL_CATEGORY_CONTENT_RIGHT_INSET_X_DESIGN);
    }

    private static boolean categoryDisplaysSectionHeader(String displayNameKey) {
        if (displayNameKey == null || displayNameKey.isBlank()) {
            return false;
        }
        String resolved = I18n.get(displayNameKey);
        return !resolved.isBlank();
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
        return minecraftClient.font.width(labelText);
    }

    @FunctionalInterface
    public interface SettingRowEditorFactory {
        @Nullable FWidget create(Setting<?> setting, float innerWidth, float sliderValueColumnStartX);
    }

    public record CategoryBlock(String displayNameKey, List<Setting<?>> settings) {}
}
