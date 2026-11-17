package cc.fascinated.fascinatedutils.systems.config.impl.profiles;

import cc.fascinated.fascinatedutils.client.Client;
import cc.fascinated.fascinatedutils.systems.config.io.AtomicFiles;
import cc.fascinated.fascinatedutils.systems.config.ConfigManager;
import cc.fascinated.fascinatedutils.systems.config.impl.config.FascinatedConfig;
import cc.fascinated.fascinatedutils.systems.modules.Module;
import cc.fascinated.fascinatedutils.systems.modules.ModuleRegistry;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
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
        Profile profile = activeProfile.get();
        profile.setSettings(captureModuleSettings());
        saveProfile(profile);
    }

    /**
     * Serialises every module's settings, each stamped with its {@code version} field, as stored in a profile.
     *
     * @return one settings blob per module key
     */
    public JsonObject captureModuleSettings() {
        JsonObject capturedModules = new JsonObject();
        for (Module module : ModuleRegistry.INSTANCE.getModules()) {
            JsonObject blob = module.serialize(gson).getAsJsonObject();
            blob.addProperty("version", module.getVersion());
            capturedModules.add(module.getModuleKey(), blob);
        }
        return capturedModules;
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
        boolean activeMissing = activeId == null || cachedProfiles.stream().noneMatch(profile -> profile.getProfileId().equals(activeId));
        if (activeMissing && activeId != null && Files.exists(profilesDirectory.resolve(activeId + ".json"))) {
            // The active profile file exists but failed to load; keep the file and the active id untouched.
            Client.LOG.error("Active profile {} exists on disk but could not be loaded; leaving it untouched", activeId);
            return;
        }
        if (!cachedProfiles.isEmpty() && activeMissing) {
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
        String profileName = requestedName == null || requestedName.isBlank() ? "Profile" : requestedName.trim();
        Profile created = new Profile(UUID.randomUUID(), profileName, 1, new JsonObject());
        saveProfile(created);
        UUID activeId = configManager.getCurrent().activeProfileId();
        if (activeId == null || cachedProfiles.stream().noneMatch(profile -> profile.getProfileId().equals(activeId))) {
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
        writeProfile(profile, profilesDirectory);
    }

    /**
     * Writes a profile atomically into a directory.
     *
     * @param profile   the profile to write
     * @param directory the directory that holds profile files
     */
    public void writeProfile(Profile profile, Path directory) {
        try {
            AtomicFiles.INSTANCE.writeAtomically(profile.resolveFilePath(directory), profile.serialize(gson), gson);
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
                        throw new JsonParseException("Profile is not a JSON object: " + path);
                    }
                    profiles.add(Profile.defaults().deserialize(element, gson));
                } catch (JsonParseException error) {
                    quarantineCorruptProfile(path, error);
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

    /**
     * Moves an unparseable profile file aside so it is kept for recovery instead of being overwritten.
     *
     * @param path  the profile file that failed to parse
     * @param error the parse failure
     */
    private void quarantineCorruptProfile(Path path, Exception error) {
        try {
            Path corruptPath = AtomicFiles.INSTANCE.quarantine(path, false);
            Client.LOG.warn("Profile {} is corrupt ({}); moved to {}", path, error.toString(), corruptPath);
        } catch (IOException moveError) {
            Client.LOG.warn("Profile {} is corrupt ({}) and could not be moved aside: {}", path, error.toString(), moveError.toString());
        }
    }
}

