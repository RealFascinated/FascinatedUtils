package cc.fascinated.fascinatedutils.systems.config.impl.waypoint;

import cc.fascinated.fascinatedutils.systems.config.GsonSerializable;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class Waypoint implements GsonSerializable<Waypoint> {
    private UUID id;
    private WaypointType type;
    private String name;
    /**
     * Server IP for multiplayer, or level name for singleplayer.
     */
    private String worldKey;
    private double x;
    private double y;
    private double z;
    private String dimension;
    private boolean visible;
    private boolean showBeam;
    private boolean showDistance;
    private int color;

    public Waypoint(UUID id, WaypointType type, String name, String worldKey, double x, double y, double z, String dimension, boolean visible, boolean showBeam, boolean showDistance, int color) {
        this.id = id;
        this.type = type;
        this.name = name;
        this.worldKey = worldKey;
        this.x = x;
        this.y = y;
        this.z = z;
        this.dimension = dimension;
        this.visible = visible;
        this.showBeam = showBeam;
        this.showDistance = showDistance;
        this.color = color;
    }

    public static Waypoint defaults() {
        return new Waypoint(null, WaypointType.NORMAL, "", "", 0, 0, 0, "minecraft:overworld", true, true, true, 0xFFFFFFFF);
    }

    @Override
    public JsonElement serialize(Gson gson) {
        JsonObject root = new JsonObject();
        root.addProperty("id", this.id.toString());
        root.addProperty("type", this.type.toString());
        root.addProperty("name", this.name);
        root.addProperty("world_key", this.worldKey);
        root.addProperty("x", this.x);
        root.addProperty("y", this.y);
        root.addProperty("z", this.z);
        root.addProperty("dimension", this.dimension);
        root.addProperty("color", this.color);
        root.addProperty("visible", this.visible);
        root.addProperty("show_beam", this.showBeam);
        root.addProperty("show_distance", this.showDistance);
        return root;
    }

    @Override
    public Waypoint deserialize(JsonElement data, Gson gson) {
        JsonObject root = data.getAsJsonObject();
        return new Waypoint(UUID.fromString(root.get("id").getAsString()), WaypointType.valueOf(root.get("type").getAsString()), root.get("name").getAsString(), root.get("world_key").getAsString(), root.get("x").getAsDouble(), root.get("y").getAsDouble(), root.get("z").getAsDouble(), root.get("dimension").getAsString(), root.get("visible").getAsBoolean(), !root.has("show_beam") || root.get("show_beam").getAsBoolean(), !root.has("show_distance") || root.get("show_distance").getAsBoolean(), root.get("color").getAsInt());
    }
}
