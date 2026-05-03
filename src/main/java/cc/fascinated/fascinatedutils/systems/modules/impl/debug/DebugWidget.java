package cc.fascinated.fascinatedutils.systems.modules.impl.debug;

import cc.fascinated.fascinatedutils.client.Client;
import cc.fascinated.fascinatedutils.common.ValueSmoother;
import cc.fascinated.fascinatedutils.common.setting.impl.EnumSetting;
import cc.fascinated.fascinatedutils.systems.hud.HudDefaults;
import cc.fascinated.fascinatedutils.systems.hud.HudHostModule;
import cc.fascinated.fascinatedutils.systems.hud.MiniMessageHudChrome;
import cc.fascinated.fascinatedutils.systems.modules.Module;
import cc.fascinated.fascinatedutils.systems.modules.impl.debug.hud.DebugHudPanel;
import cc.fascinated.fascinatedutils.systems.turboentities.TurboEntities;
import cc.fascinated.fascinatedutils.systems.turboparticles.TurboParticles;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Locale;

@Getter
public class DebugWidget extends HudHostModule {
    private final ValueSmoother hudRenderMs = new ValueSmoother(500);
    private final ValueSmoother cullPassMs = new ValueSmoother(500);
    private final ValueSmoother particleCullPassMs = new ValueSmoother(500);

    private final EnumSetting<DebugStatsFormat> turboStatsFormat = EnumSetting.<DebugStatsFormat>builder().id("turbo_stats_format").defaultValue(DebugStatsFormat.RATIO).valueFormatter(debugStatsFormat -> debugStatsFormat.displayName).categoryDisplayKey(Module.APPEARANCE_CATEGORY_DISPLAY_KEY).build();

    public DebugWidget() {
        super("debug", "Debug", HudDefaults.builder().build());
        MiniMessageHudChrome.register(this);
        addSetting(turboStatsFormat);
        registerHudPanel(new DebugHudPanel(this));
    }

    public TurboEntities turboEntities() {
        return Client.TURBO_ENTITIES;
    }

    public TurboParticles turboParticles() {
        return Client.TURBO_PARTICLES;
    }

    /**
     * Formats a ratio or percentage for turbo entity / particle stats.
     *
     * @param numerator   visible or active count
     * @param denominator total considered count
     * @return localized display string
     */
    public String formatDebugStat(int numerator, int denominator) {
        if (turboStatsFormat.getValue() == DebugStatsFormat.PERCENTAGE) {
            if (denominator <= 0) {
                return "—";
            }
            return String.format(Locale.ENGLISH, "%.1f%%", 100.0 * (double) numerator / (double) denominator);
        }
        return numerator + "/" + denominator;
    }

    @Getter
    @AllArgsConstructor
    enum DebugStatsFormat {
        RATIO("Ratio (current/total)"), PERCENTAGE("Percentage");

        private final String displayName;
    }
}
