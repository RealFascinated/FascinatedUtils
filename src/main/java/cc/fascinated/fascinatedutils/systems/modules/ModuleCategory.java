package cc.fascinated.fascinatedutils.systems.modules;

import lombok.AllArgsConstructor;
import net.minecraft.client.resources.language.I18n;

@AllArgsConstructor
public enum ModuleCategory {
    GENERAL("alumite.module.category.general"),
    HUD("alumite.module.category.hud");

    private final String translationKey;

    public String getDisplayName() {
        return I18n.get(translationKey);
    }
}
