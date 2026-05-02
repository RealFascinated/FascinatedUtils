package cc.fascinated.fascinatedutils.systems.modules.impl.reach;

import cc.fascinated.fascinatedutils.event.impl.ClientTickEvent;
import cc.fascinated.fascinatedutils.systems.modules.impl.reach.hud.ReachHudPanel;
import cc.fascinated.fascinatedutils.systems.hud.HudDefaults;
import cc.fascinated.fascinatedutils.systems.hud.HudHostModule;
import cc.fascinated.fascinatedutils.systems.hud.MiniMessageHudChrome;
import lombok.Getter;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.Minecraft;

public class ReachWidget extends HudHostModule {

    @Getter
    private float lastEntityReach = Float.NaN;

    public ReachWidget() {
        super("reach", "Reach", HudDefaults.builder().build());
        MiniMessageHudChrome.register(this);
        registerHudPanel(new ReachHudPanel(this));
    }

    /**
     * Records the reach distance from an entity interaction or attack.
     *
     * @param distance the distance in blocks between the player and the entity
     */
    public void recordEntityReach(float distance) {
        this.lastEntityReach = distance;
    }

    @EventHandler
    private void fascinatedutils$onClientTick(ClientTickEvent event) {
        Minecraft minecraftClient = event.minecraftClient();
        if (minecraftClient.level == null) {
            lastEntityReach = Float.NaN;
        }
    }
}
