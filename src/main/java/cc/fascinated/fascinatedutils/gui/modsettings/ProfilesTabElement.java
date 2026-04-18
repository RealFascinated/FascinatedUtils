package cc.fascinated.fascinatedutils.gui.modsettings;

import cc.fascinated.fascinatedutils.gui.core.Ref;
import cc.fascinated.fascinatedutils.gui.renderer.UIRenderer;
import cc.fascinated.fascinatedutils.gui.widgets.FAbsoluteStackWidget;
import cc.fascinated.fascinatedutils.gui.widgets.FWidget;
import cc.fascinated.fascinatedutils.systems.config.ModConfig;

import java.util.UUID;

public class ProfilesTabElement extends FWidget {
    private final Ref<Float> profilesScrollRef = Ref.of(0f);
    private final Ref<String> profileNameInputRef = Ref.of("");
    private final Runnable onProfilesChanged;
    private FWidget inner;
    private boolean dirty = true;
    private float cachedWidth = -1f;
    private float cachedHeight = -1f;
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

    public ProfilesTabElement(Runnable onProfilesChanged) {
        this.onProfilesChanged = onProfilesChanged;
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
        if (dirty || cachedWidth != layoutWidth || cachedHeight != layoutHeight || inner == null) {
            FWidget profilesPane = ModSettingsProfilesTabBuilder.buildProfilesTab(layoutWidth, layoutHeight, profilesScrollRef, profileNameInputRef, this::openContextMenu, this::handleProfilesChanged);
            FAbsoluteStackWidget stack = new FAbsoluteStackWidget();
            stack.addChild(profilesPane);
            if (showDeleteProfilePopup && deleteConfirmProfileName != null) {
                stack.addChild(new ProfileDeletePopupWidget(deleteConfirmProfileName, this::closeDeleteProfilePopup, this::confirmDeleteProfile));
            }
            if (showRenameProfilePopup && renameProfileName != null) {
                stack.addChild(new ProfileRenamePopupWidget(renameProfileName, this::closeRenameProfilePopup, this::submitRenameProfile));
            }
            if (showContextMenu && contextMenuProfileId != null) {
                stack.addChild(new ProfileContextMenuWidget(contextMenuX, contextMenuY, this::closeContextMenu, this::handleContextMenuAction));
            }
            inner = stack;
            clearChildren();
            addChild(inner);
            cachedWidth = layoutWidth;
            cachedHeight = layoutHeight;
            dirty = false;
        }
        if (inner != null) {
            inner.layout(measure, layoutX, layoutY, layoutWidth, layoutHeight);
        }
    }

    public void reset() {
        profilesScrollRef.setValue(0f);
        profileNameInputRef.setValue("");
        inner = null;
        dirty = true;
        cachedWidth = -1f;
        cachedHeight = -1f;
        showDeleteProfilePopup = false;
        deleteConfirmProfileId = null;
        deleteConfirmProfileName = null;
        showRenameProfilePopup = false;
        renameProfileId = null;
        renameProfileName = null;
        showContextMenu = false;
        contextMenuProfileId = null;
        clearChildren();
    }

    private void openContextMenu(UUID profileId, float x, float y) {
        if (showContextMenu) {
            return;
        }
        contextMenuProfileId = profileId;
        contextMenuX = x;
        contextMenuY = y;
        showContextMenu = true;
        dirty = true;
    }

    private void closeContextMenu() {
        if (!showContextMenu) {
            return;
        }
        showContextMenu = false;
        contextMenuProfileId = null;
        dirty = true;
    }

    private void handleContextMenuAction(String action) {
        UUID selectedProfileId = contextMenuProfileId;
        closeContextMenu();
        if ("rename".equals(action) && selectedProfileId != null) {
            ModConfig.listProfiles().stream().filter(profile -> profile.getProfileId().equals(selectedProfileId)).findFirst().ifPresent(profile -> {
                renameProfileId = profile.getProfileId();
                renameProfileName = profile.getProfileName();
                showRenameProfilePopup = true;
                dirty = true;
            });
        }
        else if ("delete".equals(action) && selectedProfileId != null) {
            ModConfig.listProfiles().stream().filter(profile -> profile.getProfileId().equals(selectedProfileId)).findFirst().ifPresent(profile -> {
                deleteConfirmProfileId = profile.getProfileId();
                deleteConfirmProfileName = profile.getProfileName();
                showDeleteProfilePopup = true;
                dirty = true;
            });
        }
    }

    private void openDeleteProfilePopup(UUID profileId) {
        if (showDeleteProfilePopup) {
            return;
        }
        ModConfig.listProfiles().stream().filter(profile -> profile.getProfileId().equals(profileId)).findFirst().ifPresent(profile -> {
            deleteConfirmProfileId = profileId;
            deleteConfirmProfileName = profile.getProfileName();
            showDeleteProfilePopup = true;
            dirty = true;
        });
    }

    private void closeDeleteProfilePopup() {
        if (!showDeleteProfilePopup) {
            return;
        }
        showDeleteProfilePopup = false;
        deleteConfirmProfileId = null;
        deleteConfirmProfileName = null;
        dirty = true;
    }

    private void confirmDeleteProfile() {
        if (!showDeleteProfilePopup || deleteConfirmProfileId == null) {
            return;
        }
        UUID idToDelete = deleteConfirmProfileId;
        showDeleteProfilePopup = false;
        deleteConfirmProfileId = null;
        deleteConfirmProfileName = null;
        dirty = true;
        if (ModConfig.deleteProfile(idToDelete)) {
            profilesScrollRef.setValue(0f);
            handleProfilesChanged();
        }
    }

    private void closeRenameProfilePopup() {
        if (!showRenameProfilePopup) {
            return;
        }
        showRenameProfilePopup = false;
        renameProfileId = null;
        renameProfileName = null;
        dirty = true;
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
        dirty = true;
        handleProfilesChanged();
    }

    private void handleProfilesChanged() {
        dirty = true;
        onProfilesChanged.run();
    }
}
