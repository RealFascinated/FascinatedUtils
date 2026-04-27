package cc.fascinated.fascinatedutils.systems.modules.impl.waypoint;

import cc.fascinated.fascinatedutils.client.keybind.KeybindsWrapper;
import cc.fascinated.fascinatedutils.common.color.SettingColor;
import cc.fascinated.fascinatedutils.common.setting.impl.BooleanSetting;
import cc.fascinated.fascinatedutils.common.setting.impl.KeybindSetting;
import cc.fascinated.fascinatedutils.common.setting.impl.SliderSetting;
import cc.fascinated.fascinatedutils.event.impl.ClientTickEvent;
import cc.fascinated.fascinatedutils.event.impl.PlayerDeathEvent;
import cc.fascinated.fascinatedutils.gui.screens.WaypointCreateScreen;
import cc.fascinated.fascinatedutils.gui.screens.WaypointsScreen;
import cc.fascinated.fascinatedutils.systems.config.ModConfig;
import cc.fascinated.fascinatedutils.systems.config.impl.waypoint.WaypointType;
import cc.fascinated.fascinatedutils.systems.modules.Module;
import cc.fascinated.fascinatedutils.systems.modules.ModuleCategory;
import com.mojang.blaze3d.platform.InputConstants;
import lombok.Getter;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import org.lwjgl.glfw.GLFW;

@Getter
public class WaypointsModule extends Module {
    private final KeyMapping openWaypointsKeyBinding = KeybindsWrapper.registerKeybind("key.fascinatedutils.open_waypoints", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_J, KeybindsWrapper.CATEGORY);
    private final KeybindSetting openWaypointsKeySetting = KeybindSetting.builder().id("open_waypoints_key").defaultValue("").keyBindingSupplier(() -> openWaypointsKeyBinding).categoryDisplayKey("Controls").build();

    private final KeyMapping createWaypointKeyBinding = KeybindsWrapper.registerKeybind("key.fascinatedutils.create_waypoint", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_PERIOD, KeybindsWrapper.CATEGORY);
    private final KeybindSetting createWaypointKeySetting = KeybindSetting.builder().id("create_waypoint_key").defaultValue("").keyBindingSupplier(() -> createWaypointKeyBinding).categoryDisplayKey("Controls").build();

    private final BooleanSetting showBeam = BooleanSetting.builder().id("show_beam").defaultValue(true).build();
    private final BooleanSetting labelOnLook = BooleanSetting.builder().id("label_on_look").defaultValue(false).build();
    private final BooleanSetting showBorder = BooleanSetting.builder().id("show_border").defaultValue(true).build();
    private final SliderSetting labelPadding = SliderSetting.builder().id("label_padding").defaultValue(2f).minValue(1f).maxValue(8f).step(1f).build();
    private final BooleanSetting deathPoints = BooleanSetting.builder().id("death_points").defaultValue(true).build();

    public WaypointsModule() {
        super("Waypoints", ModuleCategory.GENERAL);
        addSetting(openWaypointsKeySetting);
        addSetting(createWaypointKeySetting);
        addSetting(showBeam);
        addSetting(labelOnLook);
        addSetting(showBorder);
        addSetting(labelPadding);
        addSetting(deathPoints);
    }

    @EventHandler
    private void onClientTick(ClientTickEvent event) {
        if (!isEnabled()) {
            return;
        }
        if (openWaypointsKeyBinding.consumeClick()) {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.level != null) {
                minecraft.setScreen(new WaypointsScreen());
            }
        }
        if (createWaypointKeyBinding.consumeClick()) {
            openCreateScreen();
        }
    }

    private void openCreateScreen() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null) {
            return;
        }
        double x = minecraft.player.getX();
        double y = minecraft.player.getY();
        double z = minecraft.player.getZ();
        String dimension = minecraft.level.dimension().identifier().toString();
        String worldKey = resolveWorldKey(minecraft);
        minecraft.setScreen(new WaypointCreateScreen(x, y, z, dimension, worldKey));
    }

    private String resolveWorldKey(Minecraft minecraft) {
        if (minecraft.getSingleplayerServer() != null) {
            return "sp:" + minecraft.getSingleplayerServer().getWorldData().getLevelName();
        }
        ServerData serverData = minecraft.getCurrentServer();
        return "mp:" + (serverData != null ? serverData.ip : "unknown");
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
        String worldKey = resolveWorldKey(minecraft);
        int deathCount = (int) ModConfig.waypoints().getForWorld(worldKey).stream()
                .filter(waypoint -> waypoint.getType() == WaypointType.DEATH)
                .count() + 1;
        ModConfig.waypoints().create("Death #" + deathCount, worldKey, WaypointType.DEATH, x, y, z, dimension,
                new SettingColor(255, 50, 50, 255));
    }
}
