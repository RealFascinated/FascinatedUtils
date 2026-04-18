package cc.fascinated.fascinatedutils.systems.modules.impl;

import cc.fascinated.fascinatedutils.common.ColorUtils;
import cc.fascinated.fascinatedutils.common.PingColors;
import cc.fascinated.fascinatedutils.common.setting.impl.BooleanSetting;
import cc.fascinated.fascinatedutils.systems.hud.HudMiniMessageModule;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.player.LocalPlayer;

import java.util.List;

public class PingWidget extends HudMiniMessageModule {
    private final BooleanSetting usePingColor = BooleanSetting.builder().id("use_ping_color").defaultValue(true).categoryDisplayKey(APPEARANCE_CATEGORY_DISPLAY_KEY).build();

    public PingWidget() {
        super("ping", "Ping", 56f);
        addSetting(usePingColor);
    }

    @Override
    protected List<String> lines(float deltaSeconds) {
        ClientPacketListener networkHandler = Minecraft.getInstance().getConnection();
        LocalPlayer player = Minecraft.getInstance().player;
        int ping = 0;
        if (player != null && networkHandler != null) {
            PlayerInfo entry = networkHandler.getPlayerInfo(player.getUUID());
            if (entry != null) {
                ping = entry.getLatency();
            }
        }
        int color = usePingColor.getValue() ? PingColors.getPingColor(ping) : 0xFFFFFFFF;
        return List.of("<color:" + ColorUtils.rgbHex(color) + ">" + ping + " ms</color>");
    }
}
