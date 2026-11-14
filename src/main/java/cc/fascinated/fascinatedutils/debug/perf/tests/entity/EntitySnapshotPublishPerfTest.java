package cc.fascinated.fascinatedutils.debug.perf.tests.entity;

import cc.fascinated.fascinatedutils.common.culling.OcclusionProvider;
import cc.fascinated.fascinatedutils.debug.perf.core.PerfTest;
import cc.fascinated.fascinatedutils.debug.perf.coverage.PerfCoverage;
import cc.fascinated.fascinatedutils.systems.turboentities.EntitiesCullTask;
import cc.fascinated.fascinatedutils.systems.turboentities.TurboEntities;
import com.logisticscraft.occlusionculling.OcclusionCullingInstance;
import com.logisticscraft.occlusionculling.cache.ArrayOcclusionCache;
import net.minecraft.client.Minecraft;

import java.util.List;

/**
 * The main-thread entity and block entity snapshot publish that TurboEntities runs every few client ticks, on a
 * private {@link EntitiesCullTask} so the live cull thread is not shared.
 */
public class EntitySnapshotPublishPerfTest extends PerfTest {
    private static final int OCCLUSION_RANGE = 128;
    private static final double AABB_EXPANSION = 0.0;

    private EntitiesCullTask cullTask;

    public EntitySnapshotPublishPerfTest() {
        super("turbo_entities.snapshot_publish", "EntitiesCullTask entity and block entity snapshot publish", 50, 1_000);
    }

    @Override
    public void setup() {
        cullTask = new EntitiesCullTask(new OcclusionCullingInstance(OCCLUSION_RANGE, new OcclusionProvider(), new ArrayOcclusionCache(OCCLUSION_RANGE), AABB_EXPANSION));
    }

    @Override
    public void runOnce() {
        Minecraft minecraft = Minecraft.getInstance();
        cullTask.publishEntityCullSnapshotFromWorld(minecraft);
        cullTask.publishBlockEntityCullSnapshotFromWorld(minecraft, TurboEntities.BLOCK_ENTITY_CULL_CHUNK_RADIUS_CAP);
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
        return List.of(PerfCoverage.system("turboentities"));
    }
}
