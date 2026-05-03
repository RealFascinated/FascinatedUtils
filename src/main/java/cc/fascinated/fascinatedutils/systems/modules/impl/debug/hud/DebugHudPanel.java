package cc.fascinated.fascinatedutils.systems.modules.impl.debug.hud;

import cc.fascinated.fascinatedutils.systems.hud.HUDManager;
import cc.fascinated.fascinatedutils.systems.hud.MiniMessageHudPanel;
import cc.fascinated.fascinatedutils.systems.hud.anchor.HudAnchorContentAlignment;
import cc.fascinated.fascinatedutils.systems.modules.impl.debug.DebugWidget;
import cc.fascinated.fascinatedutils.systems.turboentities.EntitiesCullTask;
import cc.fascinated.fascinatedutils.systems.turboentities.TurboEntities;
import cc.fascinated.fascinatedutils.systems.turboparticles.ParticleCullTask;
import cc.fascinated.fascinatedutils.systems.turboparticles.TurboParticles;

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
            int consideredFrames = turboEntities.itemFrameCounters.lastConsidered;
            int culledFrames = turboEntities.itemFrameCounters.lastCulled;
            out.add("  Rendered Item Frames: " + debugWidget.formatDebugStat(consideredFrames - culledFrames, consideredFrames));
            int consideredSigns = turboEntities.signCounters.lastConsidered;
            int culledSigns = turboEntities.signCounters.lastCulled;
            out.add("  Rendered Signs: " + debugWidget.formatDebugStat(consideredSigns - culledSigns, consideredSigns));
            int skipped = turboEntities.getLastSkippedEntityTicks();
            int total = turboEntities.getLastTickedEntities() + skipped;
            out.add("  Ticked Entities: " + debugWidget.formatDebugStat(total - skipped, total));
            out.add(String.format(Locale.ENGLISH, "  Last Pass: %.2f ms", passMs));
        }

        out.add("<grey>Turbo Particles</grey>");
        TurboParticles particles = debugWidget.turboParticles();
        if (particles == null) {
            out.add("  <grey>Unavailable</grey>");
        }
        else {
            ParticleCullTask particleCullTask = particles.getParticleCullTask();
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
            }
        }

        return out;
    }
}
