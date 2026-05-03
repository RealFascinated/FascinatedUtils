package cc.fascinated.fascinatedutils.systems.modules.impl.tps.hud;

import cc.fascinated.fascinatedutils.common.Colors;
import cc.fascinated.fascinatedutils.common.TpsColors;
import cc.fascinated.fascinatedutils.systems.hud.HudHostModule;
import cc.fascinated.fascinatedutils.systems.hud.MiniMessageHudPanel;
import cc.fascinated.fascinatedutils.systems.modules.impl.tps.TpsWidget;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class TpsHudPanel extends MiniMessageHudPanel {

    private static final long UPDATE_INTERVAL_NANOS = TimeUnit.MILLISECONDS.toNanos(500L);

    private final TpsWidget tpsWidget;

    public TpsHudPanel(TpsWidget tpsWidget) {
        super(tpsWidget, "tps", tpsWidget.tpsHudBaseMinWidth());
        this.tpsWidget = tpsWidget;
    }

    @Override
    protected long miniMessageLineUpdateIntervalNanos() {
        return UPDATE_INTERVAL_NANOS;
    }

    @Override
    protected float resolvePanelMinimumWidth(float baseMinWidth) {
        boolean showMspt = tpsWidget.getShowMspt().isEnabled();
        float resolved = showMspt ? TpsWidget.MIN_WIDTH_WITH_MSPT : HudHostModule.UTILITY_WIDGET_MIN_WIDTH;
        return applyRemoveMinimumWidthFromHost(hudHostModule(), resolved);
    }

    @Override
    protected List<String> computeMiniMessageLines(float deltaSeconds, boolean editorMode) {
        float lastKnownTps = tpsWidget.getLastKnownTps();
        float lastKnownMspt = tpsWidget.getLastKnownMspt();
        if (!Float.isFinite(lastKnownTps) || !Float.isFinite(lastKnownMspt)) {
            return List.of("<grey>TPS N/A</grey>");
        }
        boolean showMspt = tpsWidget.getShowMspt().isEnabled();
        boolean useTpsColor = tpsWidget.getUseTpsColor().isEnabled();
        return formatLine(lastKnownTps, lastKnownMspt, !tpsWidget.isLastSampleIntegratedServer(), showMspt, useTpsColor);
    }

    private static List<String> formatLine(float tps, float mspt, boolean approximate, boolean showMspt, boolean useTpsColor) {
        String formattedTps = String.format(Locale.ENGLISH, "%.2f", tps);
        String tpsToken = useTpsColor ? String.format(Locale.ENGLISH, "<color:%s>%s</color>", Colors.rgbHex(TpsColors.getTpsColor(tps)), formattedTps) : formattedTps;
        if (!showMspt) {
            return List.of(String.format(Locale.ENGLISH, "%s TPS", tpsToken));
        }
        int roundedMspt = Math.round(mspt);
        return List.of(String.format(Locale.ENGLISH, approximate ? "%s TPS (~%dms)" : "%s TPS (%dms)", tpsToken, roundedMspt));
    }
}
