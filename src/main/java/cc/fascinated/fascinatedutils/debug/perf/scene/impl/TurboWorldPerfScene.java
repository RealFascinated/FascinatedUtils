package cc.fascinated.fascinatedutils.debug.perf.scene.impl;

import cc.fascinated.fascinatedutils.debug.perf.coverage.PerfCoverage;
import cc.fascinated.fascinatedutils.debug.perf.scene.PerfScene;
import cc.fascinated.fascinatedutils.debug.perf.scene.PerfSceneContext;
import cc.fascinated.fascinatedutils.systems.turboentities.TurboEntities;

import java.util.List;

/**
 * One Turbo workload per scene: rain, a field of displays in front of the camera, or named sheep in front of the camera.
 */
public class TurboWorldPerfScene extends PerfScene {
    public static final String RAIN = "rain";
    public static final String DISPLAYS = "displays";
    public static final String NAMETAGS = "nametags";

    private static final int COLUMNS = 25;
    private static final int ROWS = 4;
    private static final double COLUMN_SPACING = 1.5;
    private static final int ROW_SPACING = 4;

    private final String workload;

    public TurboWorldPerfScene(String workload, String description) {
        super("turbo_" + workload, description);
        this.workload = workload;
    }

    @Override
    public void setup(PerfSceneContext context) {
        if (workload.equals(RAIN)) {
            context.command("weather rain 1200");
            return;
        }
        for (int row = 0; row < ROWS; row++) {
            for (int column = 0; column < COLUMNS; column++) {
                int offsetX = (int) ((column - COLUMNS / 2) * COLUMN_SPACING);
                if (workload.equals(DISPLAYS)) {
                    int offsetZ = 10 + row * ROW_SPACING;
                    context.surfaceCommand(offsetX, offsetZ, "summon block_display ~ ~1 ~ {block_state:{Name:\"minecraft:stone\"}}");
                    context.surfaceCommand(offsetX, offsetZ + 1, "summon text_display ~ ~1 ~ {text:\"perf\"}");
                    context.surfaceCommand(offsetX, offsetZ + 2, "summon item_display ~ ~1 ~ {item:{id:\"minecraft:diamond\",count:1}}");
                } else {
                    int offsetZ = 10 + row * ROW_SPACING * 2;
                    context.surfaceCommand(offsetX, offsetZ, "summon sheep ~ ~ ~ {NoAI:1b,Silent:1b,PersistenceRequired:1b,CustomName:\"perf\",CustomNameVisible:1b}");
                }
            }
        }
    }

    @Override
    public void teardown(PerfSceneContext context) {
        context.command("weather clear");
        context.command("kill @e[type=!player]");
    }

    @Override
    public List<String> covers() {
        return List.of(PerfCoverage.mixin("turboweather.WeatherEffectRendererMixin"), PerfCoverage.mixin("perf.WeatherPerfMixin"), PerfCoverage.system("turboweather"), PerfCoverage.system("turboentities"), PerfCoverage.mixin("turboentities.EntityRendererMixin"), PerfCoverage.mixin("turboentities.LevelRendererMixin"));
    }
}
