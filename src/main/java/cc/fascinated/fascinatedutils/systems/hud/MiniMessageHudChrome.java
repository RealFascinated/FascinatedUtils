package cc.fascinated.fascinatedutils.systems.hud;

import cc.fascinated.fascinatedutils.common.setting.impl.BooleanSetting;
import cc.fascinated.fascinatedutils.common.setting.impl.ColorSetting;
import cc.fascinated.fascinatedutils.common.setting.impl.SliderSetting;

public final class MiniMessageHudChrome {
    private MiniMessageHudChrome() {
    }

    /**
     * Registers standard MiniMessage HUD chrome (panel background, corners, border, colors, remove-min-width).
     *
     * @param module host registering the HUD panel
     */
    public static void register(HudHostModule module) {
        BooleanSetting showBackground = HudWidgetAppearanceBuilders.showBackground().build();
        BooleanSetting roundedCorners = HudWidgetAppearanceBuilders.roundedCorners().build();
        SliderSetting roundingRadius = HudWidgetAppearanceBuilders.roundingRadius().build();
        BooleanSetting showBorder = HudWidgetAppearanceBuilders.showBorder().build();
        SliderSetting borderThickness = HudWidgetAppearanceBuilders.borderThickness().build();
        ColorSetting backgroundColor = HudWidgetAppearanceBuilders.backgroundColor().build();
        ColorSetting borderColor = HudWidgetAppearanceBuilders.borderColor().build();
        BooleanSetting removeMinimumWidth = HudWidgetAppearanceBuilders.removeMinimumWidth().build();

        module.addSetting(showBackground);
        module.addSetting(roundedCorners);
        module.addSetting(showBorder);
        module.addSetting(roundingRadius);
        module.addSetting(borderThickness);
        module.addSetting(backgroundColor);
        module.addSetting(borderColor);
        showBackground.addSubSetting(backgroundColor);
        roundedCorners.addSubSetting(roundingRadius);
        showBorder.addSubSetting(borderThickness);
        showBorder.addSubSetting(borderColor);
        module.addSetting(removeMinimumWidth);
    }
}
