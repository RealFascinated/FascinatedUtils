package cc.fascinated.fascinatedutils.gui.modsettings;

import cc.fascinated.fascinatedutils.gui.core.Align;
import cc.fascinated.fascinatedutils.gui.core.FNodeRegistry;
import cc.fascinated.fascinatedutils.gui.core.FNodeWidget;
import cc.fascinated.fascinatedutils.gui.core.FState;
import cc.fascinated.fascinatedutils.gui.core.FWidgetNode;
import cc.fascinated.fascinatedutils.gui.renderer.UIRenderer;
import cc.fascinated.fascinatedutils.gui.theme.SettingsUiMetrics;
import cc.fascinated.fascinatedutils.gui.widgets.*;
import net.minecraft.client.resources.language.I18n;

public class FSettingsTabElement extends FWidget {

    private final FNodeRegistry nodes = new FNodeRegistry();
    private final FNodeWidget root;

    // FState fields – null until buildRootWidget initialises them
    private FState<Float> generalRegistryScrollRef;
    private FState<Float> performanceRegistryScrollRef;
    private FState<ModSettingsRegistrySettingsTabBuilder.RegistrySettingsSubTab> registrySubTabRef;

    public FSettingsTabElement() {
        root = new FNodeWidget(nodes.get("settings", this::buildRootWidget));
        addChild(root);
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
        root.layout(measure, layoutX, layoutY, layoutWidth, layoutHeight);
        nodes.gc();
    }

    public void reset() {
        if (generalRegistryScrollRef != null) generalRegistryScrollRef.set(0f);
        if (performanceRegistryScrollRef != null) performanceRegistryScrollRef.set(0f);
        if (registrySubTabRef != null) registrySubTabRef.set(ModSettingsRegistrySettingsTabBuilder.RegistrySettingsSubTab.GENERAL);
    }

    public void disposeDeclarativeSubtree() {
        nodes.dispose();
    }

    private FWidget buildRootWidget(FWidgetNode.RenderContext ctx) {
        generalRegistryScrollRef = ctx.useState(0f);
        performanceRegistryScrollRef = ctx.useState(0f);
        registrySubTabRef = ctx.useState(ModSettingsRegistrySettingsTabBuilder.RegistrySettingsSubTab.GENERAL);
        return new FWidget() {
            private float lastWidth = Float.NaN;
            private float lastHeight = Float.NaN;

            @Override
            public boolean fillsHorizontalInRow() {
                return true;
            }

            @Override
            public boolean fillsVerticalInColumn() {
                return true;
            }

            @Override
            public void layout(UIRenderer measure, float lx, float ly, float lw, float lh) {
                setBounds(lx, ly, lw, lh);
                boolean dimChanged = Math.abs(lw - lastWidth) > 0.5f || Math.abs(lh - lastHeight) > 0.5f;
                if (dimChanged || childrenView().isEmpty()) {
                    lastWidth = lw;
                    lastHeight = lh;
                    clearChildren();
                    addChild(buildSurface(lw, lh));
                }
                for (FWidget child : childrenView()) {
                    child.layout(measure, lx, ly, lw, lh);
                }
            }
        };
    }

    private FWidget buildSurface(float width, float height) {
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

        FRowWidget tabRow = buildTabRow(controlsHeight);

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

        ModSettingsRegistrySettingsTabBuilder.RegistrySettingsSubTab activeSubTab = registrySubTabRef.get();
        FState<Float> activeScroll = activeSubTab == ModSettingsRegistrySettingsTabBuilder.RegistrySettingsSubTab.GENERAL ? generalRegistryScrollRef : performanceRegistryScrollRef;
        float settingsPaneHeight = Math.max(1f, height - tabStripHeight);
        FWidget settingsContent = ModSettingsRegistrySettingsTabBuilder.buildSettingsTab(width, settingsPaneHeight, activeScroll, activeSubTab);
        settingsContent.setCellConstraints(new FCellConstraints().setExpandVertical(true));
        mainColumn.addChild(settingsContent);

        return mainColumn;
    }

    private FRowWidget buildTabRow(float controlsHeight) {
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
            if (registrySubTabRef.get() != ModSettingsRegistrySettingsTabBuilder.RegistrySettingsSubTab.GENERAL) {
                registrySubTabRef.set(ModSettingsRegistrySettingsTabBuilder.RegistrySettingsSubTab.GENERAL);
            }
        }, () -> I18n.get("fascinatedutils.setting.shell.registry_tab_general"), 56f, 1, 1f, 6f, 1.12f, 7f, 2f, () -> registrySubTabRef.get() == ModSettingsRegistrySettingsTabBuilder.RegistrySettingsSubTab.GENERAL) {
            @Override
            public float intrinsicHeightForColumn(UIRenderer measure, float widthBudget) {
                return controlsHeight;
            }
        };

        FButtonWidget performanceTabButton = new SelectableButtonWidget(() -> {
            if (registrySubTabRef.get() != ModSettingsRegistrySettingsTabBuilder.RegistrySettingsSubTab.PERFORMANCE) {
                registrySubTabRef.set(ModSettingsRegistrySettingsTabBuilder.RegistrySettingsSubTab.PERFORMANCE);
            }
        }, () -> I18n.get("fascinatedutils.setting.shell.registry_tab_performance"), 92f, 1, 1f, 6f, 1.12f, 7f, 2f, () -> registrySubTabRef.get() == ModSettingsRegistrySettingsTabBuilder.RegistrySettingsSubTab.PERFORMANCE) {
            @Override
            public float intrinsicHeightForColumn(UIRenderer measure, float widthBudget) {
                return controlsHeight;
            }
        };

        tabRow.addChild(generalTabButton);
        tabRow.addChild(performanceTabButton);
        return tabRow;
    }
}
