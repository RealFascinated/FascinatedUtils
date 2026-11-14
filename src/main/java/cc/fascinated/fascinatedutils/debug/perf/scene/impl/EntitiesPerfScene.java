package cc.fascinated.fascinatedutils.debug.perf.scene.impl;

import cc.fascinated.fascinatedutils.debug.perf.coverage.PerfCoverage;
import cc.fascinated.fascinatedutils.debug.perf.scene.PerfScene;
import cc.fascinated.fascinatedutils.debug.perf.scene.PerfSceneContext;
import cc.fascinated.fascinatedutils.systems.turboentities.TurboEntities;

import java.util.List;

/**
 * A grid of sheep and armor stands plus signs, chests and item frames in front of the camera, so entity and block
 * entity culling and rendering do real work.
 */
public class EntitiesPerfScene extends PerfScene {
    private static final int GRID_SIZE = 10;
    private static final int GRID_SPACING = 3;
    private static final int GRID_CENTER = 5;
    private static final int GRID_START_Z = 8;

    public EntitiesPerfScene() {
        super("entities", "100 sheep, 100 armor stands and a row of signs, chests and item frames");
    }

    @Override
    public void setup(PerfSceneContext context) {
        for (int row = 0; row < GRID_SIZE; row++) {
            for (int column = 0; column < GRID_SIZE; column++) {
                int offsetX = (column - GRID_CENTER) * GRID_SPACING;
                int offsetZ = GRID_START_Z + row * GRID_SPACING;
                context.surfaceCommand(offsetX, offsetZ, "summon sheep ~ ~ ~ {NoAI:1b,Silent:1b,PersistenceRequired:1b}");
                context.surfaceCommand(offsetX + 1, offsetZ + 1, "summon armor_stand ~ ~ ~ {NoGravity:1b,Silent:1b}");
            }
        }
        for (int column = 0; column < GRID_SIZE; column++) {
            int offsetX = (column - GRID_CENTER) * GRID_SPACING;
            context.surfaceCommand(offsetX, GRID_START_Z - 2, "setblock ~ ~ ~ oak_sign{front_text:{messages:[\"perf\",\"sign\",\"\",\"\"]}}");
            context.surfaceCommand(offsetX + 1, GRID_START_Z - 2, "setblock ~ ~ ~ chest");
            context.surfaceCommand(offsetX + 2, GRID_START_Z - 2, "summon item_frame ~ ~ ~ {Facing:1b,Item:{id:\"minecraft:diamond\",count:1}}");
        }
    }

    @Override
    public void teardown(PerfSceneContext context) {
        context.command("kill @e[type=!player]");
        for (int column = 0; column < GRID_SIZE; column++) {
            int offsetX = (column - GRID_CENTER) * GRID_SPACING;
            context.surfaceCommand(offsetX, GRID_START_Z - 2, "fill ~ ~-1 ~ ~2 ~ ~ air replace #minecraft:all_signs");
            context.surfaceCommand(offsetX + 1, GRID_START_Z - 2, "fill ~ ~-1 ~ ~ ~ ~ air replace chest");
        }
    }

    @Override
    public List<String> covers() {
        return List.of(PerfCoverage.mixin("turboentities.BlockEntityRenderDispatcherMixin"), PerfCoverage.mixin("turboentities.ClientLevelTickMixin"), PerfCoverage.mixin("turboentities.CullableMixin"), PerfCoverage.mixin("turboentities.EntityRendererMixin"), PerfCoverage.mixin("turboentities.ItemFrameBackCullingMixin"), PerfCoverage.mixin("turboentities.LevelRendererMixin"), PerfCoverage.mixin("turboentities.SignBackCullingMixin"), PerfCoverage.mixin("LivingEntityRendererMixin"), PerfCoverage.system("turboentities"), PerfCoverage.listener(TurboEntities.class), PerfCoverage.common("culling"));
    }
}
