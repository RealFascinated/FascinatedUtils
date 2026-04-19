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

    // Reused allocations — only accessed from the cull thread
    private final Vec3d aabbMin = new Vec3d(0, 0, 0);
    private final Vec3d aabbMax = new Vec3d(0, 0, 0);
    private final Vec3d cameraVec3d = new Vec3d(0, 0, 0);

    private volatile boolean running = true;
    private volatile boolean requestCull = false;
    @Setter
    private volatile boolean inGame = false;
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

    @Setter
    private volatile Frustum frustum = null;
    private Vec3 lastCameraPos = Vec3.ZERO;

    public CullTask(OcclusionCullingInstance culling) {
        this.culling = culling;
    }

    private static AABB getBlockEntityAABB(BlockPos pos, BlockEntity blockEntity) {
        return blockEntity instanceof BannerBlockEntity ? new AABB(pos).inflate(0, 1, 0) : new AABB(pos);
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
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception ignored) {
                // Concurrent access from the render thread; safe to skip this pass
            }
        }
    }

    public void requestCull() {
        this.requestCull = true;
    }

    public void stop() {
        this.running = false;
    }

    private void cullEntities() {
        Vec3 cameraPos = lastCameraPos;
        List<Entity> entities = entitiesForRendering;
        int considered = 0, culled = 0;

        for (Entity entity : entities) {
            if (entity == null) {
                break;
            }
            if (!(entity instanceof Cullable cullable)) {
                continue;
            }

            considered++;
            if (cullable.fascinatedutils$isCulled()) {
                culled++;
            }
            if (cullable.fascinatedutils$isForcedVisible()) {
                continue;
            }

            if (Minecraft.getInstance().shouldEntityAppearGlowing(entity) || (Minecraft.getInstance().player != null && Minecraft.getInstance().player.getVehicle() == entity)) {
                cullable.fascinatedutils$setCulled(false);
                continue;
            }

            EntityRenderer<?, ?> renderer = Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(entity);
            if (!(renderer instanceof EntityRendererAccess rendererAccess) || !rendererAccess.fascinatedutils$affectedByCulling(entity)) {
                cullable.fascinatedutils$setCulled(false);
                continue;
            }

            AABB box = getCullingBox(entity, rendererAccess);
            cullable.fascinatedutils$setCulled(!isVisible(box, entity.position(), cameraPos, true));
        }

        consideredEntityCount = considered;
        culledEntityCount = culled;
    }

    private void cullBlockEntities() {
        Map<BlockPos, BlockEntity> snapshot = blockEntities;
        int considered = 0, culled = 0;

        for (Map.Entry<BlockPos, BlockEntity> entry : snapshot.entrySet()) {
            try {
                BlockPos pos = entry.getKey();
                BlockEntity blockEntity = entry.getValue();
                if (!(blockEntity instanceof Cullable cullable)) {
                    continue;
                }

                considered++;
                if (cullable.fascinatedutils$isCulled()) {
                    culled++;
                }
                if (cullable.fascinatedutils$isForcedVisible()) {
                    continue;
                }

                if (Minecraft.getInstance().getBlockEntityRenderDispatcher().getRenderer(blockEntity) == null) {
                    cullable.fascinatedutils$setCulled(false);
                    continue;
                }

                // Block entities beyond 64 blocks are outside vanilla's render range
                if (pos.distToCenterSqr(lastCameraPos) > 64 * 64) {
                    cullable.fascinatedutils$setCulled(false);
                    continue;
                }

                AABB box = getBlockEntityAABB(pos, blockEntity);
                cullable.fascinatedutils$setCulled(!isVisible(box, Vec3.atCenterOf(pos), lastCameraPos, false));
            } catch (Exception ignored) {
                // Concurrent modification; skip this block entity
            }
        }

        consideredBlockEntityCount = considered;
        culledBlockEntityCount = culled;
    }

    /**
     * Shared frustum → hitbox-size → occlusion pipeline.
     * Returns true if the object should be rendered, false if it should be culled.
     *
     * @param tracingRangeCheck whether to skip occlusion raycasting beyond TRACING_DISTANCE
     */
    private boolean isVisible(AABB box, Vec3 objectPos, Vec3 cameraPos, boolean tracingRangeCheck) {
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

        if (tracingRangeCheck && !objectPos.closerThan(cameraPos, TRACING_DISTANCE)) {
            return true;
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