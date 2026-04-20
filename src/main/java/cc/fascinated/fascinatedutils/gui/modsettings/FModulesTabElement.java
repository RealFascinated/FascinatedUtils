package cc.fascinated.fascinatedutils.gui.modsettings;

import java.util.List;

import cc.fascinated.fascinatedutils.gui.core.Callback;
import cc.fascinated.fascinatedutils.gui.core.Ref;
import cc.fascinated.fascinatedutils.gui.renderer.UIRenderer;
import cc.fascinated.fascinatedutils.gui.themes.FascinatedGuiTheme;
import cc.fascinated.fascinatedutils.gui.widgets.FAbsoluteStackWidget;
import cc.fascinated.fascinatedutils.gui.widgets.FRectWidget;
import cc.fascinated.fascinatedutils.gui.widgets.FSplitRowWithDividerWidget;
import cc.fascinated.fascinatedutils.gui.widgets.FWidget;
import cc.fascinated.fascinatedutils.systems.config.ModConfig;
import cc.fascinated.fascinatedutils.systems.modules.Module;
import cc.fascinated.fascinatedutils.systems.modules.ModuleCategory;
import cc.fascinated.fascinatedutils.systems.modules.ModuleRegistry;
import net.minecraft.client.animation.definitions.RabbitAnimation;

public class FModulesTabElement extends FWidget {
    private static final float PROFILES_PANEL_WIDTH_DESIGN = 118f;
    private static final float SPLIT_DIVIDER_WIDTH_DESIGN = 1f;

    private final Runnable onProfilesChanged;
    private final Runnable onOpenHudLayoutEditor;
    private final ProfilePopupController profilePopupController;
    private final Ref<Float> modulesGridScrollRef = Ref.of(0f);
    private final Ref<Float> moduleSettingsScrollRef = Ref.of(0f);
    private final Ref<String> moduleSearchRef = Ref.of("");
    private final Ref<ModuleCategory> moduleCategoryFilterRef = Ref.of(null);
    private final Ref<Float> profilesScrollRef = Ref.of(0f);
    private final Ref<String> newProfileNameRef = Ref.of("");
    private final Ref<Boolean> copyDefaultProfileSettingsRef = Ref.of(false);
    private FWidget inner;
    private Module moduleDetailModule;
    private Module settingsScrollAnchorModule;
    private boolean needsRebuild = true;
    private boolean showCreateProfilePopup;
    private float lastLayoutWidth = -1f;
    private float lastLayoutHeight = -1f;
    private Module lastBuiltDetailModule;

