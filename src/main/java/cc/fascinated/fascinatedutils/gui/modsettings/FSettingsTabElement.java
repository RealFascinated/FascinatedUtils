package cc.fascinated.fascinatedutils.gui.modsettings;

import cc.fascinated.fascinatedutils.gui.core.Align;
import cc.fascinated.fascinatedutils.gui.core.Ref;
import cc.fascinated.fascinatedutils.gui.declare.DeclarativeMountHost;
import cc.fascinated.fascinatedutils.gui.declare.UiView;
import cc.fascinated.fascinatedutils.gui.modsettings.components.ModSettingsSettingsPresentationComponent;
import cc.fascinated.fascinatedutils.gui.renderer.UIRenderer;
import cc.fascinated.fascinatedutils.gui.theme.SettingsUiMetrics;
import cc.fascinated.fascinatedutils.gui.widgets.*;
import net.minecraft.client.resources.language.I18n;
import org.jspecify.annotations.NonNull;

public class FSettingsTabElement extends FWidget implements ModSettingsSettingsPresentationComponent.HostSurface {

    private final Ref<Float> generalRegistryScrollRef = Ref.of(0f);
    private final Ref<Float> performanceRegistryScrollRef = Ref.of(0f);
    private final Ref<ModSettingsRegistrySettingsTabBuilder.RegistrySettingsSubTab> registrySubTabRef =
            Ref.of(ModSettingsRegistrySettingsTabBuilder.RegistrySettingsSubTab.GENERAL);
    private final DeclarativeMountHost declarativeMountHost;
    private int compositePresentationStamp;

    public FSettingsTabElement() {
        declarativeMountHost = new DeclarativeMountHost(this::settingsViewportDeclarative);
        addChild(declarativeMountHost);
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
        declarativeMountHost.layout(measure, layoutX, layoutY, layoutWidth, layoutHeight);
    }

    public void reset() {
        declarativeMountHost.dispose();
        generalRegistryScrollRef.setValue(0f);
        performanceRegistryScrollRef.setValue(0f);
        registrySubTabRef.setValue(ModSettingsRegistrySettingsTabBuilder.RegistrySettingsSubTab.GENERAL);
        bumpCompositeStamp();
    }

    public void disposeDeclarativeSubtree() {
        declarativeMountHost.dispose();
    }

    private void bumpCompositeStamp() {
        compositePresentationStamp++;
    }

    private UiView settingsViewportDeclarative(float viewportWidth, float viewportHeight) {
        return ModSettingsSettingsPresentationComponent.view(
                new ModSettingsSettingsPresentationComponent.Props(this, viewportWidth, viewportHeight, compositePresentationStamp));
    }

    @Override
    public FWidget composeSettingsPresentationSurface(float width, float height) {
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

        FRowWidget tabRow = getFRowWidget(controlsHeight);

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

        Ref<Float> activeScroll = registrySubTabRef.getValue() == ModSettingsRegistrySettingsTabBuilder.RegistrySettingsSubTab.GENERAL
                ? generalRegistryScrollRef
                : performanceRegistryScrollRef;
        float settingsPaneHeight = Math.max(1f, height - tabStripHeight);
        FWidget settingsContent = ModSettingsRegistrySettingsTabBuilder.buildSettingsTab(width, settingsPaneHeight, activeScroll, registrySubTabRef.getValue());
        settingsContent.setCellConstraints(new FCellConstraints().setExpandVertical(true));
        mainColumn.addChild(settingsContent);

        ModSettingsSettingsPresentationComponent.PresentationSurface rootSurface = ModSettingsSettingsPresentationComponent.presentationSurfaceShell();
        rootSurface.addChild(mainColumn);
        return rootSurface;
    }

    private @NonNull FRowWidget getFRowWidget(float controlsHeight) {
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
                bumpCompositeStamp();
            }
        }, () -> I18n.get("fascinatedutils.setting.shell.registry_tab_general"), 56f, 1, 1f, 6f, 1.12f, 7f, 2f,
                () -> registrySubTabRef.getValue() == ModSettingsRegistrySettingsTabBuilder.RegistrySettingsSubTab.GENERAL) {
            @Override
            public float intrinsicHeightForColumn(UIRenderer measure, float widthBudget) {
                return controlsHeight;
            }
        };

        FButtonWidget performanceTabButton = new SelectableButtonWidget(() -> {
            if (registrySubTabRef.getValue() != ModSettingsRegistrySettingsTabBuilder.RegistrySettingsSubTab.PERFORMANCE) {
                registrySubTabRef.setValue(ModSettingsRegistrySettingsTabBuilder.RegistrySettingsSubTab.PERFORMANCE);
                bumpCompositeStamp();
            }
        }, () -> I18n.get("fascinatedutils.setting.shell.registry_tab_performance"), 92f, 1, 1f, 6f, 1.12f, 7f, 2f,
                () -> registrySubTabRef.getValue() == ModSettingsRegistrySettingsTabBuilder.RegistrySettingsSubTab.PERFORMANCE) {
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
