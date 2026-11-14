package cc.fascinated.fascinatedutils.systems.turboentities;

import cc.fascinated.fascinatedutils.client.Client;
import cc.fascinated.fascinatedutils.common.culling.Cullable;
import cc.fascinated.fascinatedutils.debug.perf.core.PerfProbe;
import com.logisticscraft.occlusionculling.OcclusionCullingInstance;
import com.logisticscraft.occlusionculling.util.Vec3d;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Getter
public class EntitiesCullTask implements Runnable {
    private static final int SLEEP_DELAY = 10;
    private static final double HITBOX_LIMIT = 64.0;
    private static final double ENTITY_FORCE_CULL_DISTANCE_SQ = 128.0 * 128.0;
    private static final double BLOCK_ENTITY_FORCE_CULL_DISTANCE_SQ = 64.0 * 64.0;

    private static final PerfProbe.Section CULL_PASS_SECTION = PerfProbe.INSTANCE.section("turbo_entities.cull_pass");
    private final OcclusionCullingInstance culling;
    private final Vec3d aabbMin = new Vec3d(0, 0, 0);

    private final Vec3d aabbMax = new Vec3d(0, 0, 0);
    private final Vec3d cameraVec3d = new Vec3d(0, 0, 0);
    /**
     * Entity hand-off on a fixed tick cadence; the cull thread only reads the published list while holding it.
     */
    private final CullSnapshotBuffers<Entity> entitySnapshots = new CullSnapshotBuffers<>();

    /**
     * Block entity hand-off on a fixed tick cadence (see {@link TurboEntities}); the cull thread only reads the
     * published list while holding it.
     */
    private final CullSnapshotBuffers<BlockEntity> blockEntitySnapshots = new CullSnapshotBuffers<>();
    private volatile boolean running = true;
    private volatile boolean requestCull = false;
    @Setter
    private volatile boolean inGame = false;
    @Setter
    private volatile Vec3 camera = Vec3.ZERO;
    private volatile int consideredEntityCount = 0;
    private volatile int culledEntityCount = 0;
    private volatile int consideredBlockEntityCount = 0;
    private volatile int culledBlockEntityCount = 0;
    @Getter
    private volatile long lastPassNanos = 0L;

    @Setter
    private volatile Frustum frustum = null;
    private Vec3 lastCameraPos = Vec3.ZERO;

    public EntitiesCullTask(OcclusionCullingInstance culling) {
        this.culling = culling;
    }

    @Override
    public void run() {
        while (running && Minecraft.getInstance().isRunning()) {
            try {
                Thread.sleep(SLEEP_DELAY);
                if (!inGame) {
                    continue;
                }

                Vec3 cameraSnap = camera;
                if (requestCull || !cameraSnap.equals(lastCameraPos)) {
                    requestCull = false;
                    cullPass(cameraSnap);
                }
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception exception) {
                // A failed pass must not end the thread, or every entity keeps its last culled flag forever.
                Client.LOG.warn("Entity cull pass failed: {}", exception.toString());
            }
        }
    }

    /**
     * Runs one occlusion and frustum pass over the published entity and block entity snapshots.
     *
     * @param cameraPosition the camera position to cull from
     */
    public void cullPass(Vec3 cameraPosition) {
        lastCameraPos = cameraPosition;
        cameraVec3d.set(cameraPosition.x, cameraPosition.y, cameraPosition.z);
        long passStart = System.nanoTime();
        culling.resetCache();
        List<Entity> entities = entitySnapshots.acquire();
        List<BlockEntity> blockEntities = blockEntitySnapshots.acquire();
        try {
            cullEntities(entities);
            cullBlockEntities(blockEntities);
        } finally {
            entitySnapshots.release();
            blockEntitySnapshots.release();
        }
        lastPassNanos = System.nanoTime() - passStart;
        CULL_PASS_SECTION.record(lastPassNanos);
    }

    public void requestCull() {
        this.requestCull = true;
    }

    public void stop() {
        this.running = false;
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
        int playerChunkX = client.player.chunkPosition().x();
        int playerChunkZ = client.player.chunkPosition().z();
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
        int considered = 0;
        int culled = 0;

        for (Entity entity : snapshot) {
            if (entity == null) {
                break;
            }
            if (!(entity instanceof Cullable cullable)) {
                continue;
            }

            considered++;

            try {
                if (Minecraft.getInstance().shouldEntityAppearGlowing(entity) || (Minecraft.getInstance().player != null && Minecraft.getInstance().player.getVehicle() == entity)) {
                    cullable.alumite$setCulled(false);
                }
                else if (entity.distanceToSqr(lastCameraPos.x, lastCameraPos.y, lastCameraPos.z) > ENTITY_FORCE_CULL_DISTANCE_SQ) {
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
            } catch (Exception exception) {
                // Fail visible: one bad entity must not hide itself or abort the pass for the rest.
                cullable.alumite$setCulled(false);
                continue;
            }

            if (cullable.alumite$isCulled()) {
                culled++;
            }
        }

        consideredEntityCount = considered;
        culledEntityCount = culled;
    }

    private void cullBlockEntities(List<BlockEntity> snapshot) {
        int considered = 0;
        int culled = 0;

        for (BlockEntity blockEntity : snapshot) {
            try {
                if (!(blockEntity instanceof Cullable cullable)) {
                    continue;
                }

                BlockPos pos = blockEntity.getBlockPos();
                if (Minecraft.getInstance().getBlockEntityRenderDispatcher().getRenderer(blockEntity) == null) {
                    cullable.alumite$setCulled(false);
                    continue;
                }
                else if (pos.distToCenterSqr(lastCameraPos) > BLOCK_ENTITY_FORCE_CULL_DISTANCE_SQ) {
                    cullable.alumite$setCulled(true);
                }
                else {
                    AABB box = BlockEntityCullBounds.forCull(blockEntity, pos);
                    cullable.alumite$setCulled(!isVisible(box));
                }

                considered++;
                if (cullable.alumite$isCulled()) {
                    culled++;
                }
            } catch (Exception exception) {
                // Fail visible: one bad block entity must not stay hidden or abort the pass for the rest.
                if (blockEntity instanceof Cullable failedCullable) {
                    failedCullable.alumite$setCulled(false);
                }
                Client.LOG.debug("Block entity cull failed: {}", exception.toString());
            }
        }

        consideredBlockEntityCount = considered;
        culledBlockEntityCount = culled;
    }

    private boolean isVisible(AABB box) {
        if (box == null) {
            return true;
        }

        if (box.getXsize() > HITBOX_LIMIT || box.getYsize() > HITBOX_LIMIT || box.getZsize() > HITBOX_LIMIT) {
            return true;
        }

        if (frustum != null && !frustum.isVisible(box)) {
            return false;
        }

        aabbMin.set(box.minX, box.minY, box.minZ);
        aabbMax.set(box.maxX, box.maxY, box.maxZ);
        return culling.isAABBVisible(aabbMin, aabbMax, cameraVec3d);
    }

    private AABB getCullingBox(Entity entity, EntityRendererAccess rendererAccess) {
        if (entity instanceof ArmorStand armorStand && armorStand.isMarker()) {
            return EntityType.ARMOR_STAND.getDimensions().makeBoundingBox(entity.position());
        }
        return rendererAccess.alumite$getCullingBox(entity);
    }
}
