package cc.fascinated.fascinatedutils.systems.config.profiles;

import cc.fascinated.fascinatedutils.client.Client;
import cc.fascinated.fascinatedutils.common.JsonUtils;
import cc.fascinated.fascinatedutils.systems.config.ModConfig;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

@RequiredArgsConstructor
public class ProfileRepository {
    private static final String ACTIVE_PROFILE_ID_FIELD = "active_profile_id";
    @Getter
    private final Path profilesDirectory;
    private final Gson gson;

    private static String normalizeProfileName(String requestedName) {
        if (requestedName == null) {
            return "Profile";
        }
        String trimmedName = requestedName.trim();
        if (trimmedName.isEmpty()) {
            return "Profile";
        }
        return trimmedName;
    }

    public UUID getActiveProfileId() {
        return JsonUtils.uuidMember(ModConfig.loadConfigRoot(), ACTIVE_PROFILE_ID_FIELD);
    }

    public void setActiveProfileId(UUID id) {
        JsonObject root = ModConfig.loadConfigRoot();
        if (id == null) {
            root.remove(ACTIVE_PROFILE_ID_FIELD);
        }
        else {
            root.addProperty(ACTIVE_PROFILE_ID_FIELD, id.toString());
        }
        ModConfig.writeConfigRoot(root);
    }

    public List<Profile> listProfiles() {
        if (!Files.isDirectory(profilesDirectory)) {
            return List.of();
        }
        List<Profile> profiles = new ArrayList<>();
        try (Stream<Path> entries = Files.list(profilesDirectory)) {
            entries.filter(path -> path.getFileName().toString().endsWith(".json")).forEach(path -> {
                try {
                    profiles.add(Profile.load(path, gson));
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

    public Profile createProfile(String requestedName) {
        return createProfile(requestedName, new JsonObject());
    }

    public Profile createProfile(String requestedName, JsonObject initialSettings) {
        String profileName = normalizeProfileName(requestedName);
        UUID profileId = UUID.randomUUID();
        Profile createdProfile = new Profile(profileId, profileName, initialSettings == null ? new JsonObject() : initialSettings);
        saveProfile(createdProfile);
        if (getActiveProfileId() == null) {
            setActiveProfileId(profileId);
        }
        return createdProfile;
    }

    public Optional<Profile> loadProfile(UUID profileId) {
        Path profilePath = profilesDirectory.resolve(profileId + ".json");
        if (!Files.isRegularFile(profilePath)) {
            return Optional.empty();
        }
        try {
            return Optional.of(Profile.load(profilePath, gson));
        } catch (Exception error) {
            Client.LOG.warn("Failed to load profile {}: {}", profileId, error.toString());
            return Optional.empty();
        }
    }

    public void saveProfile(Profile profile) {
        try {
            profile.save(profilesDirectory, gson);
        } catch (IOException error) {
            Client.LOG.warn("Failed to save profile {}: {}", profile.getProfileId(), error.toString());
        }
    }

    public boolean deleteProfile(UUID profileId) {
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
        if (profileId.equals(getActiveProfileId())) {
            List<Profile> remaining = listProfiles();
            setActiveProfileId(remaining.isEmpty() ? null : remaining.get(0).getProfileId());
        }
        return true;
    }
}
