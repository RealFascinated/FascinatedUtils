package cc.fascinated.fascinatedutils.gui.screens.impl.settings;

import cc.fascinated.fascinatedutils.gui.core.state.UiState;
import cc.fascinated.fascinatedutils.gui.core.state.UiStateStore;
import cc.fascinated.fascinatedutils.systems.modules.Module;
import cc.fascinated.fascinatedutils.systems.modules.ModuleCategory;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;

import java.util.UUID;

/**
 * Typed access to the settings shell's UI state, so every tab reads and writes the same keys.
 */
@RequiredArgsConstructor
public class SettingsState {

    private final UiStateStore store;
    private final @Nullable Module initialModule;
    private final String initialTab;

    public UiState<String> tab() {
        return store.state("tab", initialTab);
    }

    public UiState<Module> openModule() {
        return store.state("module", initialModule);
    }

    public UiState<Integer> revision() {
        return store.state("revision", 0);
    }

    public UiState<String> moduleSearch() {
        return store.state("modules.search", "");
    }

    public UiState<ModuleCategory> moduleFilter() {
        return store.state("modules.filter", (ModuleCategory) null);
    }

    public UiState<String> detailSearch() {
        return store.state("detail.search", "");
    }

    public UiState<Boolean> moduleReset() {
        return store.state("module.reset", false);
    }

    public UiState<Boolean> profileCreate() {
        return store.state("profile.create", false);
    }

    public UiState<UUID> profileRename() {
        return store.state("profile.rename", (UUID) null);
    }

    public UiState<UUID> profileDelete() {
        return store.state("profile.delete", (UUID) null);
    }

    public UiState<String> profileName() {
        return store.state("profile.name", "");
    }

    /**
     * Whether any profile popup is open.
     *
     * @return true while the create, rename or delete popup is shown
     */
    public boolean profilePopupOpen() {
        return profileCreate().get() || profileRename().get() != null || profileDelete().get() != null;
    }

    /**
     * Closes every profile popup.
     */
    public void closeProfilePopups() {
        profileCreate().set(false);
        profileRename().set(null);
        profileDelete().set(null);
    }
}
