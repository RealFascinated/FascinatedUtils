package cc.fascinated.fascinatedutils.gui.screens.impl.settings;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.minecraft.client.resources.language.I18n;

@Getter
@RequiredArgsConstructor
public enum ModuleSort {
    DEFAULT("alumite.setting.shell.sort_default"),
    LAST_USED("alumite.setting.shell.sort_last_used"),
    ENABLED_FIRST("alumite.setting.shell.sort_enabled_first");

    private final String translationKey;

    /**
     * Resolves the localized label for this sort.
     *
     * @return the translated display name
     */
    public String displayName() {
        return I18n.get(translationKey);
    }
}
