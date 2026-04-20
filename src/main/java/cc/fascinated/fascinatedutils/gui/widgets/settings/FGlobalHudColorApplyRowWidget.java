package cc.fascinated.fascinatedutils.gui.widgets.settings;

import java.util.function.Consumer;

import cc.fascinated.fascinatedutils.common.setting.impl.ColorSetting;

public class FGlobalHudColorApplyRowWidget extends FColorSettingRowWidget {

    public FGlobalHudColorApplyRowWidget(ColorSetting staging, float outerWidth, float outerHeight, float valueColumnStartX, Runnable onApply, Consumer<ColorSetting> openColorPicker) {
        super(staging, outerWidth, outerHeight, onApply, valueColumnStartX, openColorPicker);
    }
}
