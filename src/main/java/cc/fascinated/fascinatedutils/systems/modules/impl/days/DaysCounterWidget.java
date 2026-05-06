package cc.fascinated.fascinatedutils.systems.modules.impl.days;

import cc.fascinated.fascinatedutils.common.setting.impl.BooleanSetting;
import cc.fascinated.fascinatedutils.common.setting.impl.ColorSetting;
import cc.fascinated.fascinatedutils.common.setting.impl.SliderSetting;
import cc.fascinated.fascinatedutils.systems.hud.HudDefaults;
import cc.fascinated.fascinatedutils.systems.hud.HudHostModule;
import cc.fascinated.fascinatedutils.systems.hud.HudWidgetAppearanceBuilders;
import cc.fascinated.fascinatedutils.systems.modules.impl.days.hud.DaysHudPanel;

public class DaysCounterWidget extends HudHostModule {

    private final BooleanSetting showBackground = HudWidgetAppearanceBuilders.showBackground().build();
    private final BooleanSetting roundedCorners = HudWidgetAppearanceBuilders.roundedCorners().build();
    private final SliderSetting roundingRadius = HudWidgetAppearanceBuilders.roundingRadius().build();
    private final BooleanSetting showBorder = HudWidgetAppearanceBuilders.showBorder().build();
    private final SliderSetting borderThickness = HudWidgetAppearanceBuilders.borderThickness().build();
    private final ColorSetting backgroundColor = HudWidgetAppearanceBuilders.backgroundColor().build();
    private final ColorSetting borderColor = HudWidgetAppearanceBuilders.borderColor().build();
    private final BooleanSetting removeMinimumWidth = HudWidgetAppearanceBuilders.removeMinimumWidth().build();
    private final SliderSetting padding = HudWidgetAppearanceBuilders.padding().build();
    private final BooleanSetting textShadow = HudWidgetAppearanceBuilders.textShadow().build();

    public DaysCounterWidget() {
        super("days_counter", "Days Counter", HudDefaults.builder().build());
        addSetting(showBackground);
        addSetting(roundedCorners);
        addSetting(showBorder);
        addSetting(roundingRadius);
        addSetting(borderThickness);
        addSetting(backgroundColor);
        addSetting(borderColor);
        showBackground.addSubSetting(backgroundColor);
        roundedCorners.addSubSetting(roundingRadius);
        showBorder.addSubSetting(borderThickness);
        showBorder.addSubSetting(borderColor);
        addSetting(removeMinimumWidth);
        addSetting(padding);
        addSetting(textShadow);
        registerHudPanel(new DaysHudPanel(this));
    }
}
