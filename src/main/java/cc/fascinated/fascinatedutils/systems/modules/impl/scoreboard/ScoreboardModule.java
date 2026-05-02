package cc.fascinated.fascinatedutils.systems.modules.impl.scoreboard;

import cc.fascinated.fascinatedutils.common.color.SettingColor;
import cc.fascinated.fascinatedutils.common.setting.impl.BooleanSetting;
import cc.fascinated.fascinatedutils.common.setting.impl.ColorSetting;
import cc.fascinated.fascinatedutils.common.setting.impl.SliderSetting;
import cc.fascinated.fascinatedutils.systems.modules.impl.scoreboard.hud.ScoreboardHudPanel;
import cc.fascinated.fascinatedutils.systems.hud.anchor.HUDWidgetAnchor;
import cc.fascinated.fascinatedutils.systems.hud.HudDefaults;
import cc.fascinated.fascinatedutils.systems.hud.HudHostModule;
import cc.fascinated.fascinatedutils.systems.hud.HudWidgetAppearanceBuilders;

public class ScoreboardModule extends HudHostModule {

    private final BooleanSetting hideScoreboardLines = BooleanSetting.builder().id("hide_scoreboard_lines").defaultValue(true).categoryDisplayKey(APPEARANCE_CATEGORY_DISPLAY_KEY).build();
    private final BooleanSetting showBackground = HudWidgetAppearanceBuilders.showBackground().build();
    private final ColorSetting backgroundColor = HudWidgetAppearanceBuilders.backgroundColor().defaultValue(SettingColor.fromArgb(0x55000000)).build();
    private final BooleanSetting roundedCorners = HudWidgetAppearanceBuilders.roundedCorners().defaultValue(true).build();
    private final SliderSetting roundingRadius = HudWidgetAppearanceBuilders.roundingRadius().build();
    private final BooleanSetting showBorder = HudWidgetAppearanceBuilders.showBorder().build();
    private final SliderSetting borderThickness = HudWidgetAppearanceBuilders.borderThickness().build();
    private final ColorSetting borderColor = HudWidgetAppearanceBuilders.borderColor().build();

    public ScoreboardModule() {
        super("scoreboard", "Scoreboard", HudDefaults.builder().defaultState(true).defaultAnchor(HUDWidgetAnchor.RIGHT).defaultXOffset(0).defaultYOffset(0).build());
        addSetting(hideScoreboardLines);
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
        registerHudPanel(new ScoreboardHudPanel(this));
    }

    public boolean scoreboardHideScoreLines() {
        return hideScoreboardLines.getValue();
    }
}

