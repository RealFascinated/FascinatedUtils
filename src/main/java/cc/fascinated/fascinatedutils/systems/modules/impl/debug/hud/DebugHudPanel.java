package cc.fascinated.fascinatedutils.systems.modules.impl.debug.hud;

import cc.fascinated.fascinatedutils.client.Client;
import cc.fascinated.fascinatedutils.common.ByteUtils;
import cc.fascinated.fascinatedutils.systems.TextureManager;
import cc.fascinated.fascinatedutils.systems.hud.HUDManager;
import cc.fascinated.fascinatedutils.systems.hud.MiniMessageHudPanel;
import cc.fascinated.fascinatedutils.systems.hud.anchor.HudAnchorContentAlignment;
import cc.fascinated.fascinatedutils.systems.modules.impl.debug.DebugWidget;
import cc.fascinated.fascinatedutils.systems.turboentities.EntitiesCullTask;
import cc.fascinated.fascinatedutils.systems.turboentities.TurboEntities;
import cc.fascinated.fascinatedutils.systems.turboparticles.ParticleCullTask;
import cc.fascinated.fascinatedutils.systems.turboparticles.TurboParticles;
import cc.fascinated.fascinatedutils.systems.turboweather.TurboWeather;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class DebugHudPanel extends MiniMessageHudPanel {

    private final DebugWidget debugWidget;

    public DebugHudPanel(DebugWidget debugWidget) {
        super(debugWidget, "debug", 200f);
        this.debugWidget = debugWidget;
    }

    @Override
    protected HudAnchorContentAlignment.Horizontal textLineHorizontalAlignmentOverrideForPanel() {
        return HudAnchorContentAlignment.Horizontal.LEFT;
    }

    @Override
    protected List<String> computeMiniMessageLines(float deltaSeconds, boolean editorMode) {
        List<String> out = new ArrayList<>();

        out.add("<grey>HUD</grey>");
        double hudMs = debugWidget.getHudRenderMs().smooth(HUDManager.INSTANCE.getLastRenderNanos() / 1_000_000.0, deltaSeconds);
        out.add(String.format(Locale.ENGLISH, "  Render: %.2f ms", hudMs));

        out.add("<grey>Turbo Entities</grey>");
        TurboEntities turboEntities = debugWidget.turboEntities();
        EntitiesCullTask cullTask = turboEntities.getCullTask();
        if (cullTask == null) {
            debugWidget.getCullPassMs().reset();
            debugWidget.getSnapshotCaptureMs().reset();
            out.add("  <grey>Disabled</grey>");
        }
        else {
            double passMs = debugWidget.getCullPassMs().smooth(cullTask.getLastPassNanos() / 1_000_000.0, deltaSeconds);
            int consideredBlockEntities = cullTask.getConsideredBlockEntityCount();
            int visibleBlockEntities = consideredBlockEntities - cullTask.getCulledBlockEntityCount();
            out.add("  Rendered Block Entities: " + debugWidget.formatDebugStat(visibleBlockEntities, consideredBlockEntities));
            int consideredEntities = cullTask.getConsideredEntityCount();
            int visibleEntities = consideredEntities - cullTask.getCulledEntityCount();
            out.add("  Rendered Entities: " + debugWidget.formatDebugStat(visibleEntities, consideredEntities));
            int consideredFrames = turboEntities.itemFrameCounters.getLastConsidered();
            int culledFrames = turboEntities.itemFrameCounters.getLastCulled();
            out.add("  Rendered Item Frames: " + debugWidget.formatDebugStat(consideredFrames - culledFrames, consideredFrames));
            int consideredSigns = turboEntities.signCounters.getLastConsidered();
            int culledSigns = turboEntities.signCounters.getLastCulled();
            out.add("  Rendered Signs: " + debugWidget.formatDebugStat(consideredSigns - culledSigns, consideredSigns));
            out.add("  Cached Block Models: " + turboEntities.getBlockModelCache().size());
            int skipped = turboEntities.getLastSkippedEntityTicks();
            int total = turboEntities.getLastTickedEntities() + skipped;
            out.add("  Ticked Entities: " + debugWidget.formatDebugStat(total - skipped, total));
            out.add(String.format(Locale.ENGLISH, "  Last Pass: %.2f ms", passMs));
            double snapshotMs = debugWidget.getSnapshotCaptureMs().smooth(turboEntities.getLastSnapshotNanos() / 1_000_000.0, deltaSeconds);
            out.add(String.format(Locale.ENGLISH, "  Snapshot Capture: %.2f ms", snapshotMs));
            out.add("  Cull Thread: " + (turboEntities.isCullThreadAlive() ? "Running" : "<red>Stopped</red>"));
        }

        out.add("<grey>Turbo Particles</grey>");
        TurboParticles particles = debugWidget.turboParticles();
        ParticleCullTask particleCullTask = particles.getCullTask();
        if (particleCullTask == null) {
            debugWidget.getParticleCullPassMs().reset();
            out.add("  <grey>Disabled</grey>");
        }
        else {
            double passMs = debugWidget.getParticleCullPassMs().smooth(particleCullTask.getLastPassNanos() / 1_000_000.0, deltaSeconds);
            int consideredParticles = particleCullTask.getConsideredParticleCount();
            int visibleParticles = consideredParticles - particleCullTask.getCulledParticleCount();
            out.add("  Rendered Particles: " + debugWidget.formatDebugStat(visibleParticles, consideredParticles));
            out.add(String.format(Locale.ENGLISH, "  Last Pass: %.2f ms", passMs));
            out.add("  Cull Thread: " + (particles.isCullThreadAlive() ? "Running" : "<red>Stopped</red>"));
        }

        out.add("<grey>Turbo Weather</grey>");
        TurboWeather weather = Client.TURBO_WEATHER;
        if (!weather.isEnabled()) {
            out.add("  <grey>Disabled</grey>");
        }
        else {
            TurboWeather.ColumnTable heights = weather.heights();
            TurboWeather.ColumnTable precipitations = weather.precipitations();
            TurboWeather.ColumnTable lights = weather.lights();
            out.add("  Height Cache Hits: " + debugWidget.formatDebugStat(heights.getLastHits(), heights.getLastLookups()));
            out.add("  Precipitation Cache Hits: " + debugWidget.formatDebugStat(precipitations.getLastHits(), precipitations.getLastLookups()));
            out.add("  Light Cache Hits: " + debugWidget.formatDebugStat(lights.getLastHits(), lights.getLastLookups()));
        }

        out.add("<grey>Textures</grey>");
        out.add("  Loaded: " + TextureManager.INSTANCE.loadedTextures());
        out.add("  Size: " + ByteUtils.formatBytes(TextureManager.INSTANCE.loadedTextureTotalSize(), 2));

        return out;
    }
}
