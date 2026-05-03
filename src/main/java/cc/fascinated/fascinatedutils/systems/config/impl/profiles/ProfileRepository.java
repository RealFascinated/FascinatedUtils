package cc.fascinated.fascinatedutils.systems.config.impl.profiles;

import cc.fascinated.fascinatedutils.client.Client;
import cc.fascinated.fascinatedutils.systems.config.ConfigManager;
import cc.fascinated.fascinatedutils.systems.config.impl.config.FascinatedConfig;
import cc.fascinated.fascinatedutils.systems.modules.Module;
import cc.fascinated.fascinatedutils.systems.modules.ModuleRegistry;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;

@RequiredArgsConstructor
public class ProfileRepository {
    public static final String DEFAULT_PROFILE_NAME = "Default";

    @Getter
    private final Path profilesDirectory;
    private final Gson gson;
    private final ConfigManager<FascinatedConfig> configManager;

    private List<Profile> cachedProfiles = new ArrayList<>();
    private boolean profileLoadInProgress;

    public void loadActiveProfile() {
        Optional<Profile> activeProfile = getActiveProfile();
        if (activeProfile.isEmpty()) {
            return;
        }
        JsonObject moduleStates = activeProfile.get().getSettings();
        profileLoadInProgress = true;
        try {
            for (Module module : ModuleRegistry.INSTANCE.getModules()) {
                module.resetToDefault();
                JsonElement element = moduleStates.get(module.getModuleKey());
                if (element != null && element.isJsonObject()) {
                    try {
                        module.deserialize(element, gson);
                    } catch (Exception error) {
                        Client.LOG.warn("Failed to apply module settings for {}: {}", module.getModuleKey(), error.toString());
                    }
                }
            }
        } finally {
            profileLoadInProgress = false;
        }
    }

    public void saveActiveProfile() {
        if (profileLoadInProgress) {
            return;
        }
        Optional<Profile> activeProfile = getActiveProfile();
        if (activeProfile.isEmpty()) {
            return;
        }
        JsonObject capturedModules = new JsonObject();
        for (Module module : ModuleRegistry.INSTANCE.getModules()) {
            JsonObject blob = module.serialize(gson).getAsJsonObject();
            blob.addProperty("version", module.getVersion());
            capturedModules.add(module.getModuleKey(), blob);
        }
        Profile profile = activeProfile.get();
        profile.setSettings(capturedModules);
        saveProfile(profile);
    }

    public void saveModule(Module module) {
        if (profileLoadInProgress) {
            return;
        }
        Optional<Profile> activeProfile = getActiveProfile();
        if (activeProfile.isEmpty()) {
            return;
        }
        Profile profile = activeProfile.get();
        JsonObject moduleStates = profile.getSettings();
        JsonObject blob = module.serialize(gson).getAsJsonObject();
        blob.addProperty("version", module.getVersion());
        moduleStates.add(module.getModuleKey(), blob);
        profile.setSettings(moduleStates);
        saveProfile(profile);
    }

    public void refreshCache() {
        cachedProfiles = loadAllFromDisk();
        UUID activeId = getActiveProfileId().orElse(null);
        if (!cachedProfiles.isEmpty() && (activeId == null || cachedProfiles.stream().noneMatch(profile -> profile.getProfileId().equals(activeId)))) {
            setActiveProfileId(cachedProfiles.getFirst().getProfileId());
        }
    }

    public List<Profile> listProfiles() {
        return Collections.unmodifiableList(cachedProfiles);
    }

    public Optional<UUID> getActiveProfileId() {
        return Optional.ofNullable(configManager.getCurrent().activeProfileId());
    }

    public void setActiveProfileId(UUID id) {
        FascinatedConfig current = configManager.getCurrent();
        configManager.updateAndSave(new FascinatedConfig(id, current.globalSettings()));
    }

    public Optional<Profile> getActiveProfile() {
        UUID activeId = configManager.getCurrent().activeProfileId();
        if (activeId == null) {
            return Optional.empty();
        }
        return cachedProfiles.stream().filter(profile -> profile.getProfileId().equals(activeId)).findFirst();
    }

    public boolean switchActiveProfile(UUID nextProfileId) {
        if (cachedProfiles.stream().noneMatch(profile -> profile.getProfileId().equals(nextProfileId))) {
            return false;
        }
        saveActiveProfile();
        setActiveProfileId(nextProfileId);
        loadActiveProfile();
        return true;
    }

    public boolean isDefaultProfile(UUID profileId) {
        return cachedProfiles.stream().anyMatch(profile -> profile.getProfileId().equals(profileId) && DEFAULT_PROFILE_NAME.equals(profile.getProfileName()));
    }

    public boolean profileNameExists(String profileName) {
        if (profileName == null) {
            return false;
        }
        String normalized = profileName.trim().toLowerCase(java.util.Locale.ROOT);
        return cachedProfiles.stream().anyMatch(profile -> profile.getProfileName().trim().toLowerCase(java.util.Locale.ROOT).equals(normalized));
    }

