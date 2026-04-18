package cc.fascinated.fascinatedutils.turboentities;

import cc.fascinated.fascinatedutils.event.impl.ClientTickEvent;
import cc.fascinated.fascinatedutils.event.impl.lifecycle.ClientStartedEvent;
import cc.fascinated.fascinatedutils.event.impl.lifecycle.ClientStoppingEvent;
import cc.fascinated.fascinatedutils.settings.SettingsRegistry;
import com.logisticscraft.occlusionculling.OcclusionCullingInstance;
import lombok.Getter;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.world.entity.EntityType;

import java.util.Map;

public class TurboEntities {
    public static final Map<EntityType<?>, Double> RENDER_DISTANCE_CAPS = Map.of(EntityType.ITEM, 32.0 * 32.0, EntityType.EXPERIENCE_ORB, 32.0 * 32.0, EntityType.ARMOR_STAND, 48.0 * 48.0, EntityType.ITEM_DISPLAY, 48.0 * 48.0);

    public final CullCounters itemFrameCounters = new CullCounters();
    public final CullCounters paintingCounters = new CullCounters();
    public final CullCounters signCounters = new CullCounters();

    @Getter
    private CullTask cullTask;
    private Thread cullThread;

    private boolean previousEnabledState = false;

    // Per-tick entity tick counters (snapshotted each tick for the debug widget)
    private volatile int tickedEntities = 0;
    private volatile int skippedEntityTicks = 0;
    @Getter
    private volatile int lastTickedEntities = 0;
    @Getter
    private volatile int lastSkippedEntityTicks = 0;

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
        previousEnabledState = SettingsRegistry.INSTANCE.getSettings().getTurboEntities().isEnabled();
        if (previousEnabledState) {
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
        if (enabled == previousEnabledState) {
            return;
        }

        previousEnabledState = enabled;
        if (enabled) {
            startCullThread();
        }
        else {
            stopCullThread();
        }
    }

    @EventHandler
    private void fascinatedutils$onClientStopping(ClientStoppingEvent event) {
        stopCullThread();
    }

    private void startCullThread() {
        if (cullThread != null && cullThread.isAlive()) {
            return;
        }
        OcclusionCullingInstance culling = new OcclusionCullingInstance(128, new OcclusionProvider());
        cullTask = new CullTask(culling);
        cullThread = new Thread(cullTask, "FascinatedUtils-EntityCulling");
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