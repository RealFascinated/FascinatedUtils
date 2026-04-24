package cc.fascinated.fascinatedutils.systems.config.impl.waypoint;

import cc.fascinated.fascinatedutils.client.Client;
import cc.fascinated.fascinatedutils.common.color.SettingColor;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import lombok.RequiredArgsConstructor;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

@RequiredArgsConstructor
public class WaypointRepository {
    private final Path waypointsFile;
    private final Gson gson;

    private List<Waypoint> cache = new ArrayList<>();

    public void refreshCache() {
        if (!Files.isRegularFile(waypointsFile)) {
            cache = new ArrayList<>();
            return;
        }
        List<Waypoint> loaded = new ArrayList<>();
        try {
            String json = Files.readString(waypointsFile, StandardCharsets.UTF_8);
            JsonElement element = gson.fromJson(json, JsonElement.class);
            if (element != null && element.isJsonArray()) {
                for (JsonElement entry : element.getAsJsonArray()) {
                    try {
                        loaded.add(Waypoint.defaults().deserialize(entry, gson));
                    } catch (Exception exception) {
                        Client.LOG.warn("Failed to load waypoint: {}", exception.toString());
                    }
                }
            }
        } catch (IOException exception) {
            Client.LOG.warn("Failed to read waypoints file: {}", exception.toString());
        }
        cache = loaded;
    }

    /**
     * Returns all waypoints for the given server address.
     *
     * @param server the server address (or world name for singleplayer)
     * @return unmodifiable list of matching waypoints
     */
    /**
     * Returns all waypoints matching the given world key.
     * For multiplayer, pass the server IP; for singleplayer, pass the level name.
     *
     * @param worldKey the server IP or singleplayer level name
     * @return matching waypoints
     */
    public List<Waypoint> getForWorld(String worldKey) {
        return cache.stream().filter(waypoint -> waypoint.getWorldKey().equals(worldKey)).toList();
    }

    /**
     * Returns all waypoints across all servers.
     *
     * @return unmodifiable view of the full waypoint list
     */
    public List<Waypoint> list() {
        return Collections.unmodifiableList(cache);
    }

    /**
     * Finds a waypoint by its unique ID.
     *
     * @param id the waypoint UUID
     * @return the matching waypoint, if present
     */
    public Optional<Waypoint> findById(UUID id) {
        return cache.stream().filter(waypoint -> waypoint.getId().equals(id)).findFirst();
    }

    /**
     * Creates a new waypoint, adds it to the cache, and persists it.
     *
     * @param name      display name
     * @param worldKey  prefixed world key: {@code sp:<levelName>} or {@code mp:<serverIP>}
     * @param type      waypoint type
     * @param x         world X coordinate
     * @param y         world Y coordinate
     * @param z         world Z coordinate
     * @param dimension dimension resource location (e.g. "minecraft:overworld")
     * @param color     ARGB color
     * @return the newly created waypoint
     */
    public Waypoint create(String name, String worldKey, WaypointType type, double x, double y, double z, String dimension, SettingColor color) {
        Waypoint waypoint = new Waypoint(UUID.randomUUID(), type, name, worldKey, x, y, z, dimension, true, true, true, color.copy());
        cache.add(waypoint);
        save();
        return waypoint;
    }

    /**
     * Deletes the waypoint with the given ID and persists the change.
     *
     * @param id the waypoint UUID
     * @return true if a waypoint was removed, false if not found
     */
    public boolean delete(UUID id) {
        List<Waypoint> updated = cache.stream().filter(waypoint -> !waypoint.getId().equals(id)).toList();
        if (updated.size() == cache.size()) {
            return false;
        }
        cache = new ArrayList<>(updated);
        save();
        return true;
    }

    /**
     * Persists all cached waypoints to disk.
     */
    public void save() {
        JsonArray array = new JsonArray();
        for (Waypoint waypoint : cache) {
            array.add(waypoint.serialize(gson));
        }
        try {
            Files.createDirectories(waypointsFile.getParent());
            Files.writeString(waypointsFile, gson.toJson(array), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            Client.LOG.warn("Failed to save waypoints: {}", exception.toString());
        }
    }
}

