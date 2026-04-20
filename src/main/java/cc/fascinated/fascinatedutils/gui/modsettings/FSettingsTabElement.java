package cc.fascinated.fascinatedutils.gui.modsettings;

import cc.fascinated.fascinatedutils.common.setting.impl.ColorSetting;
import cc.fascinated.fascinatedutils.gui.core.Align;
import cc.fascinated.fascinatedutils.gui.core.Ref;
import cc.fascinated.fascinatedutils.gui.renderer.UIRenderer;
import cc.fascinated.fascinatedutils.gui.theme.SettingsUiMetrics;
import cc.fascinated.fascinatedutils.gui.widgets.*;
import cc.fascinated.fascinatedutils.gui.widgets.SelectableButtonWidget;
import cc.fascinated.fascinatedutils.systems.config.ModConfig;
import net.minecraft.client.resources.language.I18n;

import java.util.function.Consumer;

public class FSettingsTabElement extends FWidget {
    private final Ref<Float> generalRegistryScrollRef = Ref.of(0f);
    private final Ref<Float> performanceRegistryScrollRef = Ref.of(0f);
    private final Ref<ModSettingsRegistrySettingsTabBuilder.RegistrySettingsSubTab> registrySubTabRef = Ref.of(ModSettingsRegistrySettingsTabBuilder.RegistrySettingsSubTab.GENERAL);
    private FWidget inner;
    private boolean dirty = true;
    private float cachedWidth = -1f;
    private float cachedHeight = -1f;

