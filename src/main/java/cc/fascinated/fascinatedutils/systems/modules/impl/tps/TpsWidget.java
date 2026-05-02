package cc.fascinated.fascinatedutils.systems.modules.impl.tps;

import cc.fascinated.fascinatedutils.common.Colors;
import cc.fascinated.fascinatedutils.common.IntegratedServerUtils;
import cc.fascinated.fascinatedutils.common.TpsColors;
import cc.fascinated.fascinatedutils.common.setting.impl.BooleanSetting;
import cc.fascinated.fascinatedutils.event.FascinatedEventBus;
import cc.fascinated.fascinatedutils.event.impl.ClientTickEvent;
import cc.fascinated.fascinatedutils.systems.modules.impl.tps.hud.TpsHudPanel;
import cc.fascinated.fascinatedutils.systems.hud.HudHostModule;
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

    private final BooleanSetting showMspt = BooleanSetting.builder().id("show_mspt")
            .defaultValue(false)
            .categoryDisplayKey(APPEARANCE_CATEGORY_DISPLAY_KEY)
            .build();

    private final BooleanSetting useTpsColor = BooleanSetting.builder()
            .id("use_tps_color")
            .defaultValue(true)
            .categoryDisplayKey(APPEARANCE_CATEGORY_DISPLAY_KEY)
            .build();

    private static final int RING_CAPACITY = 60;

    private final long[] ringWorldTimes = new long[RING_CAPACITY];
    private final long[] ringNanoTimes = new long[RING_CAPACITY];
    private int ringHead = 0;
    private int ringCount = 0;
    private float lastKnownTps = Float.NaN;
    private float lastKnownMspt = Float.NaN;
    private boolean lastSampleIntegratedServer;

    public TpsWidget() {
        super("tps", "TPS", MIN_WIDTH_WITH_MSPT);
        addSetting(showMspt);
        addSetting(useTpsColor);
        FascinatedEventBus.INSTANCE.subscribe(this);
        registerHudPanel(new TpsHudPanel(this));
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
    protected float resolvePanelMinWidth(float baseMinWidth) {
        return showMspt.getValue() ? MIN_WIDTH_WITH_MSPT : HudHostModule.UTILITY_WIDGET_MIN_WIDTH;
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
        ringHead = 0;
        ringCount = 0;
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

        long prevWorldTime = ringCount > 0 ? ringWorldTimes[(ringHead - 1 + RING_CAPACITY) % RING_CAPACITY] : Long.MIN_VALUE;

        if (currentWorldTime < prevWorldTime) {
            resetSamples();
            return;
        }

        if (currentWorldTime == prevWorldTime) {
            return;
        }

        ringWorldTimes[ringHead] = currentWorldTime;
        ringNanoTimes[ringHead] = nowNanos;
        ringHead = (ringHead + 1) % RING_CAPACITY;
        if (ringCount < RING_CAPACITY) {
            ringCount++;
        }

        if (ringCount < 2) {
            return;
        }

        int oldestIdx = (ringHead - ringCount + RING_CAPACITY) % RING_CAPACITY;
        int newestIdx = (ringHead - 1 + RING_CAPACITY) % RING_CAPACITY;
        long worldTimeDelta = ringWorldTimes[newestIdx] - ringWorldTimes[oldestIdx];
        long nanosDelta = ringNanoTimes[newestIdx] - ringNanoTimes[oldestIdx];

        if (worldTimeDelta <= 0L || nanosDelta <= 0L) {
            return;
        }

        float measuredMspt = (float) (nanosDelta / 1_000_000.0 / worldTimeDelta);
        if (!Float.isFinite(measuredMspt) || measuredMspt <= 0f) {
            return;
        }
        lastKnownMspt = measuredMspt;
        lastKnownTps = Math.min(MAX_TPS, 1000f / measuredMspt);
    }

    public float tpsHudBaseMinWidth() {
        return MIN_WIDTH_WITH_MSPT;
    }
}

