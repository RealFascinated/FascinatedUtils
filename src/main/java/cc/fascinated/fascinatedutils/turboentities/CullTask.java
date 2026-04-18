package cc.fascinated.fascinatedutils.turboentities;

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
import net.minecraft.world.level.block.entity.BannerBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
public class CullTask implements Runnable {
    private static final int SLEEP_DELAY = 10;
    private static final double TRACING_DISTANCE = 128.0;
    private static final double HITBOX_LIMIT = 64.0;

    private final OcclusionCullingInstance culling;
    private final Minecraft client = Minecraft.getInstance();
    // Reused allocations — only accessed from the cull thread
    private final Vec3d aabbMin = new Vec3d(0, 0, 0);
    private final Vec3d aabbMax = new Vec3d(0, 0, 0);
    private final Vec3d cameraVec3d = new Vec3d(0, 0, 0);
    private volatile boolean running = true;
    private volatile boolean requestCull = false;
    @Setter
    private volatile boolean ingame = false;
    @Setter
    private volatile List<Entity> entitiesForRendering = new ArrayList<>();
    @Setter
    private volatile Map<BlockPos, BlockEntity> blockEntities = new HashMap<>();
    @Setter
    private volatile Vec3 camera = Vec3.ZERO;
    private volatile int consideredEntityCount = 0;
    private volatile int culledEntityCount = 0;
    private volatile int consideredBlockEntityCount = 0;
    private volatile int culledBlockEntityCount = 0;
    @Getter
    private volatile long lastPassNanos = 0L;
    private volatile Frustum frustum = null;
    private Vec3 lastCameraPos = Vec3.ZERO;

    public CullTask(OcclusionCullingInstance culling) {
        this.culling = culling;
    }

    private static AABB getBlockEntityAABB(BlockPos pos, BlockEntity blockEntity) {
        if (blockEntity instanceof BannerBlockEntity) {
            return new AABB(pos).inflate(0, 1, 0);
        }
        return new AABB(pos);
    }

