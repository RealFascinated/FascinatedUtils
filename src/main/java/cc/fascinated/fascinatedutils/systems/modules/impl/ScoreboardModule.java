package cc.fascinated.fascinatedutils.systems.modules.impl;

import cc.fascinated.fascinatedutils.common.setting.impl.BooleanSetting;
import cc.fascinated.fascinatedutils.systems.modules.Module;
import lombok.Getter;

@Getter
public class ScoreboardModule extends Module {
    private final BooleanSetting hideScoreboardLines = BooleanSetting.builder().id("hide_scoreboard_lines").defaultValue(false).build();

    public ScoreboardModule() {
        super("Scoreboard");
        addSetting(hideScoreboardLines);
    }
}
