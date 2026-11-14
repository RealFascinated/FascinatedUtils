package cc.fascinated.fascinatedutils.debug.perf.tests.entity;

import cc.fascinated.fascinatedutils.common.culling.OcclusionProvider;
import cc.fascinated.fascinatedutils.debug.perf.core.PerfTest;
import cc.fascinated.fascinatedutils.debug.perf.coverage.PerfCoverage;
import cc.fascinated.fascinatedutils.systems.turboentities.EntitiesCullTask;
import cc.fascinated.fascinatedutils.systems.turboentities.TurboEntities;
import com.logisticscraft.occlusionculling.OcclusionCullingInstance;
import com.logisticscraft.occlusionculling.cache.ArrayOcclusionCache;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * One full entity and block entity cull pass over the entities scene's real world snapshot, on a private
 * {@link EntitiesCullTask} so the live cull thread's state is not shared. Settings match TurboEntities.
 */
public class EntityCullPassPerfTest extends PerfTest {
    private static final int OCCLUSION_RANGE = 128;
    private static final double AABB_EXPANSION = 0.0;

    private EntitiesCullTask cullTask;
    private Vec3 cameraPosition;

    public EntityCullPassPerfTest() {
        super("turbo_entities.cull_pass_isolated", "EntitiesCullTask.cullPass over the entities scene snapshot", 20, 500);
    }

    @Override
    public void setup() {
        Minecraft minecraft = Minecraft.getInstance();
        cullTask = new EntitiesCullTask(new OcclusionCullingInstance(OCCLUSION_RANGE, new OcclusionProvider(), new ArrayOcclusionCache(OCCLUSION_RANGE), AABB_EXPANSION));
        cullTask.publishEntityCullSnapshotFromWorld(minecraft);
        cullTask.publishBlockEntityCullSnapshotFromWorld(minecraft, TurboEntities.BLOCK_ENTITY_CULL_CHUNK_RADIUS_CAP);
        cameraPosition = minecraft.gameRenderer.getMainCamera().position();
    }

    @Override
    public void runOnce() {
        cullTask.cullPass(cameraPosition);
    }

    @Override
    public void teardown() {
        cullTask = null;
    }

    @Override
    public List<String> sceneIds() {
        return List.of("entities");
    }

    @Override
    public List<String> covers() {
        return List.of(PerfCoverage.system("turboentities"), PerfCoverage.common("culling"));
    }
}
