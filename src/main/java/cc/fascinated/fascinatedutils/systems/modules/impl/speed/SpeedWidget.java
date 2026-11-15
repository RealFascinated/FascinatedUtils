package cc.fascinated.fascinatedutils.systems.modules.impl.speed;

import cc.fascinated.fascinatedutils.common.setting.impl.BooleanSetting;
import cc.fascinated.fascinatedutils.event.impl.ClientTickEvent;
import cc.fascinated.fascinatedutils.systems.hud.HudDefaults;
import cc.fascinated.fascinatedutils.systems.hud.HudHostModule;
import cc.fascinated.fascinatedutils.systems.modules.impl.speed.hud.SpeedHudPanel;
import lombok.Getter;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;

@Getter
public class SpeedWidget extends HudHostModule {

    private static final int BUFFER_SIZE = 20;
    private static final double MAX_TICK_DISTANCE = 16.0;

    private final BooleanSetting includeVertical = BooleanSetting.builder().id("include_vertical").defaultValue(false).categoryDisplayKey(APPEARANCE_CATEGORY_DISPLAY_KEY).build();
    private final BooleanSetting showPeak = BooleanSetting.builder().id("show_peak").defaultValue(false).categoryDisplayKey(APPEARANCE_CATEGORY_DISPLAY_KEY).build();
    private final BooleanSetting showAverage = BooleanSetting.builder().id("show_average").defaultValue(false).categoryDisplayKey(APPEARANCE_CATEGORY_DISPLAY_KEY).build();

    private final float[] speedBuffer = new float[BUFFER_SIZE];
    private int bufferHead = 0;
    private int bufferCount = 0;
    private Vec3 lastPosition = null;
    private LocalPlayer lastPlayer = null;
    private ClientLevel lastLevel = null;
    private float currentSpeed = Float.NaN;


    public SpeedWidget() {
        super("speed", "Speed", HudDefaults.builder().build());
        addAppearanceSettings(true);
        addSetting(includeVertical);
        addSetting(showPeak);
        addSetting(showAverage);
        registerHudPanel(new SpeedHudPanel(this));
    }

    @EventHandler
    private void alumite$onClientTick(ClientTickEvent event) {
        Minecraft minecraftClient = event.minecraftClient();
        LocalPlayer player = minecraftClient.player;
        if (minecraftClient.level == null || player == null) {
            reset();
            return;
        }
        Vec3 position = player.position();
        if (player != lastPlayer || minecraftClient.level != lastLevel) {
            // New player instance (respawn) or level (dimension change): the position delta is not movement.
            lastPlayer = player;
            lastLevel = minecraftClient.level;
            lastPosition = null;
        }
        if (lastPosition == null) {
            lastPosition = position;
            return;
        }
        double dx = position.x - lastPosition.x;
        double dy = includeVertical.isEnabled() ? (position.y - lastPosition.y) : 0.0;
        double dz = position.z - lastPosition.z;
        double fullDeltaSquared = dx * dx + (position.y - lastPosition.y) * (position.y - lastPosition.y) + dz * dz;
        lastPosition = position;
        if (fullDeltaSquared > MAX_TICK_DISTANCE * MAX_TICK_DISTANCE) {
            // Teleport: ignore this sample.
            return;
        }
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
        lastPlayer = null;
        lastLevel = null;
        currentSpeed = Float.NaN;
        bufferCount = 0;
        bufferHead = 0;
    }

    public float sampledPeakSpeed() {
        float peak = 0f;
        int start = (bufferHead - bufferCount + BUFFER_SIZE) % BUFFER_SIZE;
        for (int index = 0; index < bufferCount; index++) {
            float value = speedBuffer[(start + index) % BUFFER_SIZE];
            if (value > peak) {
                peak = value;
            }
        }
        return peak;
    }

    public float sampledAverageSpeed() {
        float sum = 0f;
        int start = (bufferHead - bufferCount + BUFFER_SIZE) % BUFFER_SIZE;
        for (int index = 0; index < bufferCount; index++) {
            sum += speedBuffer[(start + index) % BUFFER_SIZE];
        }
        return sum / bufferCount;
    }
}
