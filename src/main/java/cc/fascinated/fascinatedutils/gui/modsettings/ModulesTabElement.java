package cc.fascinated.fascinatedutils.gui.modsettings;

import cc.fascinated.fascinatedutils.gui.GuiDesignSpace;
import cc.fascinated.fascinatedutils.gui.core.Callback;
import cc.fascinated.fascinatedutils.gui.core.Ref;
import cc.fascinated.fascinatedutils.gui.renderer.UIRenderer;
import cc.fascinated.fascinatedutils.gui.themes.fascinated.FascinatedGuiTheme;
import cc.fascinated.fascinatedutils.gui.widgets.FAbsoluteStackWidget;
import cc.fascinated.fascinatedutils.gui.widgets.FRectWidget;
import cc.fascinated.fascinatedutils.gui.widgets.FSplitRowWithDividerWidget;
import cc.fascinated.fascinatedutils.gui.widgets.FWidget;
import cc.fascinated.fascinatedutils.systems.config.ModConfig;
import cc.fascinated.fascinatedutils.systems.modules.Module;
import cc.fascinated.fascinatedutils.systems.modules.ModuleCategory;
import cc.fascinated.fascinatedutils.systems.modules.ModuleRegistry;

import java.util.List;
import java.util.UUID;

public class ModulesTabElement extends FWidget {
    private static final float PROFILES_PANEL_WIDTH_DESIGN = 168f;
    private static final float SPLIT_DIVIDER_WIDTH_DESIGN = 1f;

    private final Runnable onProfilesChanged;
    private final Runnable onOpenHudLayoutEditor;
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
    private boolean showDeleteProfilePopup;
    private UUID deleteConfirmProfileId;
    private String deleteConfirmProfileName;
    private boolean showRenameProfilePopup;
    private UUID renameProfileId;
    private String renameProfileName;
    private boolean showContextMenu;
    private UUID contextMenuProfileId;
    private float contextMenuX;
    private float contextMenuY;
    private float lastLayoutWidth = -1f;
    private float lastLayoutHeight = -1f;
    private Module lastBuiltDetailModule;

    public ModulesTabElement(Runnable onProfilesChanged, Runnable onOpenHudLayoutEditor) {
        this.onProfilesChanged = onProfilesChanged;
        this.onOpenHudLayoutEditor = onOpenHudLayoutEditor;
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
        showDeleteProfilePopup = false;
        deleteConfirmProfileId = null;
        deleteConfirmProfileName = null;
        showRenameProfilePopup = false;
        renameProfileId = null;
        renameProfileName = null;
        showContextMenu = false;
        contextMenuProfileId = null;
        needsRebuild = true;
        lastLayoutWidth = -1f;
        lastLayoutHeight = -1f;
        lastBuiltDetailModule = null;
        clearChildren();
    }

