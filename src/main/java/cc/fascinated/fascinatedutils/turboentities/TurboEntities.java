package cc.fascinated.fascinatedutils.turboentities;

import cc.fascinated.fascinatedutils.common.culling.CullCounters;
import cc.fascinated.fascinatedutils.common.culling.OcclusionProvider;
import cc.fascinated.fascinatedutils.event.impl.ClientTickEvent;
import cc.fascinated.fascinatedutils.event.impl.lifecycle.ClientStartedEvent;
import cc.fascinated.fascinatedutils.event.impl.lifecycle.ClientStoppingEvent;
import cc.fascinated.fascinatedutils.settings.SettingsRegistry;
import com.logisticscraft.occlusionculling.OcclusionCullingInstance;
import com.logisticscraft.occlusionculling.cache.ArrayOcclusionCache;
import lombok.Getter;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.EntityType;

import java.util.Map;

public class TurboEntities {
    public static final Map<EntityType<?>, Double> RENDER_DISTANCE_CAPS = Map.of(EntityType.ITEM, 32.0 * 32.0, EntityType.EXPERIENCE_ORB, 32.0 * 32.0, EntityType.ARMOR_STAND, 48.0 * 48.0, EntityType.ITEM_DISPLAY, 48.0 * 48.0);

    /**
     * Block entities farther than this chunk radius from the player are not snapshotted for the cull worker
     * (still capped by effective render distance).
     */
    public static final int BLOCK_ENTITY_CULL_CHUNK_RADIUS_CAP = 12;

    // Rebuild entity + block-entity cull snapshots only every N client ticks to spread chunk/entity scan cost.
    private static final int CULL_SNAPSHOT_CAPTURE_RATE_TICKS = 5;

    public final CullCounters itemFrameCounters = new CullCounters();
    public final CullCounters paintingCounters = new CullCounters();
    public final CullCounters signCounters = new CullCounters();

    @Getter
    private EntitiesCullTask cullTask;
    private Thread cullThread;

    /**
     * Snapshot of the Turbo Entities setting for hot paths (render thread); refreshed each client tick.
     */
    private volatile boolean turboEntitiesCullEnabledMirror = false;

    private boolean previousEnabledState = false;

    private int cullSnapshotTickCounter;
    // Per-tick entity tick counters (snapshotted each tick for the debug widget)
    private volatile int tickedEntities = 0;
    private volatile int skippedEntityTicks = 0;
    @Getter
    private volatile int lastTickedEntities = 0;
    @Getter
    private volatile int lastSkippedEntityTicks = 0;

    /**
     * Fast gate for mixins: avoids walking {@link SettingsRegistry} on every block entity / entity call.
     */
    public boolean isTurboEntitiesCullEnabled() {
        return turboEntitiesCullEnabledMirror;
    }

    public void incrementTickedEntities() {
        tickedEntities++;
    }

    public void incrementSkippedEntityTicks() {
        skippedEntityTicks++;
    }

    public void snapshotAndResetRenderFrameCounters() {
        itemFrameCounters.snapshotAndReset();
        paintingCounters.snapshotAndReset();
        signCounters.snapshotAndReset();
    }

    @EventHandler
    private void fascinatedutils$onClientStarted(ClientStartedEvent event) {
        boolean enabled = SettingsRegistry.INSTANCE.getSettings().getTurboEntities().isEnabled();
        previousEnabledState = enabled;
        turboEntitiesCullEnabledMirror = enabled;
        if (enabled) {
            startCullThread();
        }
    }

    @EventHandler
    private void fascinatedutils$onClientTick(ClientTickEvent event) {
        lastTickedEntities = tickedEntities;
        lastSkippedEntityTicks = skippedEntityTicks;
        tickedEntities = 0;
        skippedEntityTicks = 0;

        boolean enabled = SettingsRegistry.INSTANCE.getSettings().getTurboEntities().isEnabled();
        turboEntitiesCullEnabledMirror = enabled;
        if (enabled != previousEnabledState) {
            previousEnabledState = enabled;
            if (enabled) {
                startCullThread();
            }
            else {
                stopCullThread();
            }
        }

        EntitiesCullTask task = cullTask;
        if (task != null && enabled) {
            if (cullSnapshotTickCounter++ % CULL_SNAPSHOT_CAPTURE_RATE_TICKS == 0) {
                Minecraft client = Minecraft.getInstance();
                task.publishEntityCullSnapshotFromWorld(client);
                task.publishBlockEntityCullSnapshotFromWorld(client, BLOCK_ENTITY_CULL_CHUNK_RADIUS_CAP);
            }
        }
    }

    @EventHandler
    private void fascinatedutils$onClientStopping(ClientStoppingEvent event) {
        turboEntitiesCullEnabledMirror = false;
        stopCullThread();
    }

    private void startCullThread() {
        if (cullThread != null && cullThread.isAlive()) {
            return;
        }
        // aabbExpansion 0: default 0.5 widens single-block AABBs so the library's all-axis INSIDE fast-path
        // treats the camera cell as inside the target for most adjacent block entities when buried in solids.
        OcclusionCullingInstance culling = new OcclusionCullingInstance(128, new OcclusionProvider(), new ArrayOcclusionCache(128), 0.0);
        cullTask = new EntitiesCullTask(culling);
        cullSnapshotTickCounter = 0;
        cullThread = new Thread(cullTask, "FascinatedUtils-TurboEntitiesCull");
        cullThread.setDaemon(true);
        cullThread.start();
    }

    private void stopCullThread() {
        if (cullTask != null) {
            cullTask.stop();
        }
        cullThread = null;
        cullTask = null;
    }
}