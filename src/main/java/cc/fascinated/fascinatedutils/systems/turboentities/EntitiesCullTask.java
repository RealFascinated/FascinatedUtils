package cc.fascinated.fascinatedutils.systems.turboentities;

import cc.fascinated.fascinatedutils.common.culling.AbstractCullTask;
import cc.fascinated.fascinatedutils.common.culling.CullDistances;
import cc.fascinated.fascinatedutils.common.culling.CullSnapshotBuffers;
import cc.fascinated.fascinatedutils.debug.perf.core.PerfProbe;
import com.logisticscraft.occlusionculling.OcclusionCullingInstance;
import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.level.block.entity.BannerBlockEntity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class EntitiesCullTask extends AbstractCullTask {
    private static final int SLEEP_DELAY = 10;
    private static final long FRUSTUM_COPY_INTERVAL_NANOS = 10_000_000L;
    private static final PerfProbe.Section CULL_PASS_SECTION = PerfProbe.INSTANCE.section("turbo_entities.cull_pass");

    private final CullSnapshotBuffers<Entity> entitySnapshots = new CullSnapshotBuffers<>();
    private final CullSnapshotBuffers<BlockEntity> blockEntitySnapshots = new CullSnapshotBuffers<>();
    @Getter
    private volatile int consideredEntityCount = 0;
    @Getter
    private volatile int culledEntityCount = 0;
    @Getter
    private volatile int consideredBlockEntityCount = 0;
    @Getter
    private volatile int culledBlockEntityCount = 0;

    public EntitiesCullTask(OcclusionCullingInstance culling) {
        super(culling, SLEEP_DELAY, FRUSTUM_COPY_INTERVAL_NANOS, CULL_PASS_SECTION);
    }

    @Override
    protected void cullSnapshots() {
        List<Entity> entities = entitySnapshots.acquire();
        List<BlockEntity> blockEntities = blockEntitySnapshots.acquire();
        try {
            if (!entities.isEmpty() || !blockEntities.isEmpty()) {
                resetOcclusionCache();
                cullEntities(entities);
                cullBlockEntities(blockEntities);
            }
        } finally {
            entitySnapshots.release();
            blockEntitySnapshots.release();
        }
    }

    @Override
    protected long newestSnapshotPublishNanos() {
        return Math.max(entitySnapshots.getPublishedNanos(), blockEntitySnapshots.getPublishedNanos());
    }

    /**
     * Copies the client level {@code entitiesForRendering()} iterable on the main thread.
     *
     * @param client Minecraft client
     */
    public void publishEntityCullSnapshotFromWorld(Minecraft client) {
        if (client.level == null) {
            entitySnapshots.clear();
            return;
        }
        ArrayList<Entity> snapshot = entitySnapshots.beginPublish();
        for (Entity entity : client.level.entitiesForRendering()) {
            snapshot.add(entity);
        }
        entitySnapshots.publish(snapshot);
    }

    /**
     * Collects the block entities near the player on the main thread (matches vanilla client tick / world access
     * expectations).
     *
     * @param client         Minecraft client
     * @param chunkRadiusCap maximum chunk radius from the player chunk (inclusive)
     */
    public void publishBlockEntityCullSnapshotFromWorld(Minecraft client, int chunkRadiusCap) {
        if (client.level == null || client.player == null) {
            blockEntitySnapshots.clear();
            return;
        }
        int chunkRadius = Math.min(client.options.getEffectiveRenderDistance(), chunkRadiusCap);
        ArrayList<BlockEntity> snapshot = blockEntitySnapshots.beginPublish();
        ChunkPos playerChunk = client.player.chunkPosition();
        int playerChunkX = playerChunk.x();
        int playerChunkZ = playerChunk.z();
        for (int chunkX = -chunkRadius; chunkX <= chunkRadius; chunkX++) {
            for (int chunkZ = -chunkRadius; chunkZ <= chunkRadius; chunkZ++) {
                LevelChunk chunk = client.level.getChunk(playerChunkX + chunkX, playerChunkZ + chunkZ);
                Map<BlockPos, BlockEntity> chunkBlockEntities = chunk.getBlockEntities();
                if (chunkBlockEntities.isEmpty()) {
                    continue;
                }
                for (BlockEntity blockEntity : chunkBlockEntities.values()) {
                    snapshot.add(blockEntity);
                }
            }
        }
        blockEntitySnapshots.publish(snapshot);
    }

    private void cullEntities(List<Entity> snapshot) {
        CullTally tally = cullSnapshot(snapshot, "Entity", (entity, cullable) -> {
            if (Minecraft.getInstance().shouldEntityAppearGlowing(entity) || (Minecraft.getInstance().player != null && Minecraft.getInstance().player.getVehicle() == entity)) {
                cullable.alumite$setCulled(false);
            }
            else if (entity.distanceToSqr(getLastCameraPos().x, getLastCameraPos().y, getLastCameraPos().z) > CullDistances.ENTITY_FORCE_CULL_DISTANCE_SQ) {
                cullable.alumite$setCulled(true);
            }
            else {
                EntityRenderer<?, ?> renderer = Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(entity);
                if (!(renderer instanceof EntityRendererAccess rendererAccess) || !rendererAccess.alumite$affectedByCulling(entity)) {
                    cullable.alumite$setCulled(false);
                }
                else {
                    AABB box = getCullingBox(entity, rendererAccess);
                    cullable.alumite$setCulled(!isVisible(box));
                }
            }
            return true;
        });
        consideredEntityCount = tally.considered();
        culledEntityCount = tally.culled();
    }

    private void cullBlockEntities(List<BlockEntity> snapshot) {
        CullTally tally = cullSnapshot(snapshot, "Block entity", (blockEntity, cullable) -> {
            BlockPos pos = blockEntity.getBlockPos();
            if (Minecraft.getInstance().getBlockEntityRenderDispatcher().getRenderer(blockEntity) == null) {
                cullable.alumite$setCulled(false);
                return false;
            }
            else if (pos.distToCenterSqr(getLastCameraPos()) > CullDistances.BLOCK_ENTITY_FORCE_CULL_DISTANCE_SQ) {
                cullable.alumite$setCulled(true);
            }
            else {
                AABB box = getBlockEntityCullingBox(blockEntity, pos);
                cullable.alumite$setCulled(!isVisible(box));
            }
            return true;
        });
        consideredBlockEntityCount = tally.considered();
        culledBlockEntityCount = tally.culled();
    }

    private AABB getBlockEntityCullingBox(BlockEntity blockEntity, BlockPos pos) {
        if (blockEntity instanceof BannerBlockEntity) {
            return new AABB(pos).inflate(0, 1, 0);
        }
        return new AABB(pos);
    }

    private AABB getCullingBox(Entity entity, EntityRendererAccess rendererAccess) {
        if (entity instanceof ArmorStand armorStand && armorStand.isMarker()) {
            return EntityType.ARMOR_STAND.getDimensions().makeBoundingBox(entity.position());
        }
        return rendererAccess.alumite$getCullingBox(entity);
    }
}
