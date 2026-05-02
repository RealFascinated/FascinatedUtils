package cc.fascinated.fascinatedutils.systems.modules.impl.speed;

import cc.fascinated.fascinatedutils.common.setting.impl.BooleanSetting;
import cc.fascinated.fascinatedutils.event.impl.ClientTickEvent;
import cc.fascinated.fascinatedutils.systems.modules.impl.speed.hud.SpeedHudPanel;
import cc.fascinated.fascinatedutils.systems.hud.HudMiniMessageModule;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class SpeedWidget extends HudMiniMessageModule {

    private static final int BUFFER_SIZE = 20;

    private final BooleanSetting includeVertical = BooleanSetting.builder().id("include_vertical").defaultValue(false).categoryDisplayKey(APPEARANCE_CATEGORY_DISPLAY_KEY).build();
    private final BooleanSetting showPeak = BooleanSetting.builder().id("show_peak").defaultValue(false).categoryDisplayKey(APPEARANCE_CATEGORY_DISPLAY_KEY).build();
    private final BooleanSetting showAverage = BooleanSetting.builder().id("show_average").defaultValue(false).categoryDisplayKey(APPEARANCE_CATEGORY_DISPLAY_KEY).build();

    private final float[] speedBuffer = new float[BUFFER_SIZE];
    private int bufferHead = 0;
    private int bufferCount = 0;
    private Vec3 lastPosition = null;
    private float currentSpeed = Float.NaN;

    public SpeedWidget() {
        super("speed", "Speed", UTILITY_WIDGET_MIN_WIDTH);
        addSetting(includeVertical);
        addSetting(showPeak);
        addSetting(showAverage);
        registerHudPanel(new SpeedHudPanel(this));
    }

    @EventHandler
    private void fascinatedutils$onClientTick(ClientTickEvent event) {
        Minecraft minecraftClient = event.minecraftClient();
        LocalPlayer player = minecraftClient.player;
        if (minecraftClient.level == null || player == null) {
            reset();
            return;
        }
        Vec3 position = player.position();
        if (lastPosition == null) {
            lastPosition = position;
            return;
        }
        double dx = position.x - lastPosition.x;
        double dy = includeVertical.isEnabled() ? (position.y - lastPosition.y) : 0.0;
        double dz = position.z - lastPosition.z;
        lastPosition = position;
        float speed = (float) (Math.sqrt(dx * dx + dy * dy + dz * dz) * 20.0);
        currentSpeed = speed;
        speedBuffer[bufferHead] = speed;
        bufferHead = (bufferHead + 1) % BUFFER_SIZE;
        if (bufferCount < BUFFER_SIZE) {
            bufferCount++;
        }
    }

    private void reset() {
        lastPosition = null;
        currentSpeed = Float.NaN;
        bufferCount = 0;
        bufferHead = 0;
    }

    @Override
    protected List<String> lines(float deltaSeconds) {
        if (!Float.isFinite(currentSpeed)) {
            return List.of("<grey>Speed N/A</grey>");
        }
        List<String> lines = new ArrayList<>();
        lines.add(String.format(Locale.ENGLISH, "%.2f b/s", currentSpeed));
        if (showPeak.isEnabled() && bufferCount > 0) {
            lines.add(String.format(Locale.ENGLISH, "<grey>Peak:</grey> %.2f", peakSpeed()));
        }
        if (showAverage.isEnabled() && bufferCount > 0) {
            lines.add(String.format(Locale.ENGLISH, "<grey>Avg:</grey> %.2f", averageSpeed()));
        }
        return lines;
    }

    private float peakSpeed() {
        float peak = 0f;
        int start = (bufferHead - bufferCount + BUFFER_SIZE) % BUFFER_SIZE;
        for (int idx = 0; idx < bufferCount; idx++) {
            float val = speedBuffer[(start + idx) % BUFFER_SIZE];
            if (val > peak) {
                peak = val;
            }
        }
        return peak;
    }

    private float averageSpeed() {
        float sum = 0f;
        int start = (bufferHead - bufferCount + BUFFER_SIZE) % BUFFER_SIZE;
        for (int idx = 0; idx < bufferCount; idx++) {
            sum += speedBuffer[(start + idx) % BUFFER_SIZE];
        }
        return sum / bufferCount;
    }
}
