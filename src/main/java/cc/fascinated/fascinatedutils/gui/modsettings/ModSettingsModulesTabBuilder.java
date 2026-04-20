package cc.fascinated.fascinatedutils.gui.modsettings;

import cc.fascinated.fascinatedutils.common.setting.impl.ColorSetting;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

import cc.fascinated.fascinatedutils.gui.core.Align;
import cc.fascinated.fascinatedutils.gui.core.Callback;
import cc.fascinated.fascinatedutils.gui.core.GuiFocusState;
import cc.fascinated.fascinatedutils.gui.core.Ref;
import cc.fascinated.fascinatedutils.gui.renderer.UIRenderer;
import cc.fascinated.fascinatedutils.gui.theme.SettingsUiMetrics;
import cc.fascinated.fascinatedutils.gui.themes.FascinatedGuiTheme;
import cc.fascinated.fascinatedutils.gui.widgets.FButtonWidget;
import cc.fascinated.fascinatedutils.gui.widgets.FCellConstraints;
import cc.fascinated.fascinatedutils.gui.widgets.FColumnWidget;
import cc.fascinated.fascinatedutils.gui.widgets.FLabelWidget;
import cc.fascinated.fascinatedutils.gui.widgets.FMinWidthHostWidget;
import cc.fascinated.fascinatedutils.gui.widgets.FOutlinedTextInputWidget;
import cc.fascinated.fascinatedutils.gui.widgets.FRowWidget;
import cc.fascinated.fascinatedutils.gui.widgets.FScrollColumnWidget;
import cc.fascinated.fascinatedutils.gui.widgets.FSpacerWidget;
import cc.fascinated.fascinatedutils.gui.widgets.FTheme;
import cc.fascinated.fascinatedutils.gui.widgets.FWidget;
import cc.fascinated.fascinatedutils.gui.widgets.SelectableButtonWidget;
import cc.fascinated.fascinatedutils.systems.config.ModConfig;
import cc.fascinated.fascinatedutils.systems.modules.Module;
import cc.fascinated.fascinatedutils.systems.modules.ModuleCategory;
import cc.fascinated.fascinatedutils.systems.modules.ModuleRegistry;
import net.minecraft.network.chat.Component;

public class ModSettingsModulesTabBuilder {

    private static final float GRID_MIN_CELL_WIDTH_DESIGN = 90f;
    private static final float GRID_MARGIN_X_DESIGN = 8f;
    private static final int MODULE_CARD_GRID_MAX_COLUMNS = 3;
    private static final int MODULES_SEARCH_FOCUS_ID = 5001;

    public static FWidget buildModulesTab(float paneWidth, float paneHeight, List<Module> modules, Ref<Float> modulesGridScrollYRef, Module settingsModule, Runnable onBackFromModuleSettings, Callback<Module> onOpenModuleSettings, Ref<Float> moduleSettingsScrollYRef, Ref<String> moduleSearchRef, Ref<ModuleCategory> moduleCategoryFilterRef, Runnable onFiltersChanged, Consumer<ColorSetting> openColorPicker) {
        if (settingsModule != null) {
            return ModSettingsModuleDetailBuilder.buildModuleSettingsDetail(paneWidth, paneHeight, settingsModule, onBackFromModuleSettings, moduleSettingsScrollYRef, openColorPicker);
        }
        return buildModuleCardGrid(paneWidth, paneHeight, modules, modulesGridScrollYRef, onOpenModuleSettings, moduleSearchRef, moduleCategoryFilterRef, onFiltersChanged);
    }

