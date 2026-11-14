package cc.fascinated.fascinatedutils.systems.waypoint;

import cc.fascinated.fascinatedutils.Constants;
import cc.fascinated.fascinatedutils.client.Client;
import cc.fascinated.fascinatedutils.common.color.SettingColor;
import cc.fascinated.fascinatedutils.debug.perf.PerfMode;
import cc.fascinated.fascinatedutils.systems.config.ModConfig;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.world.level.storage.LevelResource;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;

public class WaypointRepository {
    private static List<Waypoint> WAYPOINTS = new ArrayList<>();
    private static List<JsonElement> UNPARSED_ENTRIES = new ArrayList<>();
    private static Path waypointsFile;
    private static boolean saveBlocked;

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
        List<JsonElement> unparsed = new ArrayList<>();
        try {
            String json = Files.readString(waypointsFile, StandardCharsets.UTF_8);
            JsonElement element = Constants.GSON.fromJson(json, JsonElement.class);
            if (element != null && !element.isJsonArray()) {
                throw new JsonParseException("waypoints file is not a JSON array");
            }
            if (element != null) {
                for (JsonElement entry : element.getAsJsonArray()) {
                    try {
                        loaded.add(Waypoint.defaults().deserialize(entry, Constants.GSON));
                    } catch (Exception exception) {
                        // Keep the raw entry so it is written back instead of being silently erased on the next save.
                        unparsed.add(entry);
                        Client.LOG.warn("Failed to load waypoint: {}", exception.toString());
                    }
                }
            }
        } catch (IOException exception) {
            // Do not overwrite a file we could not read with only the waypoints created this session.
            saveBlocked = true;
            Client.LOG.warn("Failed to read waypoints file, saving is disabled this session: {}", exception.toString());
        } catch (JsonParseException exception) {
            quarantineCorruptFile(exception);
        }
        WAYPOINTS = loaded;
        UNPARSED_ENTRIES = unparsed;
    }

    private void quarantineCorruptFile(Exception error) {
        Path corruptPath = waypointsFile.resolveSibling(waypointsFile.getFileName() + ".corrupt");
        try {
            Files.move(waypointsFile, corruptPath, StandardCopyOption.REPLACE_EXISTING);
            Client.LOG.warn("Waypoints file is corrupt ({}); moved to {}", error.toString(), corruptPath);
        } catch (IOException moveError) {
            saveBlocked = true;
            Client.LOG.warn("Waypoints file is corrupt ({}) and could not be moved aside, saving is disabled this session: {}", error.toString(), moveError.toString());
        }
    }

    /**
     * Returns all waypoints matching the current world key.
     * For multiplayer, pass the server IP; for singleplayer, pass the level name.
     *
     * @return matching waypoints
     */
    public static List<Waypoint> getForCurrentWorldKey() {
        String worldKey = getWorldKey();
        migrateLegacySingleplayerKey(worldKey);
        return WAYPOINTS.stream().filter(waypoint -> waypoint.getWorldKey().equals(worldKey)).toList();
    }

    /**
     * Moves waypoints stored under the old display-name based singleplayer key to the save-folder based key,
     * but only when nothing is stored under the new key yet.
     *
     * @param worldKey the current world key
     */
    private static void migrateLegacySingleplayerKey(String worldKey) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.getSingleplayerServer() == null || WAYPOINTS.stream().anyMatch(waypoint -> waypoint.getWorldKey().equals(worldKey))) {
            return;
        }
        String legacyKey = "sp:" + minecraft.getSingleplayerServer().getWorldData().getLevelName();
        if (legacyKey.equals(worldKey)) {
            return;
        }
        boolean migrated = false;
        for (Waypoint waypoint : WAYPOINTS) {
            if (waypoint.getWorldKey().equals(legacyKey)) {
                waypoint.setWorldKey(worldKey);
                migrated = true;
            }
        }
        if (migrated) {
            save();
        }
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
            Path saveFolder = minecraft.getSingleplayerServer().getWorldPath(LevelResource.ROOT).toAbsolutePath().normalize().getFileName();
            worldKey = "sp:" + (saveFolder != null ? saveFolder.toString() : minecraft.getSingleplayerServer().getWorldData().getLevelName());
        }
        else {
            ServerData serverData = minecraft.getCurrentServer();
            String address = serverData != null ? serverData.ip : null;
            if (address == null && minecraft.getConnection() != null) {
                SocketAddress remoteAddress = minecraft.getConnection().getConnection().getRemoteAddress();
                address = remoteAddress != null ? remoteAddress.toString() : null;
            }
            worldKey = "mp:" + (address != null ? address : "unknown");
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
        if (saveBlocked || PerfMode.isWorldScenario()) {
            return;
        }
        JsonArray array = new JsonArray();
        for (Waypoint waypoint : WAYPOINTS) {
            array.add(waypoint.serialize(Constants.GSON));
        }
        for (JsonElement unparsedEntry : UNPARSED_ENTRIES) {
            array.add(unparsedEntry);
        }
        try {
            Files.createDirectories(waypointsFile.getParent());
            Path temporaryFile = waypointsFile.resolveSibling(waypointsFile.getFileName() + ".tmp");
            Files.writeString(temporaryFile, Constants.GSON.toJson(array), StandardCharsets.UTF_8);
            try {
                Files.move(temporaryFile, waypointsFile, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException unsupported) {
                Files.move(temporaryFile, waypointsFile, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException exception) {
            Client.LOG.warn("Failed to save waypoints: {}", exception.toString());
        }
    }
}

