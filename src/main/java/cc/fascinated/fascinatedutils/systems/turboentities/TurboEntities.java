package cc.fascinated.fascinatedutils.systems.turboentities;

import cc.fascinated.fascinatedutils.common.culling.AbstractCullTask;
import cc.fascinated.fascinatedutils.common.culling.CullCounters;
import cc.fascinated.fascinatedutils.common.culling.CullDistances;
import cc.fascinated.fascinatedutils.common.culling.CullThreadLifecycle;
import cc.fascinated.fascinatedutils.debug.perf.core.PerfProbe;
import cc.fascinated.fascinatedutils.event.impl.ClientTickEvent;
import cc.fascinated.fascinatedutils.event.impl.lifecycle.ClientStartedEvent;
import cc.fascinated.fascinatedutils.event.impl.lifecycle.ClientStoppingEvent;
import cc.fascinated.fascinatedutils.settings.Settings;
import lombok.Getter;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.EntityType;

import java.util.Map;

public class TurboEntities {
    public static final Map<EntityType<?>, Double> RENDER_DISTANCE_CAPS = Map.of(EntityType.ITEM, CullDistances.ITEM_RENDER_DISTANCE_SQ, EntityType.EXPERIENCE_ORB, CullDistances.ITEM_RENDER_DISTANCE_SQ, EntityType.ARMOR_STAND, CullDistances.DECORATION_RENDER_DISTANCE_SQ, EntityType.ITEM_DISPLAY, CullDistances.DECORATION_RENDER_DISTANCE_SQ);

    /**
     * Block entities farther than this chunk radius from the player are not snapshotted for the cull worker
     * (still capped by effective render distance).
     */
    public static final int BLOCK_ENTITY_CULL_CHUNK_RADIUS_CAP = 12;

    // Rebuild entity + block-entity cull snapshots only every N client ticks to spread chunk/entity scan cost.
    private static final int CULL_SNAPSHOT_CAPTURE_RATE_TICKS = 5;
    private static final PerfProbe.Section SNAPSHOT_SECTION = PerfProbe.INSTANCE.section("turbo_entities.snapshot");
    private static final PerfProbe.Counter ENTITIES_CONSIDERED_COUNTER = PerfProbe.INSTANCE.counter("turbo_entities.considered");
    private static final PerfProbe.Counter ENTITIES_CULLED_COUNTER = PerfProbe.INSTANCE.counter("turbo_entities.culled");
    private static final PerfProbe.Counter BLOCK_ENTITIES_CONSIDERED_COUNTER = PerfProbe.INSTANCE.counter("turbo_entities.block_considered");
    private static final PerfProbe.Counter BLOCK_ENTITIES_CULLED_COUNTER = PerfProbe.INSTANCE.counter("turbo_entities.block_culled");
    private static final PerfProbe.Counter BACKFACE_CONSIDERED_COUNTER = PerfProbe.INSTANCE.counter("turbo_entities.backface_considered");
    private static final PerfProbe.Counter BACKFACE_CULLED_COUNTER = PerfProbe.INSTANCE.counter("turbo_entities.backface_culled");

    @Getter
    private final BlockModelCache blockModelCache = new BlockModelCache();

    public final CullCounters itemFrameCounters = new CullCounters();
    public final CullCounters signCounters = new CullCounters();

    private int cullSnapshotTickCounter;
    private final CullThreadLifecycle<EntitiesCullTask> cullLifecycle = new CullThreadLifecycle<>(() -> Settings.INSTANCE.getTurboEntities().isEnabled(), () -> new EntitiesCullTask(AbstractCullTask.createOcclusionCulling()), "FascinatedUtils-TurboEntitiesCull", () -> cullSnapshotTickCounter = 0);

    /**
     * Duration of the most recent cull snapshot capture on the client thread.
     */
    @Getter
    private volatile long lastSnapshotNanos = 0L;

    // Per-tick entity tick counters (snapshotted each tick for the debug widget)
    private volatile int tickedEntities = 0;
    private volatile int skippedEntityTicks = 0;
    @Getter
    private volatile int lastTickedEntities = 0;
    @Getter
    private volatile int lastSkippedEntityTicks = 0;

    /**
     * Fast gate for mixins: avoids walking {@link Settings} on every block entity / entity call.
     */
    public boolean isEnabled() {
        return cullLifecycle.isEnabled();
    }

    public EntitiesCullTask getCullTask() {
        return cullLifecycle.getTask();
    }

    public boolean isCullThreadAlive() {
        return cullLifecycle.isThreadAlive();
    }

    public void incrementTickedEntities() {
        tickedEntities++;
    }

    public void incrementSkippedEntityTicks() {
        skippedEntityTicks++;
    }

    public void snapshotAndResetRenderFrameCounters() {
        itemFrameCounters.snapshotAndReset();
        signCounters.snapshotAndReset();
        BACKFACE_CONSIDERED_COUNTER.add(itemFrameCounters.getLastConsidered() + signCounters.getLastConsidered());
        BACKFACE_CULLED_COUNTER.add(itemFrameCounters.getLastCulled() + signCounters.getLastCulled());
    }

    @EventHandler
    private void alumite$onClientStarted(ClientStartedEvent event) {
        cullLifecycle.onClientStarted();
    }

    @EventHandler
    private void alumite$onClientTick(ClientTickEvent event) {
        lastTickedEntities = tickedEntities;
        lastSkippedEntityTicks = skippedEntityTicks;
        tickedEntities = 0;
        skippedEntityTicks = 0;

        cullLifecycle.onClientTick();

        EntitiesCullTask cullTask = cullLifecycle.getTask();
        if (cullTask != null && cullLifecycle.isEnabled()) {
            // Gauges: each tick records the totals of the latest occlusion pass.
            ENTITIES_CONSIDERED_COUNTER.add(cullTask.getConsideredEntityCount());
            ENTITIES_CULLED_COUNTER.add(cullTask.getCulledEntityCount());
            BLOCK_ENTITIES_CONSIDERED_COUNTER.add(cullTask.getConsideredBlockEntityCount());
            BLOCK_ENTITIES_CULLED_COUNTER.add(cullTask.getCulledBlockEntityCount());
            if (cullSnapshotTickCounter++ % CULL_SNAPSHOT_CAPTURE_RATE_TICKS == 0) {
                Minecraft client = Minecraft.getInstance();
                long snapshotNanosStart = System.nanoTime();
                long snapshotStart = SNAPSHOT_SECTION.begin();
                cullTask.publishEntityCullSnapshotFromWorld(client);
                cullTask.publishBlockEntityCullSnapshotFromWorld(client, BLOCK_ENTITY_CULL_CHUNK_RADIUS_CAP);
                SNAPSHOT_SECTION.end(snapshotStart);
                lastSnapshotNanos = System.nanoTime() - snapshotNanosStart;
                // Only fresh snapshot data needs a pass; the worker picks up camera motion on its own, so asking
                // every tick would just re-cull the same positions four times out of five.
                cullTask.requestCull();
            }
        }
    }

    @EventHandler
    private void alumite$onClientStopping(ClientStoppingEvent event) {
        cullLifecycle.onClientStopping();
    }
}