    private static FWidget buildModuleCardGrid(float paneWidth, float paneHeight, List<Module> modules, Ref<Float> modulesGridScrollYRef, Callback<Module> onOpenModuleSettings, Ref<String> moduleSearchRef, Ref<ModuleCategory> moduleCategoryFilterRef, Runnable onFiltersChanged) {
        float paddedInnerWidth = Math.max(0f, paneWidth - 2f * GRID_MARGIN_X_DESIGN);
        float settingsContentWidth = Math.max(28f, paddedInnerWidth);
        float gapY = 6f;
        float gapX = 3f;
        float controlsHeight = SettingsUiMetrics.SHELL_CONTROL_HEIGHT_DESIGN;
        int columnCount = computeCardGridColumnCount(settingsContentWidth, gapX, GRID_MIN_CELL_WIDTH_DESIGN);
        float cellWidth = (settingsContentWidth - gapX * Math.max(0, columnCount - 1)) / Math.max(1, columnCount);
        float cellHeight = FModuleVisibilityCardWidget.stackedCellOuterHeightPx();

        FOutlinedTextInputWidget searchInput = new FOutlinedTextInputWidget(MODULES_SEARCH_FOCUS_ID, 180, SettingsUiMetrics.SHELL_CONTROL_HEIGHT_DESIGN, () -> "Search modules...");
        String currentSearch = moduleSearchRef.getValue();
        searchInput.setValue(currentSearch == null ? "" : currentSearch);
        searchInput.setOnChange(text -> {
            moduleSearchRef.setValue(text);
            onFiltersChanged.run();
        });
        searchInput.setExternalFocusIdSupplier(GuiFocusState::getFocusedId);

        FRowWidget controlsRow = new FRowWidget(3f, Align.CENTER) {
            @Override
            public boolean fillsHorizontalInRow() {
                return true;
            }
        };
        FRowWidget categoryButtonsRow = new FRowWidget(2f, Align.CENTER) {
            @Override
            public float intrinsicHeightForColumn(UIRenderer measure, float widthBudget) {
                return controlsHeight;
            }
        };
        categoryButtonsRow.setCellConstraints(new FCellConstraints().setExpandVertical(true));
        FButtonWidget allCategoriesButton = new SelectableButtonWidget(() -> {
            moduleCategoryFilterRef.setValue(null);
            onFiltersChanged.run();
        }, () -> "All", 62f, 1, 1f, 6f, 1.12f, 7f, 2f, () -> moduleCategoryFilterRef.getValue() == null) {
            @Override
            public float intrinsicHeightForColumn(UIRenderer measure, float widthBudget) {
                return controlsHeight;
            }
        };
        allCategoriesButton.setCellConstraints(new FCellConstraints().setExpandVertical(true));
        categoryButtonsRow.addChild(allCategoriesButton);
        for (ModuleCategory moduleCategory : ModuleCategory.values()) {
            FButtonWidget categoryButton = new SelectableButtonWidget(() -> {
                moduleCategoryFilterRef.setValue(moduleCategory);
                onFiltersChanged.run();
            }, moduleCategory::getDisplayName, 62f, 1, 1f, 6f, 1.12f, 7f, 2f, () -> moduleCategory == moduleCategoryFilterRef.getValue()) {
                @Override
                public float intrinsicHeightForColumn(UIRenderer measure, float widthBudget) {
                    return controlsHeight;
                }
            };
            categoryButton.setCellConstraints(new FCellConstraints().setExpandVertical(true));
            categoryButtonsRow.addChild(categoryButton);
        }
        controlsRow.addChild(categoryButtonsRow);

        FSpacerWidget rightPushSpacer = new FSpacerWidget(0f, 0f);
        rightPushSpacer.setCellConstraints(new FCellConstraints().setExpandHorizontal(true));
        controlsRow.addChild(rightPushSpacer);

        FMinWidthHostWidget searchHost = new FMinWidthHostWidget(210f, searchInput);
        controlsRow.addChild(searchHost);

        float horizontalInset = SettingsUiMetrics.SETTINGS_DETAIL_CONTENT_INSET_X_DESIGN;
        float controlsInnerWidth = Math.max(14f, settingsContentWidth - 2f * horizontalInset);
        FRowWidget paddedControlsRow = new FRowWidget(0f, Align.START) {
            @Override
            public boolean fillsHorizontalInRow() {
                return true;
            }
        };
        paddedControlsRow.addChild(new FSpacerWidget(horizontalInset, 0f));
        FMinWidthHostWidget controlsInnerHost = new FMinWidthHostWidget(controlsInnerWidth, controlsRow);
        controlsInnerHost.setCellConstraints(new FCellConstraints().setExpandHorizontal(true));
        paddedControlsRow.addChild(controlsInnerHost);
        paddedControlsRow.addChild(new FSpacerWidget(horizontalInset, 0f));
        FWidget controlsRowHost = new FMinWidthHostWidget(settingsContentWidth, paddedControlsRow);

        String searchLower = (moduleSearchRef.getValue() == null ? "" : moduleSearchRef.getValue()).toLowerCase(Locale.ROOT);
        ModuleCategory selectedCategory = moduleCategoryFilterRef.getValue();
        List<Module> filteredModules = modules == null ? new ArrayList<>() : modules.stream().filter(module -> selectedCategory == null || module.getCategory() == selectedCategory).filter(module -> module.getDisplayName().toLowerCase(Locale.ROOT).contains(searchLower)).toList();

        FColumnWidget scrollBody = new FColumnWidget(gapY, Align.CENTER);

        if (filteredModules.isEmpty()) {
            scrollBody.addChild(new FSpacerWidget(settingsContentWidth, 2f));
            FLabelWidget empty = new FLabelWidget();
            empty.setText(Component.translatable("fascinatedutils.setting.shell.empty_modules").getString());
            empty.setColorArgb(FascinatedGuiTheme.INSTANCE.textMuted());
            empty.setAlignX(Align.CENTER);
            scrollBody.addChild(empty);
            scrollBody.addChild(new FSpacerWidget(settingsContentWidth, 4f));
            return buildModulesPaneLayout(controlsRowHost, createModulesGridScrollClip(scrollBody, gapY, modulesGridScrollYRef));
        }
        List<Module> sortedModules = new ArrayList<>(filteredModules);
        sortedModules.sort((left, right) -> String.CASE_INSENSITIVE_ORDER.compare(left.getDisplayName().toLowerCase(Locale.ROOT), right.getDisplayName().toLowerCase(Locale.ROOT)));
        for (int moduleIndex = 0; moduleIndex < sortedModules.size(); moduleIndex += columnCount) {
            FRowWidget row = new FRowWidget(gapX, Align.START);
            for (int columnIndex = 0; columnIndex < columnCount; columnIndex++) {
                int index = moduleIndex + columnIndex;
                if (index >= sortedModules.size()) {
                    break;
                }
                Module module = sortedModules.get(index);
                row.addChild(FTheme.components().createModuleVisibilityCard(module, cellWidth, cellHeight, onOpenModuleSettings, enabled -> {
                    ModuleRegistry.INSTANCE.setModuleEnabled(module, enabled);
                    ModConfig.saveActiveModule(module);
                }));
            }
            scrollBody.addChild(row);
        }
        scrollBody.addChild(new FSpacerWidget(settingsContentWidth, 4f));
        return buildModulesPaneLayout(controlsRowHost, createModulesGridScrollClip(scrollBody, gapY, modulesGridScrollYRef));
    }

    private static FWidget buildModulesPaneLayout(FWidget controlsRowHost, FScrollColumnWidget modulesScrollClip) {
        return new FModulesPaneLayoutWidget(controlsRowHost, modulesScrollClip);
    }

    private static FScrollColumnWidget createModulesGridScrollClip(FColumnWidget scrollBody, float gapY, Ref<Float> modulesGridScrollYRef) {
        FScrollColumnWidget clip = FTheme.components().createScrollColumn(scrollBody, gapY);
        clip.setFillVerticalInColumn(true);
        if (modulesGridScrollYRef != null) {
            Float scrollOffsetY = modulesGridScrollYRef.getValue();
            clip.setScrollOffsetY(scrollOffsetY == null ? 0f : scrollOffsetY);
            clip.setScrollOffsetChangeListener(modulesGridScrollYRef::setValue);
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
        return Math.min(MODULE_CARD_GRID_MAX_COLUMNS, chosenColumns);
    }
}
