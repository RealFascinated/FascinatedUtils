package cc.fascinated.fascinatedutils.gui.modsettings;

import cc.fascinated.fascinatedutils.common.setting.Setting;
import cc.fascinated.fascinatedutils.common.setting.impl.*;
import cc.fascinated.fascinatedutils.gui.GuiDesignSpace;
import cc.fascinated.fascinatedutils.gui.core.Align;
import cc.fascinated.fascinatedutils.gui.theme.SettingsUiMetrics;
import cc.fascinated.fascinatedutils.gui.theme.UITheme;
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

    public static final float SETTINGS_SCROLL_BOTTOM_INSET = UITheme.PADDING_XS;

    public static float settingsDetailPaddedInnerWidth(float settingsInnerWidth) {
        float horizontalInset = GuiDesignSpace.pxX(SettingsUiMetrics.SETTINGS_DETAIL_CONTENT_INSET_X_DESIGN);
        return Math.max(GuiDesignSpace.pxX(20f), settingsInnerWidth - 2f * horizontalInset);
    }

    public static FWidget wrapSettingsDetailRowInShellMargin(float settingsContentWidth, float settingsInnerWidth, FWidget inner) {
        float horizontalInset = GuiDesignSpace.pxX(SettingsUiMetrics.SETTINGS_DETAIL_CONTENT_INSET_X_DESIGN);
        if (horizontalInset <= 0f) {
            return new FMinWidthHostWidget(settingsContentWidth, inner);
        }
        float paddedInnerWidth = settingsDetailPaddedInnerWidth(settingsInnerWidth);
        FRowWidget row = new FRowWidget(0f, Align.START) {
            @Override
            public boolean fillsHorizontalInRow() {
                return true;
            }
        };
        row.addChild(new FSpacerWidget(horizontalInset, 0f));
        FMinWidthHostWidget innerHost = new FMinWidthHostWidget(paddedInnerWidth, inner);
        innerHost.setCellConstraints(new FCellConstraints().setExpandHorizontal(true));
        row.addChild(innerHost);
        row.addChild(new FSpacerWidget(horizontalInset, 0f));
        return new FMinWidthHostWidget(settingsContentWidth, row);
    }

    public static boolean isRenderableSetting(Setting<?> setting) {
        return setting instanceof BooleanSetting || setting instanceof SliderSetting || setting instanceof KeybindSetting || setting instanceof EnumSetting<?> || setting instanceof ColorSetting;
    }

    public static void appendTopLevelThenCategories(FColumnWidget scrollBody, float settingsContentWidth, float settingsInnerWidth, List<CategoryBlock> categories, List<Setting<?>> topLevelSettings, SettingRowEditorFactory editorFactory) {
        List<Setting<?>> allForMeasure = new ArrayList<>(topLevelSettings);
        for (CategoryBlock category : categories) {
            allForMeasure.addAll(category.settings());
        }
        float sliderValueColumnStartX = computeValueColumnStartX(allForMeasure);
        float paddedInnerWidth = settingsDetailPaddedInnerWidth(settingsInnerWidth);
        float categoryInsetPx = GuiDesignSpace.pxX(SettingsUiMetrics.MODULE_SETTING_CATEGORY_INDENT_X_DESIGN);
        float insetBodyWidth = Math.max(GuiDesignSpace.pxX(20f), paddedInnerWidth - categoryInsetPx);
        boolean placedAnyRow = false;
        for (Setting<?> setting : topLevelSettings) {
            FWidget editor = editorFactory.create(setting, paddedInnerWidth, sliderValueColumnStartX);
            if (editor == null) {
                continue;
            }
            if (placedAnyRow) {
                scrollBody.addChild(new FSpacerWidget(settingsContentWidth, GuiDesignSpace.pxY(SettingsUiMetrics.SETTING_GROUP_GAP)));
            }
            placedAnyRow = true;
            scrollBody.addChild(wrapSettingsDetailRowInShellMargin(settingsContentWidth, settingsInnerWidth, editor));
        }
        for (CategoryBlock category : categories) {
            if (!categoryBlockHasRenderableSetting(category)) {
                continue;
            }
            if (placedAnyRow) {
                scrollBody.addChild(new FSpacerWidget(settingsContentWidth, GuiDesignSpace.pxY(SettingsUiMetrics.CATEGORY_SECTION_GAP)));
            }
            FLabelWidget categoryLabel = new FLabelWidget();
            categoryLabel.setText(I18n.get(category.displayNameKey()));
            categoryLabel.setColorArgb(FascinatedGuiTheme.INSTANCE.sectionHeaderText());
            categoryLabel.setAlignX(Align.START);
            scrollBody.addChild(wrapSettingsDetailRowInShellMargin(settingsContentWidth, settingsInnerWidth, new FMinWidthHostWidget(paddedInnerWidth, categoryLabel)));
            scrollBody.addChild(new FSpacerWidget(settingsContentWidth, GuiDesignSpace.pxY(UITheme.GAP_XS)));
            for (Setting<?> setting : category.settings()) {
                FWidget editor = editorFactory.create(setting, insetBodyWidth, sliderValueColumnStartX);
                if (editor == null) {
                    continue;
                }
                if (placedAnyRow) {
                    scrollBody.addChild(new FSpacerWidget(settingsContentWidth, GuiDesignSpace.pxY(SettingsUiMetrics.SETTING_GROUP_GAP)));
                }
                placedAnyRow = true;
                scrollBody.addChild(wrapSettingsDetailRowInShellMargin(settingsContentWidth, settingsInnerWidth, insetCategoryEditorRow(categoryInsetPx, editor)));
            }
        }
    }

    public static float computeValueColumnStartX(List<Setting<?>> settings) {
        float maxLabelWidth = GuiDesignSpace.pxX(24f);
        for (Setting<?> setting : settings) {
            String labelText = setting.getTranslatedDisplayName();
            maxLabelWidth = Math.max(maxLabelWidth, measureSettingLabelWidth(labelText));
        }
        float baseOffset = GuiDesignSpace.pxX(50f);
        return maxLabelWidth + baseOffset;
    }

    private static boolean categoryBlockHasRenderableSetting(CategoryBlock category) {
        for (Setting<?> setting : category.settings()) {
            if (isRenderableSetting(setting)) {
                return true;
            }
        }
        return false;
    }

    private static FWidget insetCategoryEditorRow(float categoryInsetPx, FWidget editor) {
        FRowWidget row = new FRowWidget(0f, Align.START);
        row.addChild(new FSpacerWidget(categoryInsetPx, 0f));
        editor.setCellConstraints(new FCellConstraints().setExpandHorizontal(true));
        row.addChild(editor);
        return row;
    }

    private static float measureSettingLabelWidth(String labelText) {
        if (labelText == null || labelText.isEmpty()) {
            return 0f;
        }
        Minecraft minecraftClient = Minecraft.getInstance();
        if (minecraftClient == null) {
            return GuiDesignSpace.pxX(24f);
        }
        return GuiDesignSpace.pxX(minecraftClient.font.width(labelText));
    }

    @FunctionalInterface
    public interface SettingRowEditorFactory {
        @Nullable FWidget create(Setting<?> setting, float innerWidth, float sliderValueColumnStartX);
    }

    public record CategoryBlock(String displayNameKey, List<Setting<?>> settings) {}
}