    public Profile createProfile(String requestedName) {
        return createProfile(requestedName, false);
    }

    public Profile createProfile(String requestedName, boolean copyDefaultSettings) {
        JsonObject initialSettings = new JsonObject();
        if (copyDefaultSettings) {
            cachedProfiles.stream().filter(profile -> DEFAULT_PROFILE_NAME.equals(profile.getProfileName())).findFirst().ifPresent(profile -> {
                JsonObject settings = profile.getSettings();
                initialSettings.entrySet().clear();
                if (settings != null) {
                    settings.entrySet().forEach(entry -> initialSettings.add(entry.getKey(), entry.getValue()));
                }
            });
        }
        String profileName = requestedName == null || requestedName.isBlank() ? "Profile" : requestedName.trim();
        Profile created = new Profile(UUID.randomUUID(), profileName, 1, initialSettings);
        saveProfile(created);
        if (configManager.getCurrent().activeProfileId() == null) {
            setActiveProfileId(created.getProfileId());
        }
        cachedProfiles = new ArrayList<>(cachedProfiles);
        cachedProfiles.add(created);
        cachedProfiles.sort((profileA, profileB) -> profileA.getProfileName().compareToIgnoreCase(profileB.getProfileName()));
        return created;
    }

    public boolean renameProfile(UUID profileId, String newName) {
        if (newName == null || newName.isBlank()) {
            return false;
        }
        for (Profile profile : cachedProfiles) {
            if (profile.getProfileId().equals(profileId)) {
                profile.setProfileName(newName.trim());
                saveProfile(profile);
                return true;
            }
        }
        return false;
    }

    public boolean deleteProfile(UUID profileId) {
        if (isDefaultProfile(profileId)) {
            return false;
        }
        Path profilePath = profilesDirectory.resolve(profileId + ".json");
        if (!Files.isRegularFile(profilePath)) {
            return false;
        }
        try {
            Files.delete(profilePath);
        } catch (IOException error) {
            Client.LOG.warn("Failed to delete profile file {}: {}", profileId, error.toString());
            return false;
        }
        cachedProfiles = cachedProfiles.stream().filter(profile -> !profile.getProfileId().equals(profileId)).toList();
        if (profileId.equals(configManager.getCurrent().activeProfileId())) {
            setActiveProfileId(cachedProfiles.isEmpty() ? null : cachedProfiles.getFirst().getProfileId());
            loadActiveProfile();
        }
        return true;
    }

    public void saveProfile(Profile profile) {
        try {
            Files.createDirectories(profilesDirectory);
            Files.writeString(profile.resolveFilePath(profilesDirectory), gson.toJson(profile.serialize(gson)), StandardCharsets.UTF_8);
        } catch (IOException error) {
            Client.LOG.warn("Failed to save profile {}: {}", profile.getProfileId(), error.toString());
        }
    }

    private List<Profile> loadAllFromDisk() {
        if (!Files.isDirectory(profilesDirectory)) {
            return new ArrayList<>();
        }
        List<Profile> profiles = new ArrayList<>();
        try (Stream<Path> entries = Files.list(profilesDirectory)) {
            entries.filter(path -> path.getFileName().toString().endsWith(".json")).forEach(path -> {
                try {
                    String json = Files.readString(path, StandardCharsets.UTF_8);
                    JsonElement element = gson.fromJson(json, JsonElement.class);
                    if (element == null || !element.isJsonObject()) {
                        throw new IOException("Profile is not a JSON object: " + path);
                    }
                    profiles.add(migrateAndSave(Profile.defaults().deserialize(element, gson)));
                } catch (Exception error) {
                    Client.LOG.warn("Failed to load profile from {}: {}", path, error.toString());
                }
            });
        } catch (IOException error) {
            Client.LOG.warn("Failed to list profiles directory: {}", error.toString());
        }
        profiles.sort((profileA, profileB) -> profileA.getProfileName().compareToIgnoreCase(profileB.getProfileName()));
        return profiles;
    }

    private Profile migrateAndSave(Profile profile) {
        JsonObject settings = profile.getSettings();
        if (settings == null || settings.isEmpty()) {
            return profile;
        }
        JsonObject migrated = new JsonObject();
        boolean anyMigrated = false;
        for (Map.Entry<String, JsonElement> entry : settings.entrySet()) {
            String key = entry.getKey();
            JsonElement blob = entry.getValue();
            if (!blob.isJsonObject()) {
                migrated.add(key, blob);
                continue;
            }
            JsonObject originalBlob = blob.getAsJsonObject();
            int fileVersion = originalBlob.has("version") ? originalBlob.get("version").getAsInt() : 1;
            JsonObject migratedBlob = configManager.migrateModuleBlob(key, originalBlob);
            migrated.add(key, migratedBlob);
            int newVersion = migratedBlob.has("version") ? migratedBlob.get("version").getAsInt() : 1;
            if (newVersion != fileVersion) {
                anyMigrated = true;
            }
        }
        if (!anyMigrated) {
            return profile;
        }
        profile.setSettings(migrated);
        saveProfile(profile);
        return profile;
    }
}

