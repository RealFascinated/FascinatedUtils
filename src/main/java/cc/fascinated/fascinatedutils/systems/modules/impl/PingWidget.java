package cc.fascinated.fascinatedutils.systems.modules.impl;

import cc.fascinated.fascinatedutils.common.Colors;
import cc.fascinated.fascinatedutils.common.PingColors;
import cc.fascinated.fascinatedutils.common.setting.impl.BooleanSetting;
import cc.fascinated.fascinatedutils.systems.hud.HudMiniMessageModule;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;

import java.util.List;
import java.util.concurrent.TimeUnit;

public class PingWidget extends HudMiniMessageModule {
    private static final long UPDATE_INTERVAL_NANOS = TimeUnit.MILLISECONDS.toNanos(1_000L);

    private final BooleanSetting usePingColor = BooleanSetting.builder().id("use_ping_color").defaultValue(true).categoryDisplayKey(APPEARANCE_CATEGORY_DISPLAY_KEY).build();

    public PingWidget() {
        super("ping", "Ping", UTILITY_WIDGET_MIN_WIDTH);
        addSetting(usePingColor);
    }

    @Override
    protected List<String> lines(float deltaSeconds) {
        int pingMs = resolvedPingMs();
        if (pingMs <= 0) {
            return List.of("<yellow>... <white>ms");
        }

        int color = usePingColor.getValue() ? PingColors.getPingColor(pingMs) : 0xFFFFFFFF;
        return List.of("<color:" + Colors.rgbHex(color) + ">" + pingMs + "</color> ms");
    }

    @Override
    protected long hudMiniMessageUpdateIntervalNanos() {
        return UPDATE_INTERVAL_NANOS;
    }

    private int resolvedPingMs() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null && minecraft.getConnection() != null) {
            PlayerInfo info = minecraft.getConnection().getPlayerInfo(minecraft.player.getUUID());
            if (info != null) {
                return info.getLatency();
            }
        }
        return 0;
    }
}
