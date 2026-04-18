package cc.fascinated.fascinatedutils.gui.modsettings;

import cc.fascinated.fascinatedutils.gui.GuiDesignSpace;
import cc.fascinated.fascinatedutils.gui.core.Align;
import cc.fascinated.fascinatedutils.gui.core.Callback;
import cc.fascinated.fascinatedutils.gui.core.GuiFocusState;
import cc.fascinated.fascinatedutils.gui.core.Ref;
import cc.fascinated.fascinatedutils.gui.renderer.UIRenderer;
import cc.fascinated.fascinatedutils.gui.theme.SettingsUiMetrics;
import cc.fascinated.fascinatedutils.gui.theme.UITheme;
import cc.fascinated.fascinatedutils.gui.themes.fascinated.FascinatedGuiTheme;
import cc.fascinated.fascinatedutils.gui.widgets.*;
import cc.fascinated.fascinatedutils.gui.widgets.settings.FModuleVisibilityCardWidget;
import cc.fascinated.fascinatedutils.systems.config.ModConfig;
import cc.fascinated.fascinatedutils.systems.modules.Module;
import cc.fascinated.fascinatedutils.systems.modules.ModuleCategory;
import cc.fascinated.fascinatedutils.systems.modules.ModuleRegistry;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ModSettingsModulesTabBuilder {

    private static final float GRID_MIN_CELL_WIDTH_DESIGN = 128f;
    private static final float GRID_MARGIN_X_DESIGN = 12f;
    private static final int MODULES_SEARCH_FOCUS_ID = 5001;

    public static FWidget buildModulesTab(float paneWidth, float paneHeight, List<Module> modules, Ref<Float> modulesGridScrollYRef, Module settingsModule, Runnable onBackFromModuleSettings, Callback<Module> onOpenModuleSettings, Ref<Float> moduleSettingsScrollYRef, Ref<String> moduleSearchRef, Ref<ModuleCategory> moduleCategoryFilterRef, Runnable onFiltersChanged) {
        if (settingsModule != null) {
            return ModSettingsModuleDetailBuilder.buildModuleSettingsDetail(paneWidth, paneHeight, settingsModule, onBackFromModuleSettings, moduleSettingsScrollYRef);
        }
        return buildModuleCardGrid(paneWidth, paneHeight, modules, modulesGridScrollYRef, onOpenModuleSettings, moduleSearchRef, moduleCategoryFilterRef, onFiltersChanged);
    }

    private static FWidget buildModuleCardGrid(float paneWidth, float paneHeight, List<Module> modules, Ref<Float> modulesGridScrollYRef, Callback<Module> onOpenModuleSettings, Ref<String> moduleSearchRef, Ref<ModuleCategory> moduleCategoryFilterRef, Runnable onFiltersChanged) {
        float gridMarginX = GuiDesignSpace.pxX(GRID_MARGIN_X_DESIGN);
        float paddedInnerWidth = Math.max(0f, paneWidth - 2f * gridMarginX);
        float settingsContentWidth = Math.max(GuiDesignSpace.pxX(40f), paddedInnerWidth);
        float settingsInnerWidth = settingsContentWidth;
        float gapY = GuiDesignSpace.pxY(UITheme.GAP_MD);
        float gapX = GuiDesignSpace.pxX(UITheme.GAP_SM);
        float minCellWidth = GuiDesignSpace.pxX(GRID_MIN_CELL_WIDTH_DESIGN);
        float controlsHeight = GuiDesignSpace.pxY(SettingsUiMetrics.SHELL_CONTROL_HEIGHT_DESIGN);
        int columnCount = computeCardGridColumnCount(settingsContentWidth, gapX, minCellWidth);
        float cellWidth = (settingsContentWidth - gapX * Math.max(0, columnCount - 1)) / Math.max(1, columnCount);
        float cellHeight = FModuleVisibilityCardWidget.stackedCellOuterHeightPx();

        FOutlinedTextInputWidget searchInput = new FOutlinedTextInputWidget(MODULES_SEARCH_FOCUS_ID, 256, SettingsUiMetrics.SHELL_CONTROL_HEIGHT_DESIGN, () -> "Search modules...");
        String currentSearch = moduleSearchRef.getValue();
        searchInput.setValue(currentSearch == null ? "" : currentSearch);
        searchInput.setOnChange(text -> {
            moduleSearchRef.setValue(text);
            onFiltersChanged.run();
        });
        searchInput.setExternalFocusIdSupplier(GuiFocusState::getFocusedId);

        FRowWidget controlsRow = new FRowWidget(GuiDesignSpace.pxX(UITheme.GAP_SM), Align.CENTER) {
            @Override
            public boolean fillsHorizontalInRow() {
                return true;
            }
        };
        FRowWidget categoryButtonsRow = new FRowWidget(GuiDesignSpace.pxX(UITheme.GAP_XS), Align.CENTER) {
            @Override
            public float intrinsicHeightForColumn(UIRenderer measure, float widthBudget) {
                return controlsHeight;
            }
        };
        categoryButtonsRow.setCellConstraints(new FCellConstraints().setExpandVertical(true));
        FButtonWidget allCategoriesButton = new FButtonWidget(() -> {
            moduleCategoryFilterRef.setValue(null);
            onFiltersChanged.run();
        }, () -> "All", GuiDesignSpace.pxX(88f), 1, 2f, 8f, 1.12f, 10f, 3f) {
            @Override
            public float intrinsicHeightForColumn(UIRenderer measure, float widthBudget) {
                return controlsHeight;
            }

            @Override
            protected int resolveButtonFillColorArgb(boolean hovered) {
                boolean activeCategory = moduleCategoryFilterRef.getValue() == null;
                if (activeCategory) {
                    return hovered ? FascinatedGuiTheme.INSTANCE.moduleListRowHover() : FascinatedGuiTheme.INSTANCE.moduleListRowSelected();
                }
                return hovered ? FascinatedGuiTheme.INSTANCE.moduleListRowHover() : FascinatedGuiTheme.INSTANCE.moduleListRow();
            }

            @Override
            protected int resolveButtonBorderColorArgb(boolean hovered) {
                boolean activeCategory = moduleCategoryFilterRef.getValue() == null;
                if (activeCategory) {
                    return hovered ? FascinatedGuiTheme.INSTANCE.borderHover() : FascinatedGuiTheme.INSTANCE.borderMuted();
                }
                return super.resolveButtonBorderColorArgb(hovered);
            }
        };
        allCategoriesButton.setCellConstraints(new FCellConstraints().setExpandVertical(true));
        categoryButtonsRow.addChild(allCategoriesButton);
        for (ModuleCategory moduleCategory : ModuleCategory.values()) {
            FButtonWidget categoryButton = new FButtonWidget(() -> {
                moduleCategoryFilterRef.setValue(moduleCategory);
                onFiltersChanged.run();
            }, moduleCategory::getDisplayName, GuiDesignSpace.pxX(88f), 1, 2f, 8f, 1.12f, 10f, 3f) {
                @Override
                public float intrinsicHeightForColumn(UIRenderer measure, float widthBudget) {
                    return controlsHeight;
                }

                @Override
                protected int resolveButtonFillColorArgb(boolean hovered) {
                    boolean activeCategory = moduleCategory == moduleCategoryFilterRef.getValue();
                    if (activeCategory) {
                        return hovered ? FascinatedGuiTheme.INSTANCE.moduleListRowHover() : FascinatedGuiTheme.INSTANCE.moduleListRowSelected();
                    }
                    return hovered ? FascinatedGuiTheme.INSTANCE.moduleListRowHover() : FascinatedGuiTheme.INSTANCE.moduleListRow();
                }

                @Override
                protected int resolveButtonBorderColorArgb(boolean hovered) {
                    boolean activeCategory = moduleCategory == moduleCategoryFilterRef.getValue();
                    if (activeCategory) {
                        return hovered ? FascinatedGuiTheme.INSTANCE.borderHover() : FascinatedGuiTheme.INSTANCE.borderMuted();
                    }
                    return super.resolveButtonBorderColorArgb(hovered);
                }
            };
            categoryButton.setCellConstraints(new FCellConstraints().setExpandVertical(true));
            categoryButtonsRow.addChild(categoryButton);
        }
        controlsRow.addChild(categoryButtonsRow);

        FSpacerWidget rightPushSpacer = new FSpacerWidget(0f, 0f);
        rightPushSpacer.setCellConstraints(new FCellConstraints().setExpandHorizontal(true));
        controlsRow.addChild(rightPushSpacer);

        FMinWidthHostWidget searchHost = new FMinWidthHostWidget(GuiDesignSpace.pxX(300f), searchInput);
        controlsRow.addChild(searchHost);

        float horizontalInset = GuiDesignSpace.pxX(SettingsUiMetrics.SETTINGS_DETAIL_CONTENT_INSET_X_DESIGN);
        float controlsInnerWidth = Math.max(GuiDesignSpace.pxX(20f), settingsInnerWidth - 2f * horizontalInset);
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
            scrollBody.addChild(new FSpacerWidget(settingsContentWidth, GuiDesignSpace.pxY(UITheme.PADDING_XS)));
            FLabelWidget empty = new FLabelWidget();
            empty.setText(Component.translatable("fascinatedutils.setting.shell.empty_modules").getString());
            empty.setColorArgb(FascinatedGuiTheme.INSTANCE.textMuted());
            empty.setAlignX(Align.CENTER);
            scrollBody.addChild(empty);
            scrollBody.addChild(new FSpacerWidget(settingsContentWidth, GuiDesignSpace.pxY(UITheme.PADDING_SM)));
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
        scrollBody.addChild(new FSpacerWidget(settingsContentWidth, GuiDesignSpace.pxY(UITheme.PADDING_SM)));
        return buildModulesPaneLayout(controlsRowHost, createModulesGridScrollClip(scrollBody, gapY, modulesGridScrollYRef));
    }

    private static FWidget buildModulesPaneLayout(FWidget controlsRowHost, FScrollColumnWidget modulesScrollClip) {
        return new FWidget() {
            {
                addChild(controlsRowHost);
                addChild(modulesScrollClip);
            }

            @Override
            public boolean fillsVerticalInColumn() {
                return true;
            }

            @Override
            public boolean fillsHorizontalInRow() {
                return true;
            }

            @Override
            public void layout(UIRenderer measure, float layoutX, float layoutY, float layoutWidth, float layoutHeight) {
                setBounds(layoutX, layoutY, layoutWidth, layoutHeight);
                float topInset = GuiDesignSpace.pxY(UITheme.PADDING_SM);
                float bottomInset = GuiDesignSpace.pxY(UITheme.PADDING_SM);
                float sectionGap = GuiDesignSpace.pxY(UITheme.PADDING_SM);
                float controlsHeight = controlsRowHost.intrinsicHeightForColumn(measure, layoutWidth);
                float controlsY = layoutY + topInset;
                controlsRowHost.layout(measure, layoutX, controlsY, layoutWidth, controlsHeight);
                float scrollY = controlsY + controlsHeight + sectionGap;
                float scrollHeight = Math.max(0f, layoutY + layoutHeight - bottomInset - scrollY);
                modulesScrollClip.layout(measure, layoutX, scrollY, layoutWidth, scrollHeight);
            }
        };
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
        return chosenColumns;
    }
}
