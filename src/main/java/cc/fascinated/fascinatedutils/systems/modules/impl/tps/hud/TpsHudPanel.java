package cc.fascinated.fascinatedutils.systems.modules.impl.tps.hud;

import cc.fascinated.fascinatedutils.common.color.Colors;
import cc.fascinated.fascinatedutils.common.color.TpsColors;
import cc.fascinated.fascinatedutils.systems.hud.HudHostModule;
import cc.fascinated.fascinatedutils.systems.hud.panel.MiniMessageHudPanel;
import cc.fascinated.fascinatedutils.systems.modules.impl.tps.TpsWidget;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class TpsHudPanel extends MiniMessageHudPanel {

    private static final long UPDATE_INTERVAL_NANOS = TimeUnit.MILLISECONDS.toNanos(500L);

    private final TpsWidget tpsWidget;

    public TpsHudPanel(TpsWidget tpsWidget) {
        super(tpsWidget, "tps", TpsWidget.MIN_WIDTH_WITH_MSPT, UPDATE_INTERVAL_NANOS);
        this.tpsWidget = tpsWidget;
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

    @Override
    protected float resolvePanelMinimumWidth(float baseMinWidth) {
        boolean showMspt = tpsWidget.getShowMspt().isEnabled();
        float resolved = showMspt ? TpsWidget.MIN_WIDTH_WITH_MSPT : HudHostModule.UTILITY_WIDGET_MIN_WIDTH;
        return super.resolvePanelMinimumWidth(resolved);
    }

    @Override
    protected List<String> computeMiniMessageLines(float deltaSeconds, boolean editorMode) {
        float lastKnownTps = tpsWidget.getLastKnownTps();
        float lastKnownMspt = tpsWidget.getLastKnownMspt();
        if (!Float.isFinite(lastKnownTps) || !Float.isFinite(lastKnownMspt)) {
            return List.of("<grey>" + hudText("unavailable") + "</grey>");
        }
        boolean showMspt = tpsWidget.getShowMspt().isEnabled();
        boolean useTpsColor = tpsWidget.getUseTpsColor().isEnabled();
        return formatLine(lastKnownTps, lastKnownMspt, !tpsWidget.isLastSampleIntegratedServer(), showMspt, useTpsColor);
    }
}
