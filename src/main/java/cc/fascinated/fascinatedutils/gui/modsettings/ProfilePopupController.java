package cc.fascinated.fascinatedutils.gui.modsettings;

import cc.fascinated.fascinatedutils.gui.widgets.FAbsoluteStackWidget;
import cc.fascinated.fascinatedutils.systems.config.ModConfig;

import java.util.UUID;

/**
 * Encapsulates the profile delete/rename/context-menu popup state that is shared between
 * {@link FModulesTabElement} and {@link FProfilesTabElement}.
 *
 * <p>Callers supply three callbacks at construction time:
 * <ul>
 *   <li>{@code markDirty} – invoked whenever state changes and a rebuild is required</li>
 *   <li>{@code onProfilesChanged} – invoked after a profile is successfully deleted or renamed</li>
 *   <li>{@code onScrollReset} – invoked after a successful deletion so callers can reset scroll</li>
 * </ul>
 */
public class ProfilePopupController {
    private final Runnable markDirty;
    private final Runnable onProfilesChanged;
    private final Runnable onScrollReset;

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

    public ProfilePopupController(Runnable markDirty, Runnable onProfilesChanged, Runnable onScrollReset) {
        this.markDirty = markDirty;
        this.onProfilesChanged = onProfilesChanged;
        this.onScrollReset = onScrollReset;
    }

    /**
     * Returns a {@link ModSettingsProfilesTabBuilder.ProfileActionCallback} that opens the context
     * menu for a given profile. Pass this to the profile list builder.
     *
     * @return callback for opening the context menu
     */
    public ModSettingsProfilesTabBuilder.ProfileActionCallback contextMenuCallback() {
        return this::openContextMenu;
    }

    /**
     * Appends any currently-visible popup or context-menu overlay widgets to {@code stack}.
     *
     * @param stack the overlay stack to append children to
     */
    public void appendOverlaysTo(FAbsoluteStackWidget stack) {
        if (showDeleteProfilePopup && deleteConfirmProfileName != null) {
            stack.addChild(new FProfileDeletePopupWidget(deleteConfirmProfileName, this::closeDeleteProfilePopup, this::confirmDeleteProfile));
        }
        if (showRenameProfilePopup && renameProfileName != null) {
            stack.addChild(new FProfileRenamePopupWidget(renameProfileName, this::closeRenameProfilePopup, this::submitRenameProfile));
        }
        if (showContextMenu && contextMenuProfileId != null) {
            stack.addChild(new FProfileContextMenuWidget(contextMenuX, contextMenuY, this::closeContextMenu, this::handleContextMenuAction));
        }
    }

    /**
     * Resets all popup state. Call this from the owning tab element's {@code reset()} method.
     */
    public void reset() {
        showDeleteProfilePopup = false;
        deleteConfirmProfileId = null;
        deleteConfirmProfileName = null;
        showRenameProfilePopup = false;
        renameProfileId = null;
        renameProfileName = null;
        showContextMenu = false;
        contextMenuProfileId = null;
    }

    private void openContextMenu(UUID profileId, float positionX, float positionY) {
        if (showContextMenu) {
            return;
        }
        contextMenuProfileId = profileId;
        contextMenuX = positionX;
        contextMenuY = positionY;
        showContextMenu = true;
        markDirty.run();
    }

    private void closeContextMenu() {
        if (!showContextMenu) {
            return;
        }
        showContextMenu = false;
        contextMenuProfileId = null;
        markDirty.run();
    }

    private void handleContextMenuAction(ProfileContextMenuAction action) {
        UUID selectedProfileId = contextMenuProfileId;
        closeContextMenu();
        if (action == ProfileContextMenuAction.RENAME && selectedProfileId != null) {
            ModConfig.profiles().listProfiles().stream()
                    .filter(profile -> profile.getProfileId().equals(selectedProfileId))
                    .findFirst()
                    .ifPresent(profile -> {
                        renameProfileId = profile.getProfileId();
                        renameProfileName = profile.getProfileName();
                        showRenameProfilePopup = true;
                        markDirty.run();
                    });
        } else if (action == ProfileContextMenuAction.DELETE && selectedProfileId != null) {
            ModConfig.profiles().listProfiles().stream()
                    .filter(profile -> profile.getProfileId().equals(selectedProfileId))
                    .findFirst()
                    .ifPresent(profile -> {
                        deleteConfirmProfileId = profile.getProfileId();
                        deleteConfirmProfileName = profile.getProfileName();
                        showDeleteProfilePopup = true;
                        markDirty.run();
                    });
        }
    }

    private void closeDeleteProfilePopup() {
        if (!showDeleteProfilePopup) {
            return;
        }
        showDeleteProfilePopup = false;
        deleteConfirmProfileId = null;
        deleteConfirmProfileName = null;
        markDirty.run();
    }

    private void confirmDeleteProfile() {
        if (!showDeleteProfilePopup || deleteConfirmProfileId == null) {
            return;
        }
        UUID idToDelete = deleteConfirmProfileId;
        showDeleteProfilePopup = false;
        deleteConfirmProfileId = null;
        deleteConfirmProfileName = null;
        markDirty.run();
        if (ModConfig.profiles().deleteProfile(idToDelete)) {
            onScrollReset.run();
            onProfilesChanged.run();
        }
    }

    private void closeRenameProfilePopup() {
        if (!showRenameProfilePopup) {
            return;
        }
        showRenameProfilePopup = false;
        renameProfileId = null;
        renameProfileName = null;
        markDirty.run();
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
        if (ModConfig.profiles().profileNameExists(normalizedName) && !normalizedName.equalsIgnoreCase(normalizedCurrentName)) {
            return;
        }
        if (!ModConfig.profiles().renameProfile(renameProfileId, normalizedName)) {
            return;
        }
        showRenameProfilePopup = false;
        renameProfileId = null;
        renameProfileName = null;
        markDirty.run();
        onProfilesChanged.run();
    }
}
