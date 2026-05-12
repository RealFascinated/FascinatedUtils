package cc.fascinated.fascinatedutils.systems.waypoint;

import cc.fascinated.fascinatedutils.Constants;
import cc.fascinated.fascinatedutils.client.Client;
import cc.fascinated.fascinatedutils.common.color.SettingColor;
import cc.fascinated.fascinatedutils.systems.config.ModConfig;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class WaypointRepository {
    private static List<Waypoint> WAYPOINTS = new ArrayList<>();
    private static Path waypointsFile;

    public WaypointRepository() {
        waypointsFile = ModConfig.getDirectory().resolve("waypoints.json");
        this.loadWaypoints();
    }

    private void loadWaypoints() {
        if (!Files.isRegularFile(waypointsFile)) {
            WAYPOINTS = new ArrayList<>();
            return;
        }
        List<Waypoint> loaded = new ArrayList<>();
        try {
            String json = Files.readString(waypointsFile, StandardCharsets.UTF_8);
            JsonElement element = Constants.GSON.fromJson(json, JsonElement.class);
            if (element != null && element.isJsonArray()) {
                for (JsonElement entry : element.getAsJsonArray()) {
                    try {
                        loaded.add(Waypoint.defaults().deserialize(entry, Constants.GSON));
                    } catch (Exception exception) {
                        Client.LOG.warn("Failed to load waypoint: {}", exception.toString());
                    }
                }
            }
        } catch (IOException exception) {
            Client.LOG.warn("Failed to read waypoints file: {}", exception.toString());
        }
        WAYPOINTS = loaded;
    }

    /**
     * Returns all waypoints matching the current world key.
     * For multiplayer, pass the server IP; for singleplayer, pass the level name.
     *
     * @return matching waypoints
     */
    public static List<Waypoint> getForCurrentWorldKey() {
        return WAYPOINTS.stream().filter(waypoint -> waypoint.getWorldKey().equals(getWorldKey())).toList();
    }

    /**
     * Returns all waypoints across all servers.
     *
     * @return unmodifiable view of the full waypoint list
     */
    public static List<Waypoint> list() {
        return Collections.unmodifiableList(WAYPOINTS);
    }

    public static String getWorldKey() {
        Minecraft minecraft = Minecraft.getInstance();
        String worldKey;
        if (minecraft.getSingleplayerServer() != null) {
            worldKey = "sp:" + minecraft.getSingleplayerServer().getWorldData().getLevelName();
        }
        else {
            ServerData serverData = minecraft.getCurrentServer();
            worldKey = "mp:" + (serverData != null ? serverData.ip : "unknown");
        }

        return worldKey;
    }

    /**
     * Creates a new waypoint, adds it to the cache, and persists it.
     *
     * @param name      display name
     * @param type      waypoint type
     * @param x         world X coordinate
     * @param y         world Y coordinate
     * @param z         world Z coordinate
     * @param dimension dimension resource location (e.g. "minecraft:overworld")
     * @param color     ARGB color
     * @return the newly created waypoint
     */
    public static Waypoint create(String name, WaypointType type, double x, double y, double z, String dimension, SettingColor color) {
        Waypoint waypoint = Waypoint.create(UUID.randomUUID(), type, name, x, y, z, dimension, true, true, true, color.copy());
        WAYPOINTS.add(waypoint);
        save();
        return waypoint;
    }

    /**
     * Deletes the waypoint with the given ID and persists the change.
     *
     * @param id the waypoint UUID
     * @return true if a waypoint was removed, false if not found
     */
    public static boolean delete(UUID id) {
        List<Waypoint> updated = WAYPOINTS.stream().filter(waypoint -> !waypoint.getId().equals(id)).toList();
        if (updated.size() == WAYPOINTS.size()) {
            return false;
        }
        WAYPOINTS = new ArrayList<>(updated);
        save();
        return true;
    }

    /**
     * Persists all cached waypoints to disk.
     */
    public static void save() {
        JsonArray array = new JsonArray();
        for (Waypoint waypoint : WAYPOINTS) {
            array.add(waypoint.serialize(Constants.GSON));
        }
        try {
            Files.createDirectories(waypointsFile.getParent());
            Files.writeString(waypointsFile, Constants.GSON.toJson(array), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            Client.LOG.warn("Failed to save waypoints: {}", exception.toString());
        }
    }
}

