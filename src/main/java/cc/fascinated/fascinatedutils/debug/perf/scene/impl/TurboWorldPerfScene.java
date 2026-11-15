package cc.fascinated.fascinatedutils.debug.perf.scene.impl;

import cc.fascinated.fascinatedutils.debug.perf.coverage.PerfCoverage;
import cc.fascinated.fascinatedutils.debug.perf.scene.PerfScene;
import cc.fascinated.fascinatedutils.debug.perf.scene.PerfSceneContext;

import java.util.List;

/**
 * One Turbo workload per scene: rain, a field of displays in front of the camera, or named sheep in front of the camera.
 */
public class TurboWorldPerfScene extends PerfScene {
    public static final String ID_PREFIX = "turbo_";
    public static final String RAIN = "rain";
    public static final String DISPLAYS = "displays";
    public static final String NAMETAGS = "nametags";

    private static final int COLUMNS = 25;
    private static final int ROWS = 4;
    private static final double COLUMN_SPACING = 1.5;
    private static final double NAMETAG_COLUMN_SPACING = 1.0;
    private static final int ROW_SPACING = 4;
    private static final int NAMETAG_WALL_Z = 16;
    private static final int NAMETAG_START_Z = 20;

    private final String workload;

    public TurboWorldPerfScene(String workload, String description) {
        super(ID_PREFIX + workload, description);
        this.workload = workload;
    }

    @Override
    public void setup(PerfSceneContext context) {
        if (workload.equals(RAIN)) {
            context.command("weather rain 1200");
            return;
        }
        if (workload.equals(NAMETAGS)) {
            // Solid wall between the camera and the sheep. Bedrock only fills air, and the teardown removes it again.
            context.surfaceCommand(0, NAMETAG_WALL_Z, "fill ~-24 ~-3 ~ ~24 ~12 ~ bedrock keep");
        }
        for (int row = 0; row < ROWS; row++) {
            for (int column = 0; column < COLUMNS; column++) {
                double columnSpacing = workload.equals(NAMETAGS) ? NAMETAG_COLUMN_SPACING : COLUMN_SPACING;
                int offsetX = (int) ((column - COLUMNS / 2) * columnSpacing);
                if (workload.equals(DISPLAYS)) {
                    int offsetZ = 10 + row * ROW_SPACING;
                    context.surfaceCommand(offsetX, offsetZ, "summon block_display ~ ~1 ~ {block_state:{Name:\"minecraft:stone\"},width:1f,height:1f}");
                    context.surfaceCommand(offsetX, offsetZ + 1, "summon text_display ~ ~1 ~ {text:\"perf\",width:1f,height:1f}");
                    context.surfaceCommand(offsetX, offsetZ + 2, "summon item_display ~ ~1 ~ {item:{id:\"minecraft:diamond\",count:1},width:1f,height:1f}");
                } else {
                    int offsetZ = NAMETAG_START_Z + row * ROW_SPACING;
                    context.surfaceCommand(offsetX, offsetZ, "summon sheep ~ ~ ~ {NoAI:1b,Silent:1b,PersistenceRequired:1b,CustomName:\"perf\",CustomNameVisible:1b}");
                }
            }
        }
    }

    @Override
    public void teardown(PerfSceneContext context) {
        context.command("weather clear");
        context.command("kill @e[type=!player]");
        context.command("fill -24 0 " + NAMETAG_WALL_Z + " 24 319 " + NAMETAG_WALL_Z + " air replace bedrock");
    }

    @Override
    public List<String> covers() {
        return List.of(PerfCoverage.mixin("turboweather.WeatherEffectRendererMixin"), PerfCoverage.mixin("perf.WeatherPerfMixin"), PerfCoverage.system("turboweather"), PerfCoverage.system("turboentities"), PerfCoverage.mixin("turboentities.EntityRendererMixin"), PerfCoverage.mixin("turboentities.LevelRendererMixin"));
    }
}
