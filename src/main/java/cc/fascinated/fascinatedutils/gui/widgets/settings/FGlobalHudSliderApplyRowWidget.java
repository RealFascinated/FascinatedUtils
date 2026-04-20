package cc.fascinated.fascinatedutils.gui.widgets.settings;

import cc.fascinated.fascinatedutils.common.setting.impl.SliderSetting;

public class FGlobalHudSliderApplyRowWidget extends FSliderSettingRowWidget {

    public FGlobalHudSliderApplyRowWidget(SliderSetting staging, float outerWidth, float outerHeight, float valueColumnStartX, Runnable onApply) {
        super(staging, outerWidth, outerHeight, onApply, valueColumnStartX);
    }
}
