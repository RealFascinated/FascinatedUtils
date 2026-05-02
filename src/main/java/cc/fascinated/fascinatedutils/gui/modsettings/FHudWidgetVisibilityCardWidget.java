package cc.fascinated.fascinatedutils.gui.modsettings;

import cc.fascinated.fascinatedutils.gui.core.Callback;
import cc.fascinated.fascinatedutils.systems.hud.HudPanel;
import cc.fascinated.fascinatedutils.systems.modules.ModuleRegistry;

public class FHudWidgetVisibilityCardWidget extends FVisibilityCardWidget<HudPanel> {

    public FHudWidgetVisibilityCardWidget(HudPanel panel, float layoutWidth, float layoutHeight, Callback<Boolean> onChange, Callback<HudPanel> onOpenSettings) {
        super(
                panel,
                layoutWidth,
                layoutHeight,
                panel::getName,
                () -> !panel.hudHostModule().getAllSettings().isEmpty(),
                () -> onOpenSettings.invoke(panel),
                () -> panel.hudHostModule().isEnabled(),
                enabled -> {
                    ModuleRegistry.INSTANCE.setModuleEnabled(panel.hudHostModule(), enabled);
                    onChange.invoke(enabled);
                });
    }
}
