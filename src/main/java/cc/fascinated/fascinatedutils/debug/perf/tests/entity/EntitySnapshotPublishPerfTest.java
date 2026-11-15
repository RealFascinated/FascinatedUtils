package cc.fascinated.fascinatedutils.debug.perf.tests.entity;

import cc.fascinated.fascinatedutils.common.culling.AbstractCullTask;
import cc.fascinated.fascinatedutils.debug.perf.core.PerfTest;
import cc.fascinated.fascinatedutils.debug.perf.coverage.PerfCoverage;
import cc.fascinated.fascinatedutils.debug.perf.scene.impl.EntitiesPerfScene;
import cc.fascinated.fascinatedutils.systems.turboentities.EntitiesCullTask;
import cc.fascinated.fascinatedutils.systems.turboentities.TurboEntities;
import net.minecraft.client.Minecraft;

import java.util.List;

/**
 * The main-thread entity and block entity snapshot publish that TurboEntities runs every few client ticks, on a
 * private {@link EntitiesCullTask} so the live cull thread is not shared.
 */
public class EntitySnapshotPublishPerfTest extends PerfTest {
    private EntitiesCullTask cullTask;

    public EntitySnapshotPublishPerfTest() {
        super("turbo_entities.snapshot_publish", "EntitiesCullTask entity and block entity snapshot publish", 50, 1_000);
    }

    @Override
    public void setup() {
        cullTask = new EntitiesCullTask(AbstractCullTask.createOcclusionCulling());
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
        return List.of(EntitiesPerfScene.ID);
    }

    @Override
    public List<String> covers() {
        return List.of(PerfCoverage.system("turboentities"));
    }
}