    private boolean showColorPicker;
    private ColorSetting activeColorSetting;

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
        if (dirty || cachedWidth != layoutWidth || cachedHeight != layoutHeight || inner == null) {
            rebuild(layoutWidth, layoutHeight);
            cachedWidth = layoutWidth;
            cachedHeight = layoutHeight;
            dirty = false;
        }
        if (inner != null) {
            inner.layout(measure, layoutX, layoutY, layoutWidth, layoutHeight);
        }
    }

    public void reset() {
        generalRegistryScrollRef.setValue(0f);
        performanceRegistryScrollRef.setValue(0f);
        registrySubTabRef.setValue(ModSettingsRegistrySettingsTabBuilder.RegistrySettingsSubTab.GENERAL);
        inner = null;
        dirty = true;
        cachedWidth = -1f;
        cachedHeight = -1f;
        showColorPicker = false;
        activeColorSetting = null;
        clearChildren();
    }

    private void rebuild(float width, float height) {
        Consumer<ColorSetting> openColorPicker = this::openColorPicker;
        float controlsHeight = SettingsUiMetrics.SHELL_CONTROL_HEIGHT_DESIGN;
        float columnGap = 4f;
        float tabStripTopInset = 4f;
        float horizontalInset = SettingsUiMetrics.SETTINGS_DETAIL_CONTENT_INSET_X_DESIGN;
        float tabStripInnerWidth = Math.max(14f, width - 2f * horizontalInset);
        float tabStripHeight = tabStripTopInset + controlsHeight + columnGap;

        FColumnWidget mainColumn = new FColumnWidget(columnGap, Align.CENTER) {
            @Override
            public boolean fillsVerticalInColumn() {
                return true;
            }

            @Override
            public boolean fillsHorizontalInRow() {
                return true;
            }
        };

        FRowWidget tabRow = new FRowWidget(3f, Align.CENTER) {
            @Override
            public boolean fillsHorizontalInRow() {
                return true;
            }

            @Override
            public float intrinsicHeightForColumn(UIRenderer measure, float widthBudget) {
                return controlsHeight;
            }
        };

        FButtonWidget generalTabButton = new SelectableButtonWidget(() -> {
            if (registrySubTabRef.getValue() != ModSettingsRegistrySettingsTabBuilder.RegistrySettingsSubTab.GENERAL) {
                registrySubTabRef.setValue(ModSettingsRegistrySettingsTabBuilder.RegistrySettingsSubTab.GENERAL);
                dirty = true;
            }
        }, () -> I18n.get("fascinatedutils.setting.shell.registry_tab_general"), 56f, 1, 1f, 6f, 1.12f, 7f, 2f, () -> registrySubTabRef.getValue() == ModSettingsRegistrySettingsTabBuilder.RegistrySettingsSubTab.GENERAL) {
            @Override
            public float intrinsicHeightForColumn(UIRenderer measure, float widthBudget) {
                return controlsHeight;
            }
        };

        FButtonWidget performanceTabButton = new SelectableButtonWidget(() -> {
            if (registrySubTabRef.getValue() != ModSettingsRegistrySettingsTabBuilder.RegistrySettingsSubTab.PERFORMANCE) {
                registrySubTabRef.setValue(ModSettingsRegistrySettingsTabBuilder.RegistrySettingsSubTab.PERFORMANCE);
                dirty = true;
            }
        }, () -> I18n.get("fascinatedutils.setting.shell.registry_tab_performance"), 92f, 1, 1f, 6f, 1.12f, 7f, 2f, () -> registrySubTabRef.getValue() == ModSettingsRegistrySettingsTabBuilder.RegistrySettingsSubTab.PERFORMANCE) {
            @Override
            public float intrinsicHeightForColumn(UIRenderer measure, float widthBudget) {
                return controlsHeight;
            }
        };

        tabRow.addChild(generalTabButton);
        tabRow.addChild(performanceTabButton);

        FRowWidget paddedTabStrip = new FRowWidget(0f, Align.START) {
            @Override
            public boolean fillsHorizontalInRow() {
                return true;
            }

            @Override
            public float intrinsicHeightForColumn(UIRenderer measure, float widthBudget) {
                return controlsHeight;
            }
        };
        paddedTabStrip.addChild(new FSpacerWidget(horizontalInset, 0f));
        FMinWidthHostWidget tabRowHost = new FMinWidthHostWidget(tabStripInnerWidth, tabRow);
        tabRowHost.setCellConstraints(new FCellConstraints().setExpandHorizontal(true));
        paddedTabStrip.addChild(tabRowHost);
        paddedTabStrip.addChild(new FSpacerWidget(horizontalInset, 0f));

        mainColumn.addChild(new FSpacerWidget(width, tabStripTopInset));
        mainColumn.addChild(paddedTabStrip);

        Ref<Float> activeScroll = registrySubTabRef.getValue() == ModSettingsRegistrySettingsTabBuilder.RegistrySettingsSubTab.GENERAL ? generalRegistryScrollRef : performanceRegistryScrollRef;
        float settingsPaneHeight = Math.max(1f, height - tabStripHeight);
        FWidget settingsContent = ModSettingsRegistrySettingsTabBuilder.buildSettingsTab(width, settingsPaneHeight, activeScroll, openColorPicker, registrySubTabRef.getValue());
        settingsContent.setCellConstraints(new FCellConstraints().setExpandVertical(true));
        mainColumn.addChild(settingsContent);

        FAbsoluteStackWidget rootStack = new FAbsoluteStackWidget();
        rootStack.addChild(mainColumn);

        if (showColorPicker && activeColorSetting != null) {
            ColorSetting captured = activeColorSetting;
            rootStack.addChild(new FColorPickerPopupWidget(captured.getValue(), newColor -> {
                captured.setValue(newColor);
                ModConfig.saveSettings();
                closeColorPicker();
            }, this::closeColorPicker));
        }

        inner = rootStack;
        clearChildren();
        addChild(inner);
    }

    private void openColorPicker(ColorSetting colorSetting) {
        if (showColorPicker) {
            return;
        }
        activeColorSetting = colorSetting;
        showColorPicker = true;
        dirty = true;
    }

    private void closeColorPicker() {
        if (!showColorPicker) {
            return;
        }
        showColorPicker = false;
        activeColorSetting = null;
        dirty = true;
    }
}
