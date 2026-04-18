package cc.fascinated.fascinatedutils.systems.config;

import cc.fascinated.fascinatedutils.FascinatedUtils;
import cc.fascinated.fascinatedutils.client.Client;
import cc.fascinated.fascinatedutils.common.FileUtils;
import cc.fascinated.fascinatedutils.common.JsonUtils;
import cc.fascinated.fascinatedutils.event.FascinatedEventBus;
import cc.fascinated.fascinatedutils.event.impl.lifecycle.ClientStoppingEvent;
import cc.fascinated.fascinatedutils.settings.SettingsRegistry;
import cc.fascinated.fascinatedutils.systems.config.profiles.Profile;
import cc.fascinated.fascinatedutils.systems.config.profiles.ProfileRepository;
import cc.fascinated.fascinatedutils.systems.config.serialization.impl.ModSerializationGson;
import cc.fascinated.fascinatedutils.systems.modules.Module;
import cc.fascinated.fascinatedutils.systems.modules.ModuleRegistry;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import meteordevelopment.orbit.EventHandler;
import net.fabricmc.loader.api.FabricLoader;

import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class ModConfig {
    public static final ModConfig INSTANCE = new ModConfig();

    private static final String CONFIG_FILE_NAME = "config.json";
    private static final String PROFILES_DIRECTORY_NAME = "profiles";
    private static final String DEFAULT_PROFILE_NAME = "Default";
    private static final String SETTINGS_FIELD = "settings";
    private static final String UI_STATE_FIELD = "ui_state";
    private static final String LAST_SHELL_CONTENT_TAB_KEY_FIELD = "last_shell_content_tab";
    private static boolean profileLoadInProgress;
    private final ProfileRepository repository;
    private List<Profile> profiles;
    private UUID activeProfileId;

    private ModConfig() {
        FascinatedEventBus.INSTANCE.subscribe(this);
        repository = new ProfileRepository(getDirectory().resolve(PROFILES_DIRECTORY_NAME), ModSerializationGson.get());
        List<Profile> existing = repository.listProfiles();
        boolean hasDefault = existing.stream().anyMatch(profile -> DEFAULT_PROFILE_NAME.equals(profile.getProfileName()));
        if (existing.isEmpty() || !hasDefault) {
            repository.createProfile(DEFAULT_PROFILE_NAME);
        }
        refreshProfileCache();
    }

    public static Path getDirectory() {
        return FabricLoader.getInstance().getConfigDir().resolve(FascinatedUtils.MOD_ID);
    }

    public static Path getConfigPath() {
        return getDirectory().resolve(CONFIG_FILE_NAME);
    }

    public static void loadAllModuleSettings() {
        loadActiveProfile();
    }

    public static void loadSettings() {
        JsonObject root = loadConfigRoot();
        JsonObject settingsJson = JsonUtils.objectMember(root, SETTINGS_FIELD);
        if (settingsJson != null) {
            SettingsPersistentState.apply(SettingsRegistry.INSTANCE.getSettings(), settingsJson);
        }
    }

    public static void saveSettings() {
        JsonObject root = loadConfigRoot();
        root.add(SETTINGS_FIELD, SettingsPersistentState.capture(SettingsRegistry.INSTANCE.getSettings()));
        writeConfigRoot(root);
    }

    public static void saveAllModuleSettings() {
        saveActiveProfile();
    }

    public static void loadModuleSettings(Module module) {
        Optional<Profile> activeProfile = resolveActiveProfile();
        if (activeProfile.isEmpty()) {
            return;
        }
        module.resetToDefault();
        applyModuleStateIfPresent(module, activeProfile.get().getSettings().get(module.getModuleKey()), ModSerializationGson.get());
    }

    public static void saveModuleSettings(Module module) {
        saveActiveModule(module);
    }

    public static void loadActiveProfile() {
        Optional<Profile> activeProfile = resolveActiveProfile();
        if (activeProfile.isEmpty()) {
            return;
        }
        JsonObject moduleStates = activeProfile.get().getSettings();
        profileLoadInProgress = true;
        try {
            Gson gsonInstance = ModSerializationGson.get();
            for (Module module : ModuleRegistry.INSTANCE.getModules()) {
                module.resetToDefault();
                applyModuleStateIfPresent(module, moduleStates.get(module.getModuleKey()), gsonInstance);
            }
        } finally {
            profileLoadInProgress = false;
        }
    }

    public static void saveActiveProfile() {
        if (profileLoadInProgress) {
            return;
        }
        Optional<Profile> activeProfile = resolveActiveProfile();
        if (activeProfile.isEmpty()) {
            return;
        }
        Gson gsonInstance = ModSerializationGson.get();
        JsonObject capturedModules = new JsonObject();
        for (Module module : ModuleRegistry.INSTANCE.getModules()) {
            capturedModules.add(module.getModuleKey(), gsonInstance.toJsonTree(ModulePersistentState.capture(module)));
        }
        Profile profile = activeProfile.get();
        profile.setSettings(capturedModules);
        INSTANCE.repository.saveProfile(profile);
    }

    public static void saveActiveModule(Module module) {
        if (profileLoadInProgress) {
            return;
        }
        Optional<Profile> activeProfile = resolveActiveProfile();
        if (activeProfile.isEmpty()) {
            return;
        }
        Gson gsonInstance = ModSerializationGson.get();
        Profile profile = activeProfile.get();
        JsonObject moduleStates = profile.getSettings();
        moduleStates.add(module.getModuleKey(), gsonInstance.toJsonTree(ModulePersistentState.capture(module)));
        profile.setSettings(moduleStates);
        INSTANCE.repository.saveProfile(profile);
    }

    public static boolean switchActiveProfile(UUID nextProfileId) {
        if (INSTANCE.profiles.stream().noneMatch(profile -> profile.getProfileId().equals(nextProfileId))) {
            return false;
        }
        saveActiveProfile();
        INSTANCE.activeProfileId = nextProfileId;
        INSTANCE.repository.setActiveProfileId(nextProfileId);
        loadActiveProfile();
        return true;
    }

    public static Profile createProfile(String profileName) {
        return createProfile(profileName, false);
    }

    public static Profile createProfile(String profileName, boolean copyDefaultProfileSettings) {
        JsonObject initialSettings = new JsonObject();
        if (copyDefaultProfileSettings) {
            for (Profile profile : INSTANCE.profiles) {
                if (DEFAULT_PROFILE_NAME.equals(profile.getProfileName())) {
                    JsonObject settings = profile.getSettings();
                    initialSettings = settings == null ? new JsonObject() : settings.deepCopy();
                    break;
                }
            }
        }
        Profile created = INSTANCE.repository.createProfile(profileName, initialSettings);
        INSTANCE.refreshProfileCache();
        return created;
    }

    public static boolean profileNameExists(String profileName) {
        if (profileName == null) {
            return false;
        }
        String normalized = profileName.trim().toLowerCase(java.util.Locale.ROOT);
        return INSTANCE.profiles.stream().anyMatch(profile -> profile.getProfileName().trim().toLowerCase(java.util.Locale.ROOT).equals(normalized));
    }

    public static List<Profile> listProfiles() {
        return INSTANCE.profiles;
    }

    public static boolean isDefaultProfile(UUID profileId) {
        return INSTANCE.profiles.stream().anyMatch(profile -> profile.getProfileId().equals(profileId) && DEFAULT_PROFILE_NAME.equals(profile.getProfileName()));
    }

    public static boolean deleteProfile(UUID profileId) {
        if (isDefaultProfile(profileId)) {
            return false;
        }
        boolean deleted = INSTANCE.repository.deleteProfile(profileId);
        if (!deleted) {
            return false;
        }
        INSTANCE.refreshProfileCache();
        loadActiveProfile();
        return true;
    }

    public static boolean renameProfile(UUID profileId, String newName) {
        if (newName == null || newName.isBlank()) {
            return false;
        }
        for (Profile profile : INSTANCE.profiles) {
            if (profile.getProfileId().equals(profileId)) {
                profile.setProfileName(newName.trim());
                INSTANCE.repository.saveProfile(profile);
                return true;
            }
        }
        return false;
    }

    public static Optional<UUID> getActiveProfileId() {
        return Optional.ofNullable(INSTANCE.activeProfileId);
    }

    public static Optional<String> loadLastShellContentTabKey() {
        JsonObject uiStateRoot = JsonUtils.objectMember(loadConfigRoot(), UI_STATE_FIELD);
        if (uiStateRoot == null) {
            return Optional.empty();
        }
        String value = JsonUtils.stringMember(uiStateRoot, LAST_SHELL_CONTENT_TAB_KEY_FIELD);
        return (value == null || value.isBlank()) ? Optional.empty() : Optional.of(value);
    }

    public static void saveLastShellContentTabKey(String tabKey) {
        JsonObject root = loadConfigRoot();
        JsonObject uiStateRoot = JsonUtils.objectMember(root, UI_STATE_FIELD);
        if (uiStateRoot == null) {
            uiStateRoot = new JsonObject();
            root.add(UI_STATE_FIELD, uiStateRoot);
        }
        uiStateRoot.addProperty(LAST_SHELL_CONTENT_TAB_KEY_FIELD, tabKey);
        writeConfigRoot(root);
    }

    public static JsonObject loadConfigRoot() {
        Path path = getConfigPath();
        if (!Files.isRegularFile(path)) {
            return new JsonObject();
        }
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            JsonElement element = JsonParser.parseReader(reader);
            if (element == null || !element.isJsonObject()) {
                return new JsonObject();
            }
            return element.getAsJsonObject();
        } catch (Exception error) {
            Client.LOG.warn("Failed to load config from {}: {}", path, error.toString());
            return new JsonObject();
        }
    }

    public static void writeConfigRoot(JsonObject root) {
        try {
            FileUtils.ensureDirectoryExists(getDirectory());
            Files.writeString(getConfigPath(), ModSerializationGson.get().toJson(root), StandardCharsets.UTF_8);
        } catch (Exception error) {
            Client.LOG.warn("Failed to save JSON file {}: {}", getConfigPath(), error.toString());
        }
    }

    private static Optional<Profile> resolveActiveProfile() {
        UUID id = INSTANCE.activeProfileId;
        if (id == null) {
            return Optional.empty();
        }
        return INSTANCE.profiles.stream().filter(profile -> profile.getProfileId().equals(id)).findFirst();
    }

    private static void applyModuleStateIfPresent(Module module, JsonElement moduleElement, Gson gsonInstance) {
        if (moduleElement == null || !moduleElement.isJsonObject()) {
            return;
        }
        try {
            ModulePersistentState state = gsonInstance.fromJson(moduleElement, ModulePersistentState.class);
            if (state != null) {
                state.applyTo(module);
            }
        } catch (Exception error) {
            Client.LOG.warn("Failed to apply module settings for {}: {}", module.getModuleKey(), error.toString());
        }
    }

    private void refreshProfileCache() {
        profiles = new ArrayList<>(repository.listProfiles());
        activeProfileId = repository.getActiveProfileId();
        if (!profiles.isEmpty() && (activeProfileId == null || profiles.stream().noneMatch(profile -> profile.getProfileId().equals(activeProfileId)))) {
            activeProfileId = profiles.get(0).getProfileId();
            repository.setActiveProfileId(activeProfileId);
        }
    }

    @EventHandler
    private void fascinatedutils$onClientStopping(ClientStoppingEvent event) {
        saveActiveProfile();
        saveSettings();
    }
}