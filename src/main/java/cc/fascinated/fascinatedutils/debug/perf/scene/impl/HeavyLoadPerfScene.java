package cc.fascinated.fascinatedutils.debug.perf.scene.impl;

import cc.fascinated.fascinatedutils.debug.perf.coverage.PerfCoverage;
import cc.fascinated.fascinatedutils.debug.perf.scene.PerfScene;
import cc.fascinated.fascinatedutils.debug.perf.scene.PerfSceneContext;

import java.util.List;

/**
 * A server-sized crowd: 2000 mixed peaceful mobs in view, rain, and very large flame and cloud bursts every tick, so the
 * vanilla entity, particle and weather paths are measured under heavy load.
 */
public class HeavyLoadPerfScene extends PerfScene {
    public static final String ID = "heavy_load";
    private static final int COLUMNS = 50;
    private static final int ROWS = 40;
    private static final double SPACING = 1.0;
    private static final int FIRST_ROW_Z = 6;
    private static final int BURST_Z = 4;
    private static final int BURST_EVERY_TICKS = 1;
    private static final List<String> MOB_TYPES = List.of("sheep", "cow", "pig", "chicken", "villager", "horse", "wolf", "rabbit");

    public HeavyLoadPerfScene() {
        super(ID, "2000 mixed peaceful mobs in view, rain and very large particle bursts every tick");
    }

    @Override
    public void setup(PerfSceneContext context) {
        context.command("weather rain 1200");
        for (int row = 0; row < ROWS; row++) {
            for (int column = 0; column < COLUMNS; column++) {
                int offsetX = (int) ((column - COLUMNS / 2) * SPACING);
                int offsetZ = FIRST_ROW_Z + (int) (row * SPACING);
                String mobType = MOB_TYPES.get((row * COLUMNS + column) % MOB_TYPES.size());
                context.surfaceCommand(offsetX, offsetZ, "summon " + mobType + " ~ ~ ~ {NoAI:1b,Silent:1b,Invulnerable:1b,PersistenceRequired:1b}");
            }
        }
    }

    @Override
    public void onTick(PerfSceneContext context, int tick) {
        if (tick % BURST_EVERY_TICKS == 0) {
            context.surfaceCommand(0, BURST_Z, "particle minecraft:flame ~ ~3 ~ 8 4 8 0.03 500 force");
            context.surfaceCommand(0, BURST_Z, "particle minecraft:cloud ~ ~3 ~ 8 4 8 0.03 400 force");
        }
    }

    @Override
    public void teardown(PerfSceneContext context) {
        context.command("weather clear");
        context.command("kill @e[type=!player]");
    }

    @Override
    public List<String> covers() {
        return List.of(PerfCoverage.system("turboentities"), PerfCoverage.system("turboparticles"), PerfCoverage.system("turboweather"));
    }
}
