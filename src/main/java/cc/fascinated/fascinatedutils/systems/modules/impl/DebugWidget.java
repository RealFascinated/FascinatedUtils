package cc.fascinated.fascinatedutils.systems.modules.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import cc.fascinated.fascinatedutils.client.Client;
import cc.fascinated.fascinatedutils.common.ValueSmoother;
import cc.fascinated.fascinatedutils.systems.hud.HUDManager;
import cc.fascinated.fascinatedutils.systems.hud.HudAnchorContentAlignment;
import cc.fascinated.fascinatedutils.systems.hud.HudMiniMessageModule;
import cc.fascinated.fascinatedutils.turboentities.CullTask;
import cc.fascinated.fascinatedutils.turboentities.TurboEntities;
import cc.fascinated.fascinatedutils.turboparticles.TurboParticlesManager;

public class DebugWidget extends HudMiniMessageModule {
    private final ValueSmoother hudRenderMs = new ValueSmoother(500);
    private final ValueSmoother cullPassMs = new ValueSmoother(500);

    public DebugWidget() {
        super("debug", "Debug", 200f);
    }

    @Override
    public HudAnchorContentAlignment.Horizontal hudContentHorizontalAlignment() {
        return HudAnchorContentAlignment.Horizontal.LEFT;
    }

    @Override
    protected List<String> lines(float deltaSeconds) {
        List<String> out = new ArrayList<>();

        // HUD render time
        out.add("<grey>HUD</grey>");
        double hudMs = hudRenderMs.smooth(HUDManager.INSTANCE.getLastRenderNanos() / 1_000_000.0, deltaSeconds);
        out.add(String.format(Locale.ENGLISH, "  Render: %.2f ms", hudMs));

        // Turbo Entities
        out.add("<grey>Turbo Entities</grey>");
        TurboEntities turboEntities = Client.TURBO_ENTITIES;
        CullTask cullTask = turboEntities.getCullTask();
        if (cullTask == null) {
            cullPassMs.reset();
            out.add("  <grey>Disabled</grey>");
        }
        else {
            double passMs = cullPassMs.smooth(cullTask.getLastPassNanos() / 1_000_000.0, deltaSeconds);
            int consideredBlockEntities = cullTask.getConsideredBlockEntityCount();
            out.add("  Rendered Block Entities: " + (consideredBlockEntities - cullTask.getCulledBlockEntityCount()) + "/" + consideredBlockEntities);
            int consideredEntities = cullTask.getConsideredEntityCount();
            out.add("  Rendered Entities: " + (consideredEntities - cullTask.getCulledEntityCount()) + "/" + consideredEntities);
            int consideredFrames = turboEntities.itemFrameCounters.lastConsidered;
            int culledFrames = turboEntities.itemFrameCounters.lastCulled;
            out.add("  Rendered Item Frames: " + (consideredFrames - culledFrames) + "/" + consideredFrames);
            int consideredPaintings = turboEntities.paintingCounters.lastConsidered;
            int culledPaintings = turboEntities.paintingCounters.lastCulled;
            out.add("  Rendered Paintings: " + (consideredPaintings - culledPaintings) + "/" + consideredPaintings);
            int consideredSigns = turboEntities.signCounters.lastConsidered;
            int culledSigns = turboEntities.signCounters.lastCulled;
            out.add("  Rendered Signs: " + (consideredSigns - culledSigns) + "/" + consideredSigns);
            int skipped = turboEntities.getLastSkippedEntityTicks();
            int total = turboEntities.getLastTickedEntities() + skipped;
            out.add("  Ticked Entities: " + (total - skipped) + "/" + total);
            out.add(String.format(Locale.ENGLISH, "  Last Pass: %.2f ms", passMs));
        }

        // Turbo Particles (merged from ParticlesWidget)
        out.add("<grey>Turbo Particles</grey>");
        TurboParticlesManager particles = Client.TURBO_PARTICLES;
        if (particles == null) {
            out.add("  <grey>Unavailable</grey>");
        }
        else {
            int pConsidered = particles.particleCounters.lastConsidered;
            int pCulled = particles.particleCounters.lastCulled;
            double pPct = pConsidered == 0 ? 0.0 : (100.0 * pCulled / pConsidered);
            out.add("  Particles: considered=" + pConsidered + " culled=" + pCulled);
            out.add(String.format(Locale.ENGLISH, "  Culled: %.1f%%", pPct));
            out.add(String.format(Locale.ENGLISH, "  Extract: %.3f ms", particles.getLastExtractNanos() / 1_000_000.0));
        }

        return out;
    }
}

