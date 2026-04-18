package cc.fascinated.fascinatedutils.gui.widgets.settings;

import cc.fascinated.fascinatedutils.gui.core.Callback;
import cc.fascinated.fascinatedutils.systems.modules.Module;

public class FModuleVisibilityCardWidget extends FVisibilityCardWidget<Module> {

    public FModuleVisibilityCardWidget(Module module, float layoutWidth, float layoutHeight, Callback<Module> onOpenSettings, Callback<Boolean> onEnabledChange) {
        super(layoutWidth, layoutHeight, module::getDisplayName, () -> !module.getAllSettings().isEmpty(), () -> onOpenSettings.invoke(module), module::isEnabled, onEnabledChange);
    }

    public static float stackedCellOuterHeightPx() {
        return FVisibilityCardWidget.stackedCellOuterHeightPx();
    }
}
