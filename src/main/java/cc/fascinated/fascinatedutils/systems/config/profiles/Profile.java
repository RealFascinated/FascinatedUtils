package cc.fascinated.fascinatedutils.systems.config.profiles;

import cc.fascinated.fascinatedutils.common.JsonUtils;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.Getter;
import lombok.Setter;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

@Getter
@Setter
public class Profile {
    private static final String PROFILE_ID_FIELD = "profile_id";
    private static final String PROFILE_NAME_FIELD = "profile_name";
    private static final String SETTINGS_FIELD = "settings";
    private UUID profileId;
    private String profileName;
    private JsonObject settings;

    public Profile(UUID profileId, String profileName, JsonObject settings) {
        this.profileId = profileId;
        this.profileName = profileName;
        this.settings = settings != null ? settings : new JsonObject();
    }

    /**
     * Loads and parses a profile JSON file.
     *
     * @param profilePath path to a profile file
     * @return parsed profile model
     * @throws IOException when the file is missing or malformed
     */
    public static Profile load(Path profilePath) throws IOException {
        return load(profilePath, null);
    }

    /**
     * Loads and parses a profile JSON file with the provided Gson instance.
     *
     * @param profilePath path to a profile file
     * @param gson        Gson instance used for JSON parsing, or null to use default parsing
     * @return parsed profile model
     * @throws IOException when the file is missing or malformed
     */
    public static Profile load(Path profilePath, Gson gson) throws IOException {
        if (!Files.isRegularFile(profilePath)) {
            throw new IOException("Profile file does not exist: " + profilePath);
        }
        try (Reader reader = Files.newBufferedReader(profilePath, StandardCharsets.UTF_8)) {
            JsonElement element = gson == null ? JsonParser.parseReader(reader) : gson.fromJson(reader, JsonElement.class);
            if (element == null || !element.isJsonObject()) {
                throw new IOException("Profile root is not an object: " + profilePath);
            }
            JsonObject root = element.getAsJsonObject();
            String idText = JsonUtils.stringMember(root, PROFILE_ID_FIELD);
            if (idText == null || idText.isBlank()) {
                throw new IOException("Profile id is missing: " + profilePath);
            }
            String name = JsonUtils.stringMember(root, PROFILE_NAME_FIELD, "Profile");
            JsonObject parsedSettings = JsonUtils.objectMemberOrEmpty(root, SETTINGS_FIELD);
            return new Profile(UUID.fromString(idText), name, parsedSettings);
        }
    }

    /**
     * Saves this profile to disk using pretty JSON formatting from the shared Gson.
     *
     * @param profilesDirectory directory where profile files are stored
     * @param gson              Gson instance configured for mod persistence
     * @throws IOException when writing to disk fails
     */
    public void save(Path profilesDirectory, Gson gson) throws IOException {
        Files.createDirectories(profilesDirectory);
        JsonObject root = new JsonObject();
        root.addProperty(PROFILE_ID_FIELD, profileId.toString());
        root.addProperty(PROFILE_NAME_FIELD, profileName);
        root.add(SETTINGS_FIELD, settings != null ? settings : new JsonObject());
        Files.writeString(resolveFilePath(profilesDirectory), gson.toJson(root), StandardCharsets.UTF_8);
    }

    public Path resolveFilePath(Path profilesDirectory) {
        return profilesDirectory.resolve(profileId + ".json");
    }
}