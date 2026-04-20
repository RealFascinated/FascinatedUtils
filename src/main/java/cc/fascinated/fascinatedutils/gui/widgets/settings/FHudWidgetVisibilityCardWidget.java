package cc.fascinated.fascinatedutils.gui.widgets.settings;

import cc.fascinated.fascinatedutils.gui.core.Callback;
import cc.fascinated.fascinatedutils.systems.hud.HudModule;

public class FHudWidgetVisibilityCardWidget extends FVisibilityCardWidget<HudModule> {

    public FHudWidgetVisibilityCardWidget(HudModule widget, float layoutWidth, float layoutHeight, Callback<Boolean> onChange, Callback<HudModule> onOpenSettings) {
        super(layoutWidth, layoutHeight, widget::getName, () -> widget.getAllSettings() != null && !widget.getAllSettings().isEmpty(), () -> onOpenSettings.invoke(widget), widget::isEnabled, onChange);
    }

    public static float stackedCellOuterHeightPx() {
        return FVisibilityCardWidget.stackedCellOuterHeightPx();
    }
}
