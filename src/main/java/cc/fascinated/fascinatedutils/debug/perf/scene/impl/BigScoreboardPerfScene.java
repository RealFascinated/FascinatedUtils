package cc.fascinated.fascinatedutils.debug.perf.scene.impl;

import cc.fascinated.fascinatedutils.debug.perf.coverage.PerfCoverage;
import cc.fascinated.fascinatedutils.debug.perf.scene.PerfScene;
import cc.fascinated.fascinatedutils.debug.perf.scene.PerfSceneContext;
import cc.fascinated.fascinatedutils.systems.modules.impl.scoreboard.ScoreboardModule;

import java.util.List;
import java.util.Locale;

/**
 * A sidebar objective with fifteen entries, each in its own team with a long coloured prefix and suffix, whose scores
 * change every five ticks, so the restyled sidebar lays out and redraws long lines.
 */
public class BigScoreboardPerfScene extends PerfScene {
    public static final String ID = "big_scoreboard";
    private static final String OBJECTIVE = "alumite_big";
    private static final int ENTRY_COUNT = 15;
    private static final int UPDATE_EVERY_TICKS = 5;
    private static final String LONG_PREFIX = "Prefix text that runs long ";
    private static final String LONG_SUFFIX = " suffix text that also runs long";

    public BigScoreboardPerfScene() {
        super(ID, "Sidebar with 15 long team-styled entries whose scores change every 5 ticks");
    }

    @Override
    public void setup(PerfSceneContext context) {
        context.command("scoreboard objectives add " + OBJECTIVE + " dummy \"Big Scoreboard\"");
        context.command("scoreboard objectives setdisplay sidebar " + OBJECTIVE);
        for (int index = 0; index < ENTRY_COUNT; index++) {
            String team = teamName(index);
            context.command("team add " + team);
            context.command("team modify " + team + " prefix {\"text\":\"" + LONG_PREFIX + index + "\",\"color\":\"red\"}");
            context.command("team modify " + team + " suffix {\"text\":\"" + LONG_SUFFIX + index + "\",\"color\":\"green\"}");
            context.command("team join " + team + " " + entryName(index));
        }
    }

    @Override
    public void onTick(PerfSceneContext context, int tick) {
        if (tick % UPDATE_EVERY_TICKS != 0) {
            return;
        }
        for (int index = 0; index < ENTRY_COUNT; index++) {
            context.command("scoreboard players add " + entryName(index) + " " + OBJECTIVE + " 1");
        }
    }

    @Override
    public void teardown(PerfSceneContext context) {
        context.command("scoreboard objectives remove " + OBJECTIVE);
        for (int index = 0; index < ENTRY_COUNT; index++) {
            context.command("team remove " + teamName(index));
        }
    }

    @Override
    public List<String> covers() {
        return List.of(PerfCoverage.mixin("scoreboard.GuiMixin"), PerfCoverage.mixin("scoreboard.GuiScoreDisplayOrderAccessor"), PerfCoverage.module(ScoreboardModule.class));
    }

    private static String teamName(int index) {
        return String.format(Locale.ROOT, "perf_team_%02d", index);
    }

    private static String entryName(int index) {
        return String.format(Locale.ROOT, "perf_entry_%02d", index);
    }
}
