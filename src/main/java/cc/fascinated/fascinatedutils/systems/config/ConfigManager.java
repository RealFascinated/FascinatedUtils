package cc.fascinated.fascinatedutils.systems.config;

import cc.fascinated.fascinatedutils.client.Client;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import lombok.Getter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Supplier;

public class ConfigManager<R extends GsonSerializable<R>> {

    private final Path filePath;
    private final Class<R> rootType;
    private final Supplier<R> rootDefaults;
    private final Gson gson;

    @Getter
    private R current;

    public ConfigManager(Path filePath, Class<R> rootType, Supplier<R> rootDefaults, Gson gson) {
        this.filePath = filePath;
        this.rootType = rootType;
        this.rootDefaults = rootDefaults;
        this.gson = gson;
    }

    /**
     * Reads and deserialises the root config from disk.
     * If the on-disk version is behind the current code version, the config is saved again under the current version.
     * If the file is missing or malformed, defaults are used and saved immediately.
     *
     * @throws IllegalStateException if the on-disk version is newer than the code version
     */
    public void load() {
        if (!Files.exists(filePath)) {
            current = rootDefaults.get();
            save();
            return;
        }
        try {
            String json = Files.readString(filePath);
            JsonObject root = gson.fromJson(json, JsonObject.class);
            if (root == null) {
                throw new JsonParseException("file is empty or not a JSON object");
            }
            int fileVersion;
            try {
                fileVersion = root.has("version") ? root.get("version").getAsInt() : 1;
            } catch (RuntimeException exception) {
                throw new JsonParseException("invalid version field", exception);
            }
            int codeVersion = codeVersion();
            if (fileVersion > codeVersion) {
                // Intentional: fail at startup rather than overwrite a config written by a newer build.
                throw new IllegalStateException("Config file version " + fileVersion + " is newer than the supported version " + codeVersion + ". Please update the mod.");
            }
            try {
                current = rootDefaults.get().deserialize(root, gson);
            } catch (RuntimeException exception) {
                throw new JsonParseException("invalid config structure", exception);
            }
            if (fileVersion < codeVersion) {
                save();
            }
        } catch (IOException | JsonParseException exception) {
            backUpUnreadableConfig(exception);
            current = rootDefaults.get();
            save();
        }
    }

    /**
     * Copies a config file that could not be read or parsed to a {@code .corrupt} sibling so the defaults written
     * next do not destroy it.
     *
     * @param cause why the file was rejected
     */
    private void backUpUnreadableConfig(Exception cause) {
        try {
            Path backupPath = AtomicFiles.INSTANCE.quarantine(filePath, true);
            Client.LOG.warn("Config {} could not be loaded ({}); original saved to {}", filePath, cause.toString(), backupPath);
        } catch (IOException backupError) {
            Client.LOG.warn("Config {} could not be loaded ({}) and could not be backed up: {}", filePath, cause.toString(), backupError.toString());
        }
    }

    /**
     * Serialises the current root config to disk atomically.
     * The {@code "version"} field is injected automatically — config records do not manage it.
     */
    public void save() {
        JsonElement serialized = current.serialize(gson);
        JsonObject root = serialized.isJsonObject() ? serialized.getAsJsonObject() : new JsonObject();
        root.addProperty("version", codeVersion());
        try {
            AtomicFiles.INSTANCE.writeAtomically(filePath, root, gson);
        } catch (IOException exception) {
            throw new RuntimeException("Failed to save config to " + filePath, exception);
        }
    }

    /**
     * Replaces the current root config and immediately persists it.
     *
     * @param newCurrent the new root config to use
     */
    public void updateAndSave(R newCurrent) {
        R previous = this.current;
        this.current = newCurrent;
        try {
            save();
        } catch (RuntimeException exception) {
            this.current = previous;
            throw exception;
        }
    }

    private int codeVersion() {
        ConfigVersion annotation = rootType.getAnnotation(ConfigVersion.class);
        return annotation != null ? annotation.value() : 1;
    }
}