    public FModulesTabElement(Runnable onProfilesChanged, Runnable onOpenHudLayoutEditor) {
        this.onProfilesChanged = onProfilesChanged;
        this.onOpenHudLayoutEditor = onOpenHudLayoutEditor;
        this.profilePopupController = new ProfilePopupController(
                () -> needsRebuild = true,
                this::handleProfilesChanged,
                () -> profilesScrollRef.setValue(0f)
        );
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
        }
        syncScrollAnchors();
        if (needsRebuild || layoutWidth != lastLayoutWidth || layoutHeight != lastLayoutHeight || moduleDetailModule != lastBuiltDetailModule) {
            rebuild(layoutWidth, layoutHeight);
        }
        if (inner != null) {
            inner.layout(measure, layoutX, layoutY, layoutWidth, layoutHeight);
        }
    }

    public void reset() {
        modulesGridScrollRef.setValue(0f);
        moduleSettingsScrollRef.setValue(0f);
        moduleSearchRef.setValue("");
        moduleCategoryFilterRef.setValue(null);
        profilesScrollRef.setValue(0f);
        newProfileNameRef.setValue("");
        copyDefaultProfileSettingsRef.setValue(false);
        inner = null;
        moduleDetailModule = null;
        settingsScrollAnchorModule = null;
        showCreateProfilePopup = false;
        profilePopupController.reset();
        needsRebuild = true;
        lastLayoutWidth = -1f;
        lastLayoutHeight = -1f;
        lastBuiltDetailModule = null;
        clearChildren();
    }

    /**
     * Opens the modules tab detail view for the given module (used when entering settings from elsewhere, e.g. HUD editor).
     *
     * @param module module whose settings page should be shown
     */
    public void navigateToModuleSettingsPage(Module module) {
        openModuleDetail(module);
    }

    private void rebuild(float width, float height) {
        Callback<Module> openModuleSettings = this::openModuleDetail;

        float splitDividerWidth = Math.max(1f, (float) Math.floor(SPLIT_DIVIDER_WIDTH_DESIGN));
        float profilesPanelWidth = Math.max(112f, Math.min(PROFILES_PANEL_WIDTH_DESIGN, width * 0.4f));
        float modulesPanelWidth = Math.max(0f, width - profilesPanelWidth - splitDividerWidth);

        FSplitRowWithDividerWidget splitLayout = new FSplitRowWithDividerWidget(profilesPanelWidth, splitDividerWidth);
        splitLayout.addChild(ModSettingsProfilesTabBuilder.buildProfilesPane(profilesPanelWidth, height, profilesScrollRef, this::openCreateProfilePopup, profilePopupController.contextMenuCallback(), this::handleProfilesChanged, onOpenHudLayoutEditor));

        FRectWidget splitDivider = new FRectWidget();
        splitDivider.setFillColorArgb(FascinatedGuiTheme.INSTANCE.borderMuted());
        splitLayout.addChild(splitDivider);

        FWidget modulesPane = ModSettingsModulesTabBuilder.buildModulesTab(modulesPanelWidth, height, List.copyOf(ModuleRegistry.INSTANCE.getModules()), modulesGridScrollRef, moduleDetailModule, this::closeModuleDetail, openModuleSettings, moduleSettingsScrollRef, moduleSearchRef, moduleCategoryFilterRef, this::onModuleFiltersChanged);
        splitLayout.addChild(modulesPane);

        FAbsoluteStackWidget rootStack = new FAbsoluteStackWidget();
        rootStack.addChild(splitLayout);
        if (showCreateProfilePopup) {
            rootStack.addChild(new FProfileCreatePopupWidget(newProfileNameRef, copyDefaultProfileSettingsRef, this::closeCreateProfilePopup, this::submitCreateProfilePopup));
        }
        profilePopupController.appendOverlaysTo(rootStack);
        inner = rootStack;
        clearChildren();
        addChild(inner);
        needsRebuild = false;
        lastLayoutWidth = width;
        lastLayoutHeight = height;
        lastBuiltDetailModule = moduleDetailModule;
    }

    private void openCreateProfilePopup() {
        if (showCreateProfilePopup) {
            return;
        }
        showCreateProfilePopup = true;
        newProfileNameRef.setValue("");
        copyDefaultProfileSettingsRef.setValue(false);
        needsRebuild = true;
    }

    private void closeCreateProfilePopup() {
        if (!showCreateProfilePopup) {
            return;
        }
        showCreateProfilePopup = false;
        needsRebuild = true;
    }

    private void submitCreateProfilePopup(String requestedProfileName, boolean copyDefaultProfileSettings) {
        String normalizedName = requestedProfileName == null ? "" : requestedProfileName.trim();
        if (normalizedName.isBlank()) {
            return;
        }
        if (ModConfig.profileNameExists(normalizedName)) {
            return;
        }
        ModConfig.createProfile(normalizedName, copyDefaultProfileSettings);
        showCreateProfilePopup = false;
        profilesScrollRef.setValue(0f);
        needsRebuild = true;
        handleProfilesChanged();
    }

    private void handleProfilesChanged() {
        needsRebuild = true;
        onProfilesChanged.run();
    }

    private void onModuleFiltersChanged() {
        modulesGridScrollRef.setValue(0f);
        needsRebuild = true;
    }

    private void openModuleDetail(Module module) {
        if (moduleDetailModule == module) {
            return;
        }
        moduleDetailModule = module;
        moduleSettingsScrollRef.setValue(0f);
        settingsScrollAnchorModule = module;
        needsRebuild = true;
    }

    private void closeModuleDetail() {
        if (moduleDetailModule == null) {
            return;
        }
        moduleDetailModule = null;
        needsRebuild = true;
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
