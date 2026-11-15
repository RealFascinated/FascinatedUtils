package cc.fascinated.fascinatedutils.debug.perf.scene.impl;

import cc.fascinated.fascinatedutils.debug.perf.coverage.PerfCoverage;
import cc.fascinated.fascinatedutils.debug.perf.scene.PerfScene;
import cc.fascinated.fascinatedutils.debug.perf.scene.PerfSceneContext;
import cc.fascinated.fascinatedutils.debug.perf.scene.PerfScenario;
import cc.fascinated.fascinatedutils.systems.turboentities.TurboEntities;

import java.util.List;

/**
 * A grid of sheep and armor stands plus signs, chests and item frames in front of the camera, so entity and block
 * entity culling and rendering do real work. The grid is square; its side length sets the density.
 */
public class EntitiesPerfScene extends PerfScene {
    public static final String ID = "entities";
    public static final int GRID_SIZE = 10;

    private static final int GRID_SPACING = 3;
    private static final int GRID_START_Z = 8;
    private static final int SETUP_COMMANDS_PER_TICK = 4;
    private static final int SETTLE_MARGIN_TICKS = 40;

    private final int gridSize;

    /**
     * Creates a grid scene.
     *
     * @param id       the scene id
     * @param gridSize the grid side length in entity cells
     */
    public EntitiesPerfScene(String id, int gridSize) {
        super(id, gridSize * gridSize + " sheep, " + gridSize * gridSize + " armor stands and a row of signs, chests and item frames");
        this.gridSize = gridSize;
    }

    @Override
    public void setup(PerfSceneContext context) {
        for (int row = 0; row < gridSize; row++) {
            for (int column = 0; column < gridSize; column++) {
                int offsetX = (column - gridSize / 2) * GRID_SPACING;
                int offsetZ = GRID_START_Z + row * GRID_SPACING;
                context.surfaceCommand(offsetX, offsetZ, "summon sheep ~ ~ ~ {NoAI:1b,Silent:1b,PersistenceRequired:1b}");
                context.surfaceCommand(offsetX + 1, offsetZ + 1, "summon armor_stand ~ ~ ~ {NoGravity:1b,Silent:1b}");
            }
        }
        for (int column = 0; column < gridSize; column++) {
            int offsetX = (column - gridSize / 2) * GRID_SPACING;
            context.surfaceCommand(offsetX, GRID_START_Z - 2, "setblock ~ ~ ~ oak_sign{front_text:{messages:[\"perf\",\"sign\",\"\",\"\"]}}");
            context.surfaceCommand(offsetX + 1, GRID_START_Z - 2, "setblock ~ ~ ~ chest");
            context.surfaceCommand(offsetX + 2, GRID_START_Z - 2, "summon item_frame ~ ~ ~ {Facing:1b,Item:{id:\"minecraft:diamond\",count:1}}");
        }
    }

    /**
     * Waits until every queued setup command has been sent, since the context sends a few commands per tick.
     *
     * @param scenario the running scenario
     * @return ticks to wait, never fewer than the scenario's own settle time
     */
    @Override
    public int settleTicks(PerfScenario scenario) {
        int setupCommands = gridSize * gridSize * 2 + gridSize * 3;
        int setupTicks = (setupCommands + SETUP_COMMANDS_PER_TICK - 1) / SETUP_COMMANDS_PER_TICK + SETTLE_MARGIN_TICKS;
        return Math.max(super.settleTicks(scenario), setupTicks);
    }

    @Override
    public void teardown(PerfSceneContext context) {
        context.command("kill @e[type=!player]");
        for (int column = 0; column < gridSize; column++) {
            int offsetX = (column - gridSize / 2) * GRID_SPACING;
            context.surfaceCommand(offsetX, GRID_START_Z - 2, "fill ~ ~-1 ~ ~2 ~ ~ air replace #minecraft:all_signs");
            context.surfaceCommand(offsetX + 1, GRID_START_Z - 2, "fill ~ ~-1 ~ ~ ~ ~ air replace chest");
        }
    }

    @Override
    public List<String> covers() {
        return List.of(PerfCoverage.mixin("turboentities.BlockEntityRenderDispatcherMixin"), PerfCoverage.mixin("turboentities.ClientLevelTickMixin"), PerfCoverage.mixin("turboentities.CullableMixin"), PerfCoverage.mixin("turboentities.EntityRendererMixin"), PerfCoverage.mixin("turboentities.ItemFrameBackCullingMixin"), PerfCoverage.mixin("turboentities.LevelRendererMixin"), PerfCoverage.mixin("turboentities.SignBackCullingMixin"), PerfCoverage.mixin("LivingEntityRendererMixin"), PerfCoverage.system("turboentities"), PerfCoverage.listener(TurboEntities.class), PerfCoverage.common("culling"));
    }
}
