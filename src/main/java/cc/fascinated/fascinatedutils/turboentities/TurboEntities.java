package cc.fascinated.fascinatedutils.turboentities;

import java.util.Map;

import com.logisticscraft.occlusionculling.OcclusionCullingInstance;

import cc.fascinated.fascinatedutils.event.impl.ClientTickEvent;
import cc.fascinated.fascinatedutils.event.impl.lifecycle.ClientStartedEvent;
import cc.fascinated.fascinatedutils.event.impl.lifecycle.ClientStoppingEvent;
import cc.fascinated.fascinatedutils.settings.SettingsRegistry;
import lombok.Getter;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.world.entity.EntityType;

public class TurboEntities {
    public static final Map<EntityType<?>, Double> RENDER_DISTANCE_CAPS = Map.of(EntityType.ITEM, 32.0 * 32.0, EntityType.EXPERIENCE_ORB, 32.0 * 32.0, EntityType.ARMOR_STAND, 48.0 * 48.0, EntityType.ITEM_DISPLAY, 48.0 * 48.0);

    @Getter
    private CullTask cullTask;
    private Thread cullThread;
    private boolean previousEnabledState = false;

    private volatile int tickedEntities = 0;
    private volatile int skippedEntityTicks = 0;
    private volatile int consideredItemFrames = 0;
    private volatile int backCulledItemFrames = 0;
    private volatile int consideredPaintings = 0;
    private volatile int backCulledPaintings = 0;
    private volatile int consideredSigns = 0;
    private volatile int backCulledSigns = 0;

    // Snapshots of the previous tick's counters, read by the debug widget
    @Getter
    private volatile int lastTickedEntities = 0;
    @Getter
    private volatile int lastSkippedEntityTicks = 0;
    @Getter
    private volatile int lastConsideredItemFrames = 0;
    @Getter
    private volatile int lastBackCulledItemFrames = 0;
    @Getter
    private volatile int lastConsideredPaintings = 0;
    @Getter
    private volatile int lastBackCulledPaintings = 0;
    @Getter
    private volatile int lastConsideredSigns = 0;
    @Getter
    private volatile int lastBackCulledSigns = 0;

    public void incrementTickedEntities() {
        tickedEntities++;
    }

    public void incrementSkippedEntityTicks() {
        skippedEntityTicks++;
    }

    public void incrementConsideredItemFrames() {
        consideredItemFrames++;
    }

    public void incrementBackCulledItemFrames() {
        backCulledItemFrames++;
    }

    public void incrementConsideredPaintings() {
        consideredPaintings++;
    }

    public void incrementBackCulledPaintings() {
        backCulledPaintings++;
    }

    public void incrementConsideredSigns() {
        consideredSigns++;
    }

    public void incrementBackCulledSigns() {
        backCulledSigns++;
    }

    public void snapshotAndResetRenderFrameCounters() {
        lastConsideredItemFrames = consideredItemFrames;
        lastBackCulledItemFrames = backCulledItemFrames;
        lastConsideredPaintings = consideredPaintings;
        lastBackCulledPaintings = backCulledPaintings;
        lastConsideredSigns = consideredSigns;
        lastBackCulledSigns = backCulledSigns;
        consideredItemFrames = 0;
        backCulledItemFrames = 0;
        consideredPaintings = 0;
        backCulledPaintings = 0;
        consideredSigns = 0;
        backCulledSigns = 0;
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
        // Snapshot this tick's counters for the debug widget to read after rendering
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
            return;
        }
        stopCullThread();
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
