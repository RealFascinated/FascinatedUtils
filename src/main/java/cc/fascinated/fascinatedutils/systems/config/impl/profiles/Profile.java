package cc.fascinated.fascinatedutils.systems.config.profiles;

import cc.fascinated.fascinatedutils.systems.config.serialization.GsonSerializable;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import lombok.Getter;
import lombok.Setter;

import java.nio.file.Path;
import java.util.UUID;

@Getter
@Setter
public class Profile implements GsonSerializable<Profile> {
    private static final String PROFILE_ID_FIELD = "profile_id";
    private static final String PROFILE_NAME_FIELD = "profile_name";
    private static final String PROFILE_VERSION_FIELD = "profile_version";
    private static final String SETTINGS_FIELD = "settings";

    private UUID profileId;
    private String profileName;
    private int profileVersion;
    private JsonObject settings;

    public Profile(UUID profileId, String profileName, int profileVersion, JsonObject settings) {
        this.profileId = profileId;
        this.profileName = profileName;
        this.profileVersion = profileVersion;
        this.settings = settings != null ? settings : new JsonObject();
    }

    public static Profile defaults() {
        return new Profile(null, "Profile", 1, new JsonObject());
    }

    @Override
    public JsonElement serialize(Gson gson) {
        JsonObject root = new JsonObject();
        root.addProperty(PROFILE_ID_FIELD, profileId.toString());
        root.addProperty(PROFILE_NAME_FIELD, profileName);
        root.addProperty(PROFILE_VERSION_FIELD, profileVersion);
        root.add(SETTINGS_FIELD, settings != null ? settings : new JsonObject());
        return root;
    }

    @Override
    public Profile deserialize(JsonElement data, Gson gson) {
        JsonObject root = data.getAsJsonObject();
        String idText = root.has(PROFILE_ID_FIELD) ? root.get(PROFILE_ID_FIELD).getAsString() : null;
        UUID parsedId = UUID.fromString(idText);
        String name = root.has(PROFILE_NAME_FIELD) ? root.get(PROFILE_NAME_FIELD).getAsString() : "Profile";
        int version = root.has(PROFILE_VERSION_FIELD) ? root.get(PROFILE_VERSION_FIELD).getAsInt() : 1;
        JsonObject parsedSettings = root.has(SETTINGS_FIELD) && root.get(SETTINGS_FIELD).isJsonObject()
                ? root.get(SETTINGS_FIELD).getAsJsonObject() : new JsonObject();
        return new Profile(parsedId, name, version, parsedSettings);
    }

    public Path resolveFilePath(Path profilesDirectory) {
        return profilesDirectory.resolve(profileId + ".json");
    }
}
