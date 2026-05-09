package cc.fascinated.fascinatedutils.oldgui.modsettings;

import cc.fascinated.fascinatedutils.oldgui.core.Callback;
import cc.fascinated.fascinatedutils.systems.modules.Module;

public class FModuleVisibilityCardWidget extends FVisibilityCardWidget<Module> {

    public FModuleVisibilityCardWidget(Module module, float layoutWidth, float layoutHeight, Callback<Module> onOpenSettings, Callback<Boolean> onEnabledChange) {
        super(layoutWidth, layoutHeight, module::getDisplayName, () -> !module.getAllSettings().isEmpty(), () -> onOpenSettings.invoke(module), module::isEnabled, onEnabledChange);
    }

    public static float stackedCellOuterHeightPx() {
        return FVisibilityCardWidget.stackedCellOuterHeightPx();
    }
}
