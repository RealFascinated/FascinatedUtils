package cc.fascinated.fascinatedutils.turboentities;

import cc.fascinated.fascinatedutils.common.culling.Cullable;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
public class EntitiesCullTask implements Runnable {
    private static final int SLEEP_DELAY = 10;
    private static final double HITBOX_LIMIT = 64.0;
    private static final double ENTITY_FORCE_CULL_DISTANCE_SQ = 128.0 * 128.0;
    private static final double BLOCK_ENTITY_FORCE_CULL_DISTANCE_SQ = 128.0 * 128.0;

    private final OcclusionCullingInstance culling;

    private final Vec3d aabbMin = new Vec3d(0, 0, 0);
    private final Vec3d aabbMax = new Vec3d(0, 0, 0);
    private final Vec3d cameraVec3d = new Vec3d(0, 0, 0);

    private static final int ENTITY_BUFFER_CAPACITY = 4096;
    private static final int BLOCK_ENTITY_SNAPSHOT_INITIAL_CAPACITY = 16384;

    private volatile boolean running = true;
    private volatile boolean requestCull = false;
    @Setter
    private volatile boolean inGame = false;
    @Setter
    private volatile Vec3 camera = Vec3.ZERO;

    private final ArrayList<Entity> entityPublishScratch = new ArrayList<>(ENTITY_BUFFER_CAPACITY);

    /**
     * Immutable hand-off from {@link #publishEntityCullSnapshotFromWorld} on a fixed tick cadence; the cull thread
     * only reads this reference.
     */
    private volatile List<Entity> entityCullSnapshot = List.of();

    /**
     * Replaced on the main thread on a fixed tick cadence (see {@link TurboEntities}). The cull thread only reads the
     * published map reference once per pass.
     */
    private volatile Map<BlockPos, BlockEntity> blockEntityCullSnapshot = Map.of();

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
                    lastCameraPos = cameraSnap;
                    cameraVec3d.set(cameraSnap.x, cameraSnap.y, cameraSnap.z);
                    long passStart = System.nanoTime();
                    culling.resetCache();
                    cullEntities();
                    cullBlockEntities();
                    lastPassNanos = System.nanoTime() - passStart;
                }
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                break;
            }
        }
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
            entityPublishScratch.clear();
            entityCullSnapshot = List.of();
            return;
        }
        entityPublishScratch.clear();
        for (Entity entity : client.level.entitiesForRendering()) {
            if (entity == null) {
                break;
            }
            entityPublishScratch.add(entity);
        }
        entityCullSnapshot = List.copyOf(entityPublishScratch);
    }

    /**
     * Builds a position-keyed map of block entities near the player on the main thread (matches vanilla
     * client tick / world access expectations).
     *
     * @param client            Minecraft client
     * @param chunkRadiusCap    maximum chunk radius from the player chunk (inclusive)
     */
    public void publishBlockEntityCullSnapshotFromWorld(Minecraft client, int chunkRadiusCap) {
        if (client.level == null || client.player == null) {
            this.blockEntityCullSnapshot = Map.of();
            return;
        }
        int chunkRadius = Math.min(client.options.getEffectiveRenderDistance(), chunkRadiusCap);
        HashMap<BlockPos, BlockEntity> map = new HashMap<>(BLOCK_ENTITY_SNAPSHOT_INITIAL_CAPACITY);
        int playerChunkX = client.player.chunkPosition().x();
        int playerChunkZ = client.player.chunkPosition().z();
        for (int chunkX = -chunkRadius; chunkX <= chunkRadius; chunkX++) {
            for (int chunkZ = -chunkRadius; chunkZ <= chunkRadius; chunkZ++) {
                LevelChunk chunk = client.level.getChunk(playerChunkX + chunkX, playerChunkZ + chunkZ);
                map.putAll(chunk.getBlockEntities());
            }
        }
        this.blockEntityCullSnapshot = map;
    }

    private void cullEntities() {
        List<Entity> entities = entityCullSnapshot;
        int considered = 0;
        int culled = 0;

        for (Entity entity : entities) {
            if (entity == null) {
                break;
            }
            if (!(entity instanceof Cullable cullable)) {
                continue;
            }

            considered++;

            if (Minecraft.getInstance().shouldEntityAppearGlowing(entity) || (Minecraft.getInstance().player != null && Minecraft.getInstance().player.getVehicle() == entity)) {
                cullable.fascinatedutils$setCulled(false);
            }
            else if (entity.distanceToSqr(lastCameraPos.x, lastCameraPos.y, lastCameraPos.z) > ENTITY_FORCE_CULL_DISTANCE_SQ) {
                cullable.fascinatedutils$setCulled(true);
            }
            else {
                EntityRenderer<?, ?> renderer = Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(entity);
                if (!(renderer instanceof EntityRendererAccess rendererAccess) || !rendererAccess.fascinatedutils$affectedByCulling(entity)) {
                    cullable.fascinatedutils$setCulled(false);
                }
                else {
                    AABB box = getCullingBox(entity, rendererAccess);
                    cullable.fascinatedutils$setCulled(!isVisible(box));
                }
            }

            if (cullable.fascinatedutils$isCulled()) {
                culled++;
            }
        }

        consideredEntityCount = considered;
        culledEntityCount = culled;
    }

    private void cullBlockEntities() {
        Map<BlockPos, BlockEntity> snapshot = blockEntityCullSnapshot;
        int considered = 0;
        int culled = 0;

        for (BlockEntity blockEntity : snapshot.values()) {
            try {
                if (!(blockEntity instanceof Cullable cullable)) {
                    continue;
                }

                BlockPos pos = blockEntity.getBlockPos();

                considered++;

                if (Minecraft.getInstance().getBlockEntityRenderDispatcher().getRenderer(blockEntity) == null) {
                    cullable.fascinatedutils$setCulled(false);
                }
                else if (pos.distToCenterSqr(lastCameraPos) > BLOCK_ENTITY_FORCE_CULL_DISTANCE_SQ) {
                    cullable.fascinatedutils$setCulled(true);
                }
                else {
                    AABB box = BlockEntityCullBounds.forCull(blockEntity, pos);
                    cullable.fascinatedutils$setCulled(!isVisible(box));
                }

                if (cullable.fascinatedutils$isCulled()) {
                    culled++;
                }
            } catch (Exception ignored) {
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

        Frustum currentFrustum = frustum;
        if (currentFrustum != null && !currentFrustum.isVisible(box)) {
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
        return rendererAccess.fascinatedutils$getCullingBox(entity);
    }
}
