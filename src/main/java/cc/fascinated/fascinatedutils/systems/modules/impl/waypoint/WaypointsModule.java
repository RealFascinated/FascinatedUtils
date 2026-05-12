package cc.fascinated.fascinatedutils.systems.modules.impl.waypoint;

import cc.fascinated.fascinatedutils.client.keybind.KeybindsWrapper;
import cc.fascinated.fascinatedutils.common.color.SettingColor;
import cc.fascinated.fascinatedutils.common.setting.impl.BooleanSetting;
import cc.fascinated.fascinatedutils.common.setting.impl.KeybindSetting;
import cc.fascinated.fascinatedutils.common.setting.impl.SliderSetting;
import cc.fascinated.fascinatedutils.event.impl.PlayerDeathEvent;
import cc.fascinated.fascinatedutils.gui2.screens.impl.waypoint.WaypointsScreen;
import cc.fascinated.fascinatedutils.systems.waypoint.WaypointRepository;
import cc.fascinated.fascinatedutils.systems.waypoint.WaypointType;
import cc.fascinated.fascinatedutils.systems.modules.Module;
import cc.fascinated.fascinatedutils.systems.modules.ModuleCategory;
import com.mojang.blaze3d.platform.InputConstants;
import lombok.Getter;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

@Getter
public class WaypointsModule extends Module {

    private final KeybindSetting waypointsKeybind = new KeybindSetting("waypoints_keybind", () -> KeybindsWrapper.registerCallbackKeybind(
            "key.alumite.waypoints_keybind",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_M,
            KeybindsWrapper.CATEGORY,
            () -> {
                if (isEnabled()) {
                    Minecraft.getInstance().setScreen(new WaypointsScreen());
                }
            }
    ));

    private final BooleanSetting showBeam = BooleanSetting.builder().id("show_beam").defaultValue(true).build();
    private final BooleanSetting labelOnLook = BooleanSetting.builder().id("label_on_look").defaultValue(false).build();
    private final BooleanSetting showBorder = BooleanSetting.builder().id("show_border").defaultValue(true).build();
    private final SliderSetting labelPadding = SliderSetting.builder().id("label_padding").defaultValue(2f).minValue(1f).maxValue(8f).step(1f).build();
    private final BooleanSetting deathPoints = BooleanSetting.builder().id("death_points").defaultValue(true).build();

    public WaypointsModule() {
        super("Waypoints", ModuleCategory.GENERAL);
        addSetting(waypointsKeybind);
        addSetting(showBeam);
        addSetting(labelOnLook);
        addSetting(showBorder);
        addSetting(labelPadding);
        addSetting(deathPoints);
    }

    @EventHandler
    private void onPlayerDeath(PlayerDeathEvent event) {
        if (!isEnabled() || !deathPoints.getValue()) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }
        double x = event.player().getX();
        double y = event.player().getY();
        double z = event.player().getZ();
        String dimension = minecraft.level.dimension().identifier().toString();
        int deathCount = (int) WaypointRepository.getForCurrentWorldKey().stream().filter(waypoint -> waypoint.getType() == WaypointType.DEATH).count() + 1;
        WaypointRepository.create("Death #" + deathCount, WaypointType.DEATH, x, y, z, dimension, new SettingColor(255, 50, 50, 255));
    }
}
