package cc.fascinated.fascinatedutils.debug.perf.tests.entity;

import cc.fascinated.fascinatedutils.common.culling.AbstractCullTask;
import cc.fascinated.fascinatedutils.common.culling.Cullable;
import cc.fascinated.fascinatedutils.debug.perf.core.PerfTest;
import cc.fascinated.fascinatedutils.debug.perf.coverage.PerfCoverage;
import cc.fascinated.fascinatedutils.systems.turboentities.EntitiesCullTask;
import cc.fascinated.fascinatedutils.systems.turboentities.TurboEntities;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.sheep.Sheep;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Checks the turbo cull result against line of sight. In every scene, an entity in front of the camera with a clear
 * line of sight to both its eye and its box center must never be culled. At least 90% of the sheep whose box center is
 * hidden behind the nametags scene's bedrock wall must be culled (terrain-hidden sheep are not checked, since terrain
 * hiding the center does not mean the whole box is hidden). A failed check throws, so the test reports FAILED.
 */
public class CullCorrectnessPerfTest extends PerfTest {
    private static final double WALL_Z = 16;
    private static final double REQUIRED_CULLED_FRACTION = 0.9;
    private static final double MAX_CHECK_DISTANCE = 48;
    private static final double MIN_FORWARD_DOT = 0.8;

    private EntitiesCullTask cullTask;
    private Vec3 cameraPosition;
    private List<Entity> clearlyVisible;
    private List<Sheep> sheepBehindWall;

    public CullCorrectnessPerfTest() {
        super("turbo_entities.cull_correctness", "Turbo entity cull result checked against line of sight", 2, 20);
    }

    @Override
    public void setup() {
        Minecraft minecraft = Minecraft.getInstance();
        cullTask = new EntitiesCullTask(AbstractCullTask.createOcclusionCulling());
        cullTask.publishEntityCullSnapshotFromWorld(minecraft);
        cullTask.publishBlockEntityCullSnapshotFromWorld(minecraft, TurboEntities.BLOCK_ENTITY_CULL_CHUNK_RADIUS_CAP);
        Camera camera = minecraft.gameRenderer.getMainCamera();
        cameraPosition = camera.position();
        Vec3 forward = Vec3.directionFromRotation(camera.xRot(), camera.yRot());

        clearlyVisible = new ArrayList<>();
        sheepBehindWall = new ArrayList<>();
        for (Entity entity : minecraft.level.entitiesForRendering()) {
            if (entity == minecraft.player) {
                continue;
            }
            Vec3 center = entity.getBoundingBox().getCenter();
            boolean centerBlocked = isBlocked(minecraft, cameraPosition, center);
            if (entity instanceof Sheep sheep && sheep.getZ() > WALL_Z && isBehindWall(minecraft, cameraPosition, center)) {
                sheepBehindWall.add(sheep);
                continue;
            }
            Vec3 offset = center.subtract(cameraPosition);
            double distance = offset.length();
            if (distance > MAX_CHECK_DISTANCE || distance < 1e-3 || offset.scale(1 / distance).dot(forward) < MIN_FORWARD_DOT) {
                continue;
            }
            if (!centerBlocked && !isBlocked(minecraft, cameraPosition, entity.getEyePosition())) {
                clearlyVisible.add(entity);
            }
        }
    }

    @Override
    public void runOnce() {
        cullTask.cullPass(cameraPosition);

        int wronglyCulled = 0;
        for (Entity entity : clearlyVisible) {
            if (isCulled(entity)) {
                wronglyCulled++;
            }
        }
        if (wronglyCulled > 0) {
            throw new IllegalStateException("cull_correctness: " + wronglyCulled + " of " + clearlyVisible.size() + " clearly visible entities were culled");
        }

        if (sheepBehindWall.isEmpty()) {
            return;
        }
        int culledBehindWall = 0;
        for (Sheep sheep : sheepBehindWall) {
            if (isCulled(sheep)) {
                culledBehindWall++;
            }
        }
        if (culledBehindWall < sheepBehindWall.size() * REQUIRED_CULLED_FRACTION) {
            throw new IllegalStateException("cull_correctness: culled " + culledBehindWall + " of " + sheepBehindWall.size() + " sheep behind the wall, need 90%");
        }
    }

    @Override
    public void teardown() {
        cullTask = null;
        clearlyVisible = null;
        sheepBehindWall = null;
    }

    @Override
    public List<String> sceneIds() {
        return List.of("entities", "turbo_nametags");
    }

    @Override
    public List<String> covers() {
        return List.of(PerfCoverage.system("turboentities"), PerfCoverage.common("culling"));
    }

    private static boolean isCulled(Entity entity) {
        return entity instanceof Cullable cullable && cullable.alumite$isCulled();
    }

    private static boolean isBehindWall(Minecraft minecraft, Vec3 from, Vec3 to) {
        BlockHitResult hit = minecraft.level.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, minecraft.player));
        return hit.getType() == HitResult.Type.BLOCK && minecraft.level.getBlockState(hit.getBlockPos()).is(Blocks.BEDROCK);
    }

    private static boolean isBlocked(Minecraft minecraft, Vec3 from, Vec3 to) {
        BlockHitResult hit = minecraft.level.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, minecraft.player));
        return hit.getType() == HitResult.Type.BLOCK;
    }
}
