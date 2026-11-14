package cc.fascinated.fascinatedutils.debug.perf.scene.impl;

import cc.fascinated.fascinatedutils.common.color.SettingColor;
import cc.fascinated.fascinatedutils.debug.perf.coverage.PerfCoverage;
import cc.fascinated.fascinatedutils.debug.perf.scene.PerfScene;
import cc.fascinated.fascinatedutils.debug.perf.scene.PerfSceneContext;
import cc.fascinated.fascinatedutils.systems.modules.impl.BlockOutlineModule;
import cc.fascinated.fascinatedutils.systems.modules.impl.reach.ReachWidget;
import cc.fascinated.fascinatedutils.systems.modules.impl.wawla.WawlaWidget;
import cc.fascinated.fascinatedutils.systems.modules.impl.waypoint.WaypointsModule;
import cc.fascinated.fascinatedutils.systems.waypoint.Waypoint;
import cc.fascinated.fascinatedutils.systems.waypoint.WaypointRepository;
import cc.fascinated.fascinatedutils.systems.waypoint.WaypointType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.animal.sheep.Sheep;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;

import java.util.List;

/**
 * The player looks at a block (outline and WAWLA), interacts with a sheep every few ticks (reach) and has a waypoint
 * in view (waypoint world render and label). Waypoint saving is disabled during perf world runs.
 */
public class InteractPerfScene extends PerfScene {
    private static final int INTERACT_EVERY_TICKS = 5;
    private static final double SHEEP_SEARCH_RADIUS = 6.0;
    private static final double WAYPOINT_DISTANCE = 12.0;
    private static final SettingColor WAYPOINT_COLOR = new SettingColor(80, 200, 255, 255);

    private Waypoint waypoint;
    private Sheep sheep;

    public InteractPerfScene() {
        super("interact", "Looking at a block, interacting with a sheep every 5 ticks, a waypoint in view");
    }

    @Override
    public void setup(PerfSceneContext context) {
        context.command("execute anchored eyes run setblock ^ ^ ^3 stone");
        context.command("summon sheep ~1 ~-1 ~1 {NoAI:1b,Silent:1b,NoGravity:1b}");
        LocalPlayer player = context.getMinecraft().player;
        String dimension = context.getMinecraft().level.dimension().identifier().toString();
        waypoint = WaypointRepository.create("perf", WaypointType.NORMAL, player.getX(), player.getY(), player.getZ() + WAYPOINT_DISTANCE, dimension, WAYPOINT_COLOR);
    }

    @Override
    public void onTick(PerfSceneContext context, int tick) {
        Minecraft minecraft = context.getMinecraft();
        LocalPlayer player = minecraft.player;
        if (sheep == null || sheep.isRemoved()) {
            AABB searchBox = player.getBoundingBox().inflate(SHEEP_SEARCH_RADIUS);
            sheep = minecraft.level.getEntitiesOfClass(Sheep.class, searchBox).stream().findFirst().orElse(null);
        }
        if (sheep != null && tick % INTERACT_EVERY_TICKS == 0) {
            minecraft.gameMode.interact(player, sheep, new EntityHitResult(sheep), InteractionHand.MAIN_HAND);
        }
    }

    @Override
    public void teardown(PerfSceneContext context) {
        context.command("execute anchored eyes run setblock ^ ^ ^3 air");
        if (waypoint != null) {
            WaypointRepository.delete(waypoint.getId());
            waypoint = null;
        }
        sheep = null;
    }

    @Override
    public List<String> covers() {
        return List.of(PerfCoverage.mixin("blockoutline.BlockOutlineMixin"), PerfCoverage.mixin("waypoints.LevelRendererMixin"), PerfCoverage.mixin("reach.EntityInteractionReachMixin"), PerfCoverage.mixin("ClientPlayerInteractionManagerAccessorMixin"), PerfCoverage.module(BlockOutlineModule.class), PerfCoverage.module(WawlaWidget.class), PerfCoverage.module(ReachWidget.class), PerfCoverage.module(WaypointsModule.class), PerfCoverage.listener(ReachWidget.class), PerfCoverage.system("waypoint"), PerfCoverage.renderer("FascinatedWorldRenderTypes"), PerfCoverage.gui("anim"));
    }
}
