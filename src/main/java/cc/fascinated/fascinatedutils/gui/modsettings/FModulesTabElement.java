package cc.fascinated.fascinatedutils.gui.modsettings;

import cc.fascinated.fascinatedutils.common.setting.impl.ColorSetting;
import cc.fascinated.fascinatedutils.gui.core.Callback;
import cc.fascinated.fascinatedutils.gui.core.GuiFocusState;
import cc.fascinated.fascinatedutils.gui.core.Ref;
import cc.fascinated.fascinatedutils.gui.declare.DeclarativeMountHost;
import cc.fascinated.fascinatedutils.gui.declare.UiView;
import cc.fascinated.fascinatedutils.gui.modsettings.components.ModSettingsModulesPresentationComponent;
import cc.fascinated.fascinatedutils.gui.renderer.UIRenderer;
import cc.fascinated.fascinatedutils.gui.themes.FascinatedGuiTheme;
import cc.fascinated.fascinatedutils.gui.widgets.FOutlinedTextInputWidget;
import cc.fascinated.fascinatedutils.gui.widgets.FRectWidget;
import cc.fascinated.fascinatedutils.gui.widgets.FSplitRowWithDividerWidget;
import cc.fascinated.fascinatedutils.gui.widgets.FWidget;
import cc.fascinated.fascinatedutils.systems.config.ModConfig;
import cc.fascinated.fascinatedutils.systems.modules.Module;
import cc.fascinated.fascinatedutils.systems.modules.ModuleCategory;
import cc.fascinated.fascinatedutils.systems.modules.ModuleRegistry;

import org.jspecify.annotations.Nullable;

import java.util.List;

public class FModulesTabElement extends FWidget implements ModSettingsModulesPresentationComponent.HostSurface {
    private static final float PROFILES_PANEL_WIDTH_DESIGN = 118f;
    private static final float SPLIT_DIVIDER_WIDTH_DESIGN = 1f;

    private final Runnable onProfilesChanged;
    private final Runnable onOpenHudLayoutEditor;
    private final ProfilePopupController profilePopupController;
    private final Ref<Float> modulesGridScrollRef = Ref.of(0f);
    private final Ref<Float> moduleSettingsScrollRef = Ref.of(0f);
    private final Ref<String> moduleSearchRef = Ref.of("");
    private final Ref<String> moduleSettingsSearchRef = Ref.of("");
    private final Ref<ModuleCategory> moduleCategoryFilterRef = Ref.of(null);
    private final Ref<Float> profilesScrollRef = Ref.of(0f);
    private final Ref<String> newProfileNameRef = Ref.of("");
    private final Ref<Boolean> copyDefaultProfileSettingsRef = Ref.of(false);
    private final DeclarativeMountHost declarativeMountHost;
    private int compositePresentationStamp;
    private Module moduleDetailModule;
    private Module settingsScrollAnchorModule;
    private boolean showCreateProfilePopup;
    private FColorPickerPopupWidget colorPickerWidget;
    private @Nullable FOutlinedTextInputWidget moduleDetailSearchField;

    public FModulesTabElement(Runnable onProfilesChanged, Runnable onOpenHudLayoutEditor) {
        this.onProfilesChanged = onProfilesChanged;
        this.onOpenHudLayoutEditor = onOpenHudLayoutEditor;
        this.profilePopupController = new ProfilePopupController(
                () -> {},
                this::handleProfilesChanged,
                () -> profilesScrollRef.setValue(0f)
        );
        declarativeMountHost = new DeclarativeMountHost(this::modulesViewportDeclarative);
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
        List<Module> modules = ModuleRegistry.INSTANCE.getModules();
        if (moduleDetailModule != null && !modules.contains(moduleDetailModule)) {
            moduleDetailModule = null;
            moduleSettingsScrollRef.setValue(0f);
            settingsScrollAnchorModule = null;
            bumpCompositeStamp();
        }
        syncScrollAnchors();
        declarativeMountHost.layout(measure, layoutX, layoutY, layoutWidth, layoutHeight);
    }

    public void reset() {
        declarativeMountHost.dispose();
        modulesGridScrollRef.setValue(0f);
        moduleSettingsScrollRef.setValue(0f);
        moduleSearchRef.setValue("");
        moduleSettingsSearchRef.setValue("");
        moduleCategoryFilterRef.setValue(null);
        profilesScrollRef.setValue(0f);
        newProfileNameRef.setValue("");
        copyDefaultProfileSettingsRef.setValue(false);
        moduleDetailModule = null;
        settingsScrollAnchorModule = null;
        showCreateProfilePopup = false;
        colorPickerWidget = null;
        moduleDetailSearchField = null;
        profilePopupController.reset();
        bumpCompositeStamp();
    }

    public void disposeDeclarativeSubtree() {
        declarativeMountHost.dispose();
    }

    /**
     * Opens the modules tab detail view for the given module (used when entering settings from elsewhere, e.g. HUD editor).
     *
     * @param module module whose settings page should be shown
     */
    public void navigateToModuleSettingsPage(Module module) {
        openModuleDetail(module);
    }

    private void bumpCompositeStamp() {
        compositePresentationStamp++;
    }

    private UiView modulesViewportDeclarative(float viewportWidth, float viewportHeight) {
        syncScrollAnchors();
        return ModSettingsModulesPresentationComponent.view(new ModSettingsModulesPresentationComponent.Props(this, viewportWidth, viewportHeight, compositePresentationStamp));
    }

