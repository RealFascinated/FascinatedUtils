package cc.fascinated.fascinatedutils.systems.modules.impl;

import java.util.List;

import cc.fascinated.fascinatedutils.common.ColorUtils;
import cc.fascinated.fascinatedutils.common.PingColors;
import cc.fascinated.fascinatedutils.common.setting.impl.BooleanSetting;
import cc.fascinated.fascinatedutils.event.impl.packet.PacketReceiveEvent;
import cc.fascinated.fascinatedutils.event.impl.packet.PacketSendEvent;
import cc.fascinated.fascinatedutils.systems.hud.HudMiniMessageModule;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.protocol.common.ClientboundPingPacket;
import net.minecraft.network.protocol.common.ServerboundPongPacket;

public class PingWidget extends HudMiniMessageModule {
    private final BooleanSetting usePingColor = BooleanSetting.builder().id("use_ping_color").defaultValue(true).categoryDisplayKey(APPEARANCE_CATEGORY_DISPLAY_KEY).build();

    private volatile long pingReceivedNanos = 0;
    private volatile int pingReceivedId = Integer.MIN_VALUE;
    private volatile int pingMs = 0;

    public PingWidget() {
        super("ping", "Ping", 56f);
        addSetting(usePingColor);
    }

    @Override
    protected List<String> lines(float deltaSeconds) {
        int color = usePingColor.getValue() ? PingColors.getPingColor(pingMs) : 0xFFFFFFFF;
        return List.of("<color:" + ColorUtils.rgbHex(color) + ">" + pingMs + " ms</color>");
    }

    @EventHandler
    private void onPacketReceive(PacketReceiveEvent event) {
        if (event.packet() instanceof ClientboundPingPacket ping) {
            pingReceivedNanos = System.nanoTime();
            pingReceivedId = ping.getId();
        }
    }

    @EventHandler
    private void onPacketSend(PacketSendEvent event) {
        if (event.packet() instanceof ServerboundPongPacket pong && pong.getId() == pingReceivedId) {
            long receivedNanos = pingReceivedNanos;
            if (receivedNanos > 0) {
                pingMs = (int) ((System.nanoTime() - receivedNanos) / 1_000_000L);
            }
        }
    }
}