    @Override
    public void run() {
        while (running && client.isRunning()) {
            try {
                Thread.sleep(SLEEP_DELAY);

                if (!ingame) {
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
            } catch (Exception exception) {
                // Silently handle exceptions from concurrent access
            }
        }
    }

    public void setFrustum(Frustum newFrustum) {
        this.frustum = newFrustum;
    }

    /**
     * Requests a cull update on the next tick.
     */
    public void requestCull() {
        this.requestCull = true;
    }

    /**
     * Stops the cull task.
     */
    public void stop() {
        this.running = false;
    }

    private void cullEntities() {
        Vec3 cameraPos = lastCameraPos;
        List<Entity> entities = entitiesForRendering;
        int consideredCount = 0;
        int culledCount = 0;

        for (Entity entity : entities) {
            if (entity == null) {
                break;
            }
            if (!(entity instanceof Cullable cullable)) {
                continue;
            }
            consideredCount++;
            if (cullable.fascinatedutils$isCulled()) {
                culledCount++;
            }
            if (cullable.fascinatedutils$isForcedVisible()) {
                continue;
            }

            if (client.shouldEntityAppearGlowing(entity)) {
                cullable.fascinatedutils$setCulled(false);
                continue;
            }

            // Never cull the entity the player is currently riding
            if (client.player != null && client.player.getVehicle() == entity) {
                cullable.fascinatedutils$setCulled(false);
                continue;
            }

            // Skip entities whose renderer opts out of culling (paintings, item frames, etc.)
            EntityRenderer<?, ?> entityRenderer = client.getEntityRenderDispatcher().getRenderer(entity);
            if (entityRenderer == null || !(entityRenderer instanceof EntityRendererAccess rendererAccess) || !rendererAccess.fascinatedutils$affectedByCulling(entity)) {
                cullable.fascinatedutils$setCulled(false);
                continue;
            }

            AABB boundingBox = getCullingBox(entity, rendererAccess);
            if (boundingBox == null) {
                cullable.fascinatedutils$setCulled(false);
                continue;
            }

            if (boundingBox.getXsize() > HITBOX_LIMIT || boundingBox.getYsize() > HITBOX_LIMIT || boundingBox.getZsize() > HITBOX_LIMIT) {
                cullable.fascinatedutils$setCulled(false);
                continue;
            }

            // Frustum check first — fastest rejection, covers behind-you and off-screen
            Frustum currentFrustum = frustum;
            if (currentFrustum != null && !currentFrustum.isVisible(boundingBox)) {
                cullable.fascinatedutils$setCulled(true);
                continue;
            }

            // Occlusion raycast only for entities within tracing range
            if (!entity.position().closerThan(cameraPos, TRACING_DISTANCE)) {
                cullable.fascinatedutils$setCulled(false);
                continue;
            }

            aabbMin.set(boundingBox.minX, boundingBox.minY, boundingBox.minZ);
            aabbMax.set(boundingBox.maxX, boundingBox.maxY, boundingBox.maxZ);
            boolean visible = culling.isAABBVisible(aabbMin, aabbMax, cameraVec3d);
            cullable.fascinatedutils$setCulled(!visible);
        }

        consideredEntityCount = consideredCount;
        culledEntityCount = culledCount;
    }

    private void cullBlockEntities() {
        Vec3 cameraPos = lastCameraPos;
        Map<BlockPos, BlockEntity> blockEntitiesCopy = blockEntities;
        int consideredCount = 0;
        int culledCount = 0;

        for (Map.Entry<BlockPos, BlockEntity> entry : blockEntitiesCopy.entrySet()) {
            try {
                BlockPos pos = entry.getKey();
                BlockEntity blockEntity = entry.getValue();

                if (blockEntity == null) {
                    continue;
                }
                if (!(blockEntity instanceof Cullable cullable)) {
                    continue;
                }
                consideredCount++;
                if (cullable.fascinatedutils$isCulled()) {
                    culledCount++;
                }
                if (cullable.fascinatedutils$isForcedVisible()) {
                    continue;
                }

                // No renderer means vanilla won't render it anyway
                if (client.getBlockEntityRenderDispatcher().getRenderer(blockEntity) == null) {
                    cullable.fascinatedutils$setCulled(false);
                    continue;
                }

                // Block entities beyond 64 blocks are outside vanilla's render range
                double distSq = pos.distToCenterSqr(cameraPos);
                if (distSq > 64 * 64) {
                    cullable.fascinatedutils$setCulled(false);
                    continue;
                }

                AABB boundingBox = getBlockEntityAABB(pos, blockEntity);

                if (boundingBox.getXsize() > HITBOX_LIMIT || boundingBox.getYsize() > HITBOX_LIMIT || boundingBox.getZsize() > HITBOX_LIMIT) {
                    cullable.fascinatedutils$setCulled(false);
                    continue;
                }

                Frustum currentFrustum = frustum;
                if (currentFrustum != null && !currentFrustum.isVisible(boundingBox)) {
                    cullable.fascinatedutils$setCulled(true);
                    continue;
                }

                aabbMin.set(boundingBox.minX, boundingBox.minY, boundingBox.minZ);
                aabbMax.set(boundingBox.maxX, boundingBox.maxY, boundingBox.maxZ);
                boolean visible = culling.isAABBVisible(aabbMin, aabbMax, cameraVec3d);
                cullable.fascinatedutils$setCulled(!visible);
            } catch (Exception exception) {
                // Silently handle concurrent modification
            }
        }

        consideredBlockEntityCount = consideredCount;
        culledBlockEntityCount = culledCount;
    }

    private AABB getCullingBox(Entity entity, EntityRendererAccess rendererAccess) {
        if (entity instanceof ArmorStand armorStand && armorStand.isMarker()) {
            return EntityType.ARMOR_STAND.getDimensions().makeBoundingBox(entity.position());
        }
        return rendererAccess.fascinatedutils$getCullingBox(entity);
    }
}
