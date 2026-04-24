package cc.fascinated.fascinatedutils.gui.modsettings;

import cc.fascinated.fascinatedutils.gui.core.Callback;
import cc.fascinated.fascinatedutils.systems.hud.HudModule;

public class FHudWidgetVisibilityCardWidget extends FVisibilityCardWidget<HudModule> {

    public FHudWidgetVisibilityCardWidget(HudModule widget, float layoutWidth, float layoutHeight, Callback<Boolean> onChange, Callback<HudModule> onOpenSettings) {
        super(widget, layoutWidth, layoutHeight, widget::getName, () -> !widget.getAllSettings().isEmpty(), () -> onOpenSettings.invoke(widget), widget::isEnabled, onChange);
    }
}
