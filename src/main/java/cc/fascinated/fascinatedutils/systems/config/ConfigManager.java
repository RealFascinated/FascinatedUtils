package cc.fascinated.fascinatedutils.systems.config;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import lombok.Getter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.function.Supplier;

public class ConfigManager<R extends GsonSerializable<R>> {

    private final Path filePath;
    private final Class<R> rootType;
    private final Supplier<R> rootDefaults;
    private final Gson gson;

    private final List<Migration> migrations = new ArrayList<>();
    private final Map<String, List<Migration>> moduleMigrations = new HashMap<>();
    @Getter
    private R current;

    public ConfigManager(Path filePath, Class<R> rootType, Supplier<R> rootDefaults, Gson gson) {
        this.filePath = filePath;
        this.rootType = rootType;
        this.rootDefaults = rootDefaults;
        this.gson = gson;
    }

    /**
     * Registers a root-config migration step.
     * Steps are applied in ascending {@link Migration#fromVersion()} order during {@link #load()}.
     *
     * @param migration the migration step to register
     */
    public void addMigration(Migration migration) {
        migrations.add(migration);
        migrations.sort(Comparator.comparingInt(Migration::fromVersion));
    }

    /**
     * Registers a module-blob migration step for the given module key.
     * Steps are applied in ascending {@link Migration#fromVersion()} order during profile load.
     *
     * @param moduleKey the module key the migration applies to
     * @param migration the migration step to register
     */
    public void addModuleMigration(String moduleKey, Migration migration) {
        moduleMigrations.computeIfAbsent(moduleKey, ignored -> new ArrayList<>()).add(migration);
        moduleMigrations.get(moduleKey).sort(Comparator.comparingInt(Migration::fromVersion));
    }

    /**
     * Reads and deserialises the root config from disk.
     * If the on-disk version is behind the current code version, registered migration steps are
     * applied to the raw JSON in ascending order before deserialisation.
     * The upgraded result is then written back atomically.
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
            int fileVersion = root.has("version") ? root.get("version").getAsInt() : 1;
            int codeVersion = codeVersion();
            if (fileVersion > codeVersion) {
                throw new IllegalStateException("Config file version " + fileVersion + " is newer than the supported version " + codeVersion + ". Please update the mod.");
            }
            boolean migrated = fileVersion < codeVersion;
            if (migrated) {
                root = applyMigrations(root, fileVersion, codeVersion);
            }
            current = rootDefaults.get().deserialize(root, gson);
            if (migrated) {
                save();
            }
        } catch (IOException | JsonParseException exception) {
            current = rootDefaults.get();
            save();
        }
    }

    /**
     * Serialises the current root config to disk atomically.
     * The {@code "version"} field is injected automatically — config records do not manage it.
     * Writes to a {@code .tmp} sibling file first, then moves it into place.
     */
    public void save() {
        try {
            Path parent = filePath.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Path tmp = filePath.resolveSibling(filePath.getFileName() + ".tmp");
            JsonElement serialized = current.serialize(gson);
            JsonObject root = serialized.isJsonObject() ? serialized.getAsJsonObject() : new JsonObject();
            root.addProperty("version", codeVersion());
            Files.writeString(tmp, gson.toJson(root));
            Files.move(tmp, filePath, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
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
        this.current = newCurrent;
        save();
    }

    /**
     * Walks a module-settings blob through any registered migrations for its module key.
     * The {@code "version"} field in the returned object reflects the final version after migration.
     * Returns the original blob unchanged if no migrations are registered for the given key
     * or the blob is already at the current version.
     *
     * @param moduleKey the module key
     * @param blob      the raw module-settings JSON from disk
     * @return the migrated blob (same object if no migrations applied)
     * @throws IllegalStateException if the blob version is newer than the registered migration chain
     */
    public JsonObject migrateModuleBlob(String moduleKey, JsonObject blob) {
        List<Migration> steps = moduleMigrations.get(moduleKey);
        if (steps == null || steps.isEmpty()) {
            return blob;
        }
        int fileVersion = blob.has("version") ? blob.get("version").getAsInt() : 1;
        int codeVersion = steps.getLast().fromVersion() + 1;
        if (fileVersion > codeVersion) {
            throw new IllegalStateException("Module '" + moduleKey + "' blob version " + fileVersion + " is newer than supported version " + codeVersion + ". Please update the mod.");
        }
        if (fileVersion == codeVersion) {
            return blob;
        }
        JsonObject result = applyMigrationsFrom(steps, blob, fileVersion, codeVersion);
        result.addProperty("version", codeVersion);
        return result;
    }

    private int codeVersion() {
        ConfigVersion annotation = rootType.getAnnotation(ConfigVersion.class);
        return annotation != null ? annotation.value() : 1;
    }

    private JsonObject applyMigrations(JsonObject raw, int fromVersion, int toVersion) {
        return applyMigrationsFrom(migrations, raw, fromVersion, toVersion);
    }

    private JsonObject applyMigrationsFrom(List<Migration> steps, JsonObject raw, int fromVersion, int toVersion) {
        JsonObject result = raw;
        for (Migration step : steps) {
            if (step.fromVersion() >= fromVersion && step.fromVersion() < toVersion) {
                result = step.apply(result);
            }
        }
        return result;
    }
}
