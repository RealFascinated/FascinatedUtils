package cc.fascinated.fascinatedutils.gui.screens.impl.settings;

import cc.fascinated.fascinatedutils.gui.components.ColorPickerController;
import cc.fascinated.fascinatedutils.gui.core.state.UiStateStore;
import cc.fascinated.fascinatedutils.systems.modules.Module;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;

/**
 * What the tabs of the settings shell share: state, geometry, the screen refresh hook and the color picker.
 */
@Getter
@RequiredArgsConstructor
public class SettingsTabContext {

    private final UiStateStore stateStore;
    private final SettingsState state;
    private final SettingsLayout layout;
    private final Runnable refresh;
    private final ColorPickerController colorPicker;

    /**
     * Explains why a module cannot be switched on.
     *
     * @param module the module to check
     * @return the restriction reason, or null when the module is not restricted
     */
    public @Nullable String restrictionReason(Module module) {
        return module.getActiveRestriction().map(restriction -> restriction.getReason().getString()).orElse(null);
    }
}