    private void rebuild(float width, float height) {
        Callback<Module> openModuleSettings = this::openModuleDetail;

        float splitDividerWidth = Math.max(1f, (float) Math.floor(GuiDesignSpace.pxX(SPLIT_DIVIDER_WIDTH_DESIGN)));
        float profilesPanelWidth = Math.max(GuiDesignSpace.pxX(160f), Math.min(GuiDesignSpace.pxX(PROFILES_PANEL_WIDTH_DESIGN), width * 0.4f));
        float modulesPanelWidth = Math.max(0f, width - profilesPanelWidth - splitDividerWidth);

        FSplitRowWithDividerWidget splitLayout = new FSplitRowWithDividerWidget(profilesPanelWidth, splitDividerWidth);
        splitLayout.addChild(ModSettingsProfilesTabBuilder.buildProfilesPane(profilesPanelWidth, height, profilesScrollRef, this::openCreateProfilePopup, this::openContextMenu, this::handleProfilesChanged, onOpenHudLayoutEditor));

        FRectWidget splitDivider = new FRectWidget();
        splitDivider.setFillColorArgb(FascinatedGuiTheme.INSTANCE.borderMuted());
        splitLayout.addChild(splitDivider);

        FWidget modulesPane = ModSettingsModulesTabBuilder.buildModulesTab(modulesPanelWidth, height, List.copyOf(ModuleRegistry.INSTANCE.getModules()), modulesGridScrollRef, moduleDetailModule, this::closeModuleDetail, openModuleSettings, moduleSettingsScrollRef, moduleSearchRef, moduleCategoryFilterRef, this::onModuleFiltersChanged);
        splitLayout.addChild(modulesPane);

        FAbsoluteStackWidget rootStack = new FAbsoluteStackWidget();
        rootStack.addChild(splitLayout);
        if (showCreateProfilePopup) {
            rootStack.addChild(new ProfileCreatePopupWidget(newProfileNameRef, copyDefaultProfileSettingsRef, this::closeCreateProfilePopup, this::submitCreateProfilePopup));
        }
        if (showDeleteProfilePopup && deleteConfirmProfileName != null) {
            rootStack.addChild(new ProfileDeletePopupWidget(deleteConfirmProfileName, this::closeDeleteProfilePopup, this::confirmDeleteProfile));
        }
        if (showRenameProfilePopup && renameProfileName != null) {
            rootStack.addChild(new ProfileRenamePopupWidget(renameProfileName, this::closeRenameProfilePopup, this::submitRenameProfile));
        }
        if (showContextMenu && contextMenuProfileId != null) {
            rootStack.addChild(new ProfileContextMenuWidget(contextMenuX, contextMenuY, this::closeContextMenu, this::handleContextMenuAction));
        }
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

    private void openDeleteProfilePopup(UUID profileId) {
        if (showDeleteProfilePopup) {
            return;
        }
        ModConfig.listProfiles().stream().filter(profile -> profile.getProfileId().equals(profileId)).findFirst().ifPresent(profile -> {
            deleteConfirmProfileId = profileId;
            deleteConfirmProfileName = profile.getProfileName();
            showDeleteProfilePopup = true;
            needsRebuild = true;
        });
    }

    private void closeDeleteProfilePopup() {
        if (!showDeleteProfilePopup) {
            return;
        }
        showDeleteProfilePopup = false;
        deleteConfirmProfileId = null;
        deleteConfirmProfileName = null;
        needsRebuild = true;
    }

    private void confirmDeleteProfile() {
        if (!showDeleteProfilePopup || deleteConfirmProfileId == null) {
            return;
        }
        UUID idToDelete = deleteConfirmProfileId;
        showDeleteProfilePopup = false;
        deleteConfirmProfileId = null;
        deleteConfirmProfileName = null;
        needsRebuild = true;
        if (ModConfig.deleteProfile(idToDelete)) {
            profilesScrollRef.setValue(0f);
            handleProfilesChanged();
        }
    }

    private void openContextMenu(UUID profileId, float x, float y) {
        if (showContextMenu) {
            return;
        }
        contextMenuProfileId = profileId;
        contextMenuX = x;
        contextMenuY = y;
        showContextMenu = true;
        needsRebuild = true;
    }

    private void closeContextMenu() {
        if (!showContextMenu) {
            return;
        }
        showContextMenu = false;
        contextMenuProfileId = null;
        needsRebuild = true;
    }

    private void handleContextMenuAction(String action) {
        UUID selectedProfileId = contextMenuProfileId;
        closeContextMenu();
        if ("rename".equals(action) && selectedProfileId != null) {
            ModConfig.listProfiles().stream().filter(profile -> profile.getProfileId().equals(selectedProfileId)).findFirst().ifPresent(profile -> {
                renameProfileId = profile.getProfileId();
                renameProfileName = profile.getProfileName();
                showRenameProfilePopup = true;
                needsRebuild = true;
            });
        }
        else if ("delete".equals(action) && selectedProfileId != null) {
            ModConfig.listProfiles().stream().filter(profile -> profile.getProfileId().equals(selectedProfileId)).findFirst().ifPresent(profile -> {
                deleteConfirmProfileId = profile.getProfileId();
                deleteConfirmProfileName = profile.getProfileName();
                showDeleteProfilePopup = true;
                needsRebuild = true;
            });
        }
    }

    private void openRenameProfilePopup(UUID profileId, String profileName) {
        renameProfileId = profileId;
        renameProfileName = profileName;
        showRenameProfilePopup = true;
        needsRebuild = true;
    }

    private void closeRenameProfilePopup() {
        if (!showRenameProfilePopup) {
            return;
        }
        showRenameProfilePopup = false;
        renameProfileId = null;
        renameProfileName = null;
        needsRebuild = true;
    }

    private void submitRenameProfile(String newName) {
        if (!showRenameProfilePopup || renameProfileId == null) {
            return;
        }
        String normalizedName = newName == null ? "" : newName.trim();
        if (normalizedName.isBlank()) {
            return;
        }
        String normalizedCurrentName = renameProfileName == null ? "" : renameProfileName.trim();
        if (ModConfig.profileNameExists(normalizedName) && !normalizedName.equalsIgnoreCase(normalizedCurrentName)) {
            return;
        }
        if (!ModConfig.renameProfile(renameProfileId, normalizedName)) {
            return;
        }
        showRenameProfilePopup = false;
        renameProfileId = null;
        renameProfileName = null;
        needsRebuild = true;
        handleProfilesChanged();
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
