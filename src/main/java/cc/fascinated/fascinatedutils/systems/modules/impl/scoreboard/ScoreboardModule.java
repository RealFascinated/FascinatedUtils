package cc.fascinated.fascinatedutils.systems.modules.impl.scoreboard;

import cc.fascinated.fascinatedutils.common.setting.impl.BooleanSetting;
import cc.fascinated.fascinatedutils.systems.hud.HudDefaults;
import cc.fascinated.fascinatedutils.systems.hud.HudHostModule;
import cc.fascinated.fascinatedutils.systems.hud.anchor.HUDWidgetAnchor;
import cc.fascinated.fascinatedutils.systems.modules.impl.scoreboard.hud.ScoreboardHudPanel;

public class ScoreboardModule extends HudHostModule {

    private final BooleanSetting hideScoreboardLines = BooleanSetting.builder().id("hide_scoreboard_lines").defaultValue(true).categoryDisplayKey(APPEARANCE_CATEGORY_DISPLAY_KEY).build();

    public ScoreboardModule() {
        super("scoreboard", "Scoreboard", HudDefaults.builder().defaultState(true).defaultAnchor(HUDWidgetAnchor.RIGHT).defaultXOffset(0).defaultYOffset(0).build());
        addSetting(hideScoreboardLines);
        addAppearanceSettings(false);
        registerHudPanel(new ScoreboardHudPanel(this));
    }

    public boolean scoreboardHideScoreLines() {
        return hideScoreboardLines.getValue();
    }
}

