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
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;

@Getter
public class EntitiesCullTask implements Runnable {
    private static final int SLEEP_DELAY = 10;
    private static final double HITBOX_LIMIT = 64.0;
    private static final double BLOCK_ENTITY_FORCE_CULL_DISTANCE_SQ = 128.0 * 128.0;

    private final OcclusionCullingInstance culling;

    private final Vec3d aabbMin = new Vec3d(0, 0, 0);
    private final Vec3d aabbMax = new Vec3d(0, 0, 0);
    private final Vec3d cameraVec3d = new Vec3d(0, 0, 0);

    private static final int ENTITY_BUFFER_CAPACITY = 4096;
    private static final int BLOCK_ENTITY_BUFFER_CAPACITY = 8192;

    private volatile boolean running = true;
    private volatile boolean requestCull = false;
    @Setter
    private volatile boolean inGame = false;
    @Setter
    private volatile Vec3 camera = Vec3.ZERO;

    private volatile int snapshotReadSlot;

    /**
     * {@code -1} while idle; otherwise the snapshot slot the cull thread is iterating.
     */
    private volatile int workerActiveReadSlot = -1;

    private final ArrayList<Entity> entityBuffer0 = new ArrayList<>(ENTITY_BUFFER_CAPACITY);
    private final ArrayList<Entity> entityBuffer1 = new ArrayList<>(ENTITY_BUFFER_CAPACITY);
    private final ArrayList<BlockEntity> blockEntityBuffer0 = new ArrayList<>(BLOCK_ENTITY_BUFFER_CAPACITY);
    private final ArrayList<BlockEntity> blockEntityBuffer1 = new ArrayList<>(BLOCK_ENTITY_BUFFER_CAPACITY);

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
                    int readSlot = snapshotReadSlot;
                    workerActiveReadSlot = readSlot;
                    try {
                        cullEntities(readSlot);
                        cullBlockEntities(readSlot);
                    }
                    finally {
                        workerActiveReadSlot = -1;
                    }
                    lastPassNanos = System.nanoTime() - passStart;
                }
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    public int snapshotWriteSlot() {
        int workerSlot = workerActiveReadSlot;
        if (workerSlot == 0) {
            return 1;
        }
        if (workerSlot == 1) {
            return 0;
        }
        return 1 - snapshotReadSlot;
    }

    public ArrayList<Entity> entityBufferForWrite(int writeSlot) {
        return writeSlot == 0 ? entityBuffer0 : entityBuffer1;
    }

    public ArrayList<BlockEntity> blockEntityBufferForWrite(int writeSlot) {
        return writeSlot == 0 ? blockEntityBuffer0 : blockEntityBuffer1;
    }

    public void publishSnapshotWrite(int writeSlot) {
        this.snapshotReadSlot = writeSlot;
    }

    public void requestCull() {
        this.requestCull = true;
    }

    public void stop() {
        this.running = false;
    }

    private void cullEntities(int readSlot) {
        ArrayList<Entity> entities = readSlot == 0 ? entityBuffer0 : entityBuffer1;
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

    private void cullBlockEntities(int readSlot) {
        ArrayList<BlockEntity> snapshot = readSlot == 0 ? blockEntityBuffer0 : blockEntityBuffer1;
        int considered = 0;
        int culled = 0;

        for (BlockEntity blockEntity : snapshot) {
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