    @Override
    public FWidget composeModulesPresentationSurface(float width, float height) {
        Callback<Module> openModuleSettings = this::openModuleDetail;

        float splitDividerWidth = Math.max(1f, (float) Math.floor(SPLIT_DIVIDER_WIDTH_DESIGN));
        float profilesPanelWidth = Math.max(112f, Math.min(PROFILES_PANEL_WIDTH_DESIGN, width * 0.4f));
        float modulesPanelWidth = Math.max(0f, width - profilesPanelWidth - splitDividerWidth);

        FSplitRowWithDividerWidget splitLayout = new FSplitRowWithDividerWidget(profilesPanelWidth, splitDividerWidth);
        splitLayout.addChild(ModSettingsProfilesTabBuilder.buildProfilesPane(profilesPanelWidth, height, profilesScrollRef, this::openCreateProfilePopup, profilePopupController.contextMenuCallback(),
                this::handleProfilesChanged, onOpenHudLayoutEditor));

        FRectWidget splitDivider = new FRectWidget();
        splitDivider.setFillColorArgb(FascinatedGuiTheme.INSTANCE.borderMuted());
        splitLayout.addChild(splitDivider);

        FWidget modulesPane = ModSettingsModulesTabBuilder.buildModulesTab(modulesPanelWidth, height, List.copyOf(ModuleRegistry.INSTANCE.getModules()), modulesGridScrollRef, moduleDetailModule,
                this::closeModuleDetail, openModuleSettings, moduleSettingsScrollRef, moduleSearchRef, moduleCategoryFilterRef, this::onModuleFiltersChanged, moduleSettingsSearchRef,
                this::onModuleSettingsSearchChanged, this::openColorPicker, this::lazyModuleDetailSearchField);
        splitLayout.addChild(modulesPane);

        ModSettingsModulesPresentationComponent.PresentationSurface rootSurface = ModSettingsModulesPresentationComponent.presentationSurfaceShell();
        rootSurface.addChild(splitLayout);
        if (showCreateProfilePopup) {
            rootSurface.addChild(new FProfileCreatePopupWidget(newProfileNameRef, copyDefaultProfileSettingsRef, this::closeCreateProfilePopup, this::submitCreateProfilePopup));
        }
        if (colorPickerWidget != null) {
            rootSurface.addChild(colorPickerWidget);
        }
        profilePopupController.appendOverlaysTo(rootSurface);
        return rootSurface;
    }

    private void openColorPicker(ColorSetting setting) {
        colorPickerWidget = new FColorPickerPopupWidget(setting.getValue(), newColor -> {
            setting.setValue(newColor);
            ModConfig.profiles().saveActiveProfile();
            colorPickerWidget = null;
        }, () -> {
            colorPickerWidget = null;
        });
    }

    private void openCreateProfilePopup() {
        if (showCreateProfilePopup) {
            return;
        }
        showCreateProfilePopup = true;
        newProfileNameRef.setValue("");
        copyDefaultProfileSettingsRef.setValue(false);
    }

    private void closeCreateProfilePopup() {
        if (!showCreateProfilePopup) {
            return;
        }
        showCreateProfilePopup = false;
    }

    private void submitCreateProfilePopup(String requestedProfileName, boolean copyDefaultProfileSettings) {
        String normalizedName = requestedProfileName == null ? "" : requestedProfileName.trim();
        if (normalizedName.isBlank()) {
            return;
        }
        if (ModConfig.profiles().profileNameExists(normalizedName)) {
            return;
        }
        ModConfig.profiles().createProfile(normalizedName, copyDefaultProfileSettings);
        showCreateProfilePopup = false;
        profilesScrollRef.setValue(0f);
        bumpCompositeStamp();
        handleProfilesChanged();
    }

    private void handleProfilesChanged() {
        bumpCompositeStamp();
        onProfilesChanged.run();
    }

    private void onModuleFiltersChanged() {
        modulesGridScrollRef.setValue(0f);
        bumpCompositeStamp();
    }

    private void onModuleSettingsSearchChanged() {
        moduleSettingsScrollRef.setValue(0f);
        bumpCompositeStamp();
    }

    private void openModuleDetail(Module module) {
        if (moduleDetailModule == module) {
            return;
        }
        moduleDetailModule = module;
        moduleSettingsScrollRef.setValue(0f);
        moduleSettingsSearchRef.setValue("");
        settingsScrollAnchorModule = module;
        bumpCompositeStamp();
    }

    private void closeModuleDetail() {
        if (moduleDetailModule == null) {
            return;
        }
        moduleDetailModule = null;
        moduleDetailSearchField = null;
        moduleSettingsSearchRef.setValue("");
        bumpCompositeStamp();
    }

    private FOutlinedTextInputWidget lazyModuleDetailSearchField() {
        if (moduleDetailSearchField == null) {
            moduleDetailSearchField = ModSettingsModuleDetailBuilder.createSharedModuleDetailSearchField();
            moduleDetailSearchField.setExternalFocusIdSupplier(GuiFocusState::getFocusedId);
        }
        return moduleDetailSearchField;
    }

    private void syncScrollAnchors() {
        if (moduleDetailModule == null) {
            return;
        }
        if (settingsScrollAnchorModule != moduleDetailModule) {
            moduleSettingsScrollRef.setValue(0f);
            settingsScrollAnchorModule = moduleDetailModule;
        }
    }
}
