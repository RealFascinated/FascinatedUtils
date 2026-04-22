package cc.fascinated.fascinatedutils.systems.modules.impl;

import cc.fascinated.fascinatedutils.common.Colors;
import cc.fascinated.fascinatedutils.common.IntegratedServerUtils;
import cc.fascinated.fascinatedutils.common.TpsColors;
import cc.fascinated.fascinatedutils.common.setting.impl.BooleanSetting;
import cc.fascinated.fascinatedutils.event.FascinatedEventBus;
import cc.fascinated.fascinatedutils.event.impl.ClientTickEvent;
import cc.fascinated.fascinatedutils.systems.hud.HudMiniMessageModule;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.Minecraft;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class TpsWidget extends HudMiniMessageModule {

    private static final long UPDATE_INTERVAL_NANOS = TimeUnit.MILLISECONDS.toNanos(500L);
    private static final float MAX_TPS = 20f;
    private static final float MIN_WIDTH_WITH_MSPT = 110f;
    private static final float MIN_WIDTH_TPS_ONLY = UTILITY_WIDGET_MIN_WIDTH;
    private final BooleanSetting showMspt = BooleanSetting.builder().id("show_mspt").defaultValue(false).categoryDisplayKey(APPEARANCE_CATEGORY_DISPLAY_KEY).build();
    private final BooleanSetting useTpsColor = BooleanSetting.builder().id("use_tps_color").defaultValue(true).categoryDisplayKey(APPEARANCE_CATEGORY_DISPLAY_KEY).build();
    private long lastWorldTime = -1L;
    private long lastSampleTimeNanos = -1L;
    private float lastKnownTps = Float.NaN;
    private float lastKnownMspt = Float.NaN;
    private boolean lastSampleIntegratedServer;

    public TpsWidget() {
        super("tps", "TPS", MIN_WIDTH_WITH_MSPT);
        addSetting(showMspt);
        addSetting(useTpsColor);
        FascinatedEventBus.INSTANCE.subscribe(this);
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
    public float getMinWidth() {
        return showMspt.getValue() ? MIN_WIDTH_WITH_MSPT : MIN_WIDTH_TPS_ONLY;
    }

    @Override
    protected List<String> lines(float deltaSeconds) {
        if (!Float.isFinite(lastKnownTps) || !Float.isFinite(lastKnownMspt)) {
            return List.of("<grey>TPS N/A</grey>");
        }
        return formatLine(lastKnownTps, lastKnownMspt, !lastSampleIntegratedServer, showMspt.getValue(), useTpsColor.getValue());
    }

    @Override
    protected long hudMiniMessageUpdateIntervalNanos() {
        return UPDATE_INTERVAL_NANOS;
    }

    @EventHandler
    private void fascinatedutils$onClientTick(ClientTickEvent event) {
        updateSamples(event.minecraftClient());
    }

    private void resetSamples() {
        lastWorldTime = -1L;
        lastSampleTimeNanos = -1L;
        lastKnownTps = Float.NaN;
        lastKnownMspt = Float.NaN;
        lastSampleIntegratedServer = false;
    }

    private void updateSamples(Minecraft minecraftClient) {
        if (minecraftClient.level == null) {
            resetSamples();
            return;
        }
        float integratedServerMspt = IntegratedServerUtils.sampleAverageMspt(minecraftClient);
        if (Float.isFinite(integratedServerMspt) && integratedServerMspt > 0f) {
            lastKnownMspt = integratedServerMspt;
            lastKnownTps = Math.min(MAX_TPS, 1000f / integratedServerMspt);
            lastSampleIntegratedServer = true;
            return;
        }
        lastSampleIntegratedServer = false;
        long currentWorldTime = minecraftClient.level.getGameTime();
        long nowNanos = System.nanoTime();
        if (lastWorldTime < 0L || lastSampleTimeNanos < 0L) {
            lastWorldTime = currentWorldTime;
            lastSampleTimeNanos = nowNanos;
            return;
        }
        long tickDelta = currentWorldTime - lastWorldTime;
        long nanosDelta = nowNanos - lastSampleTimeNanos;
        lastWorldTime = currentWorldTime;
        lastSampleTimeNanos = nowNanos;
        if (tickDelta <= 0L || nanosDelta <= 0L) {
            return;
        }
        float measuredMspt = (nanosDelta / 1_000_000f) / tickDelta;
        if (!Float.isFinite(measuredMspt) || measuredMspt <= 0f) {
            return;
        }
        lastKnownMspt = measuredMspt;
        lastKnownTps = Math.min(MAX_TPS, 1000f / measuredMspt);
    }
}
