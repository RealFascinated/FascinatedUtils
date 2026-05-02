package cc.fascinated.fascinatedutils.systems.modules.impl.ping;

import cc.fascinated.fascinatedutils.common.setting.impl.BooleanSetting;
import cc.fascinated.fascinatedutils.systems.modules.impl.ping.hud.PingHudPanel;
import cc.fascinated.fascinatedutils.systems.hud.HudDefaults;
import cc.fascinated.fascinatedutils.systems.hud.HudHostModule;
import cc.fascinated.fascinatedutils.systems.hud.MiniMessageHudChrome;
import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;

@Getter
public class PingWidget extends HudHostModule {
    private final BooleanSetting usePingColor = BooleanSetting.builder().id("use_ping_color").defaultValue(true).categoryDisplayKey(APPEARANCE_CATEGORY_DISPLAY_KEY).build();

    public PingWidget() {
        super("ping", "Ping", HudDefaults.builder().build());
        MiniMessageHudChrome.register(this);
        addSetting(usePingColor);
        registerHudPanel(new PingHudPanel(this));
    }

    /**
     * Resolved tab-list latency for the local player, or {@code 0} when unknown.
     *
     * @return ping in milliseconds
     */
    public int resolvedPingMs() {
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
