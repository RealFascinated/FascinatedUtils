package cc.fascinated.fascinatedutils.systems.modules.impl.clock;

import cc.fascinated.fascinatedutils.common.setting.impl.BooleanSetting;
import cc.fascinated.fascinatedutils.common.setting.impl.ColorSetting;
import cc.fascinated.fascinatedutils.common.setting.impl.EnumSetting;
import cc.fascinated.fascinatedutils.common.setting.impl.SliderSetting;
import cc.fascinated.fascinatedutils.systems.hud.HudDefaults;
import cc.fascinated.fascinatedutils.systems.hud.HudHostModule;
import cc.fascinated.fascinatedutils.systems.hud.HudWidgetAppearanceBuilders;
import cc.fascinated.fascinatedutils.systems.modules.impl.clock.hud.ClockHudPanel;
import lombok.AllArgsConstructor;
import lombok.Getter;

public class ClockWidget extends HudHostModule {

    private final EnumSetting<ClockFormat> clockFormat = EnumSetting.<ClockFormat>builder().id("clock_format").defaultValue(ClockFormat.DMY_24H).valueFormatter(ClockFormat::getFormat).categoryDisplayKey(APPEARANCE_CATEGORY_DISPLAY_KEY).build();

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

    public ClockWidget() {
        super("clock", "Clock", HudDefaults.builder().build());
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
        addSetting(clockFormat);
        registerHudPanel(new ClockHudPanel(this));
    }

    public EnumSetting<ClockFormat> clockFormatSetting() {
        return clockFormat;
    }

    @Getter
    @AllArgsConstructor
    public enum ClockFormat {
        DMY_12H("dd MMM yyyy, hh:mm a"), DMY_24H("dd MMM yyyy, HH:mm"), MDY_12H("MMM dd, yyyy hh:mm a"), MDY_24H("MMM dd, yyyy HH:mm"), ISO_24H("yyyy-MM-dd HH:mm"), ISO_SECONDS("yyyy-MM-dd HH:mm:ss"), DMY_SHORT("dd/MM/yyyy HH:mm"), TIME_ONLY_12H("hh:mm a"), TIME_ONLY_24H("HH:mm");
        private final String format;
    }
}
