package cc.fascinated.fascinatedutils.systems.modules.impl;

import cc.fascinated.fascinatedutils.client.Client;
import cc.fascinated.fascinatedutils.common.ValueSmoother;
import cc.fascinated.fascinatedutils.common.setting.impl.EnumSetting;
import cc.fascinated.fascinatedutils.systems.hud.HUDManager;
import cc.fascinated.fascinatedutils.systems.hud.HudAnchorContentAlignment;
import cc.fascinated.fascinatedutils.systems.hud.HudMiniMessageModule;
import cc.fascinated.fascinatedutils.systems.hud.HudModule;
import cc.fascinated.fascinatedutils.turboentities.EntitiesCullTask;
import cc.fascinated.fascinatedutils.turboentities.TurboEntities;
import cc.fascinated.fascinatedutils.turboparticles.ParticleCullTask;
import cc.fascinated.fascinatedutils.turboparticles.TurboParticles;
import lombok.AllArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class DebugWidget extends HudMiniMessageModule {
    private final ValueSmoother hudRenderMs = new ValueSmoother(500);
    private final ValueSmoother cullPassMs = new ValueSmoother(500);
    private final ValueSmoother particleCullPassMs = new ValueSmoother(500);

    private final EnumSetting<DebugStatsFormat> turboStatsFormat = EnumSetting.<DebugStatsFormat>builder().id("turbo_stats_format").defaultValue(DebugStatsFormat.RATIO).valueFormatter(debugStatsFormat -> debugStatsFormat.displayName).categoryDisplayKey(HudModule.APPEARANCE_CATEGORY_DISPLAY_KEY).build();

    public DebugWidget() {
        super("debug", "Debug", 200f);
        addSetting(turboStatsFormat);
    }

    @Override
    public HudAnchorContentAlignment.Horizontal hudTextLineHorizontalAlignment() {
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
        EntitiesCullTask cullTask = turboEntities.getCullTask();
        if (cullTask == null) {
            cullPassMs.reset();
            out.add("  <grey>Disabled</grey>");
        }
        else {
            double passMs = cullPassMs.smooth(cullTask.getLastPassNanos() / 1_000_000.0, deltaSeconds);
            int consideredBlockEntities = cullTask.getConsideredBlockEntityCount();
            int visibleBlockEntities = consideredBlockEntities - cullTask.getCulledBlockEntityCount();
            out.add("  Rendered Block Entities: " + formatStat(visibleBlockEntities, consideredBlockEntities));
            int consideredEntities = cullTask.getConsideredEntityCount();
            int visibleEntities = consideredEntities - cullTask.getCulledEntityCount();
            out.add("  Rendered Entities: " + formatStat(visibleEntities, consideredEntities));
            int consideredFrames = turboEntities.itemFrameCounters.lastConsidered;
            int culledFrames = turboEntities.itemFrameCounters.lastCulled;
            out.add("  Rendered Item Frames: " + formatStat(consideredFrames - culledFrames, consideredFrames));
            int consideredSigns = turboEntities.signCounters.lastConsidered;
            int culledSigns = turboEntities.signCounters.lastCulled;
            out.add("  Rendered Signs: " + formatStat(consideredSigns - culledSigns, consideredSigns));
            int skipped = turboEntities.getLastSkippedEntityTicks();
            int total = turboEntities.getLastTickedEntities() + skipped;
            out.add("  Ticked Entities: " + formatStat(total - skipped, total));
            out.add(String.format(Locale.ENGLISH, "  Last Pass: %.2f ms", passMs));
        }

        // Turbo Particles (merged from ParticlesWidget)
        out.add("<grey>Turbo Particles</grey>");
        TurboParticles particles = Client.TURBO_PARTICLES;
        if (particles == null) {
            out.add("  <grey>Unavailable</grey>");
        }
        else {
            ParticleCullTask particleCullTask = particles.getParticleCullTask();
            if (particleCullTask == null) {
                particleCullPassMs.reset();
                out.add("  <grey>Disabled</grey>");
            }
            else {
                double passMs = particleCullPassMs.smooth(particleCullTask.getLastPassNanos() / 1_000_000.0, deltaSeconds);
                int consideredParticles = particleCullTask.getConsideredParticleCount();
                int visibleParticles = consideredParticles - particleCullTask.getCulledParticleCount();
                out.add("  Rendered Particles: " + formatStat(visibleParticles, consideredParticles));
                out.add(String.format(Locale.ENGLISH, "  Last Pass: %.2f ms", passMs));
            }
        }

        return out;
    }

    private String formatStat(int numerator, int denominator) {
        if (turboStatsFormat.getValue() == DebugStatsFormat.PERCENTAGE) {
            if (denominator <= 0) {
                return "—";
            }
            return String.format(Locale.ENGLISH, "%.1f%%", 100.0 * (double) numerator / (double) denominator);
        }
        return numerator + "/" + denominator;
    }

    @AllArgsConstructor
    private enum DebugStatsFormat {
        RATIO("Ratio (current/total)"), PERCENTAGE("Percentage");

        private final String displayName;
    }
}
