package cc.fascinated.fascinatedutils.systems.modules.impl;

import cc.fascinated.fascinatedutils.common.Colors;
import cc.fascinated.fascinatedutils.event.impl.ClientTickEvent;
import cc.fascinated.fascinatedutils.systems.hud.HudMiniMessageModule;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.Minecraft;

import java.util.List;
import java.util.Locale;

public class ReachWidget extends HudMiniMessageModule {

    private float lastEntityReach = Float.NaN;

    public ReachWidget() {
        super("reach", "Reach", UTILITY_WIDGET_MIN_WIDTH);
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

    @Override
    protected List<String> lines(float deltaSeconds) {
        if (!Float.isFinite(lastEntityReach)) {
            return List.of("<yellow>N/A <white>blocks");
        }
        // Vanilla survival reach is ~3.0 blocks; color shifts red toward 6.0+
        float fraction = Math.min((lastEntityReach - 3f) / 3f, 1f);
        String color = Colors.rgbHex(Colors.getGoodBadColor(Math.max(fraction, 0f), true));
        return List.of(String.format(Locale.ENGLISH, "<color:%s>%.2f <white>blocks", color, lastEntityReach));
    }
}
