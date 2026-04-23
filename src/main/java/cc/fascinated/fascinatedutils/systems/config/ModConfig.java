package cc.fascinated.fascinatedutils.systems.config;

import cc.fascinated.fascinatedutils.FascinatedUtils;
import cc.fascinated.fascinatedutils.event.FascinatedEventBus;
import cc.fascinated.fascinatedutils.event.impl.lifecycle.ClientStoppingEvent;
import cc.fascinated.fascinatedutils.systems.config.impl.config.ConfigRepository;
import cc.fascinated.fascinatedutils.systems.config.impl.config.FascinatedConfig;
import cc.fascinated.fascinatedutils.systems.config.impl.profiles.ProfileRepository;
import cc.fascinated.fascinatedutils.systems.config.impl.settings.UIStateRepository;
import cc.fascinated.fascinatedutils.systems.config.impl.waypoint.WaypointRepository;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import meteordevelopment.orbit.EventHandler;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Path;

public class ModConfig {
    public static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    public static final ModConfig INSTANCE = new ModConfig();

    private final ProfileRepository profileRepository;
    private final ConfigRepository configRepository;
    private final UIStateRepository uiStateRepository;
    private final WaypointRepository waypointRepository;

    private ModConfig() {
        FascinatedEventBus.INSTANCE.subscribe(this);

        ConfigManager<FascinatedConfig> configManager = new ConfigManager<>(getConfigPath(), FascinatedConfig.class, FascinatedConfig::defaults, GSON);
        configManager.load();

        profileRepository = new ProfileRepository(getDirectory().resolve("profiles"), GSON, configManager);
        profileRepository.refreshCache();
        if (!profileRepository.profileNameExists(ProfileRepository.DEFAULT_PROFILE_NAME)) {
            profileRepository.createProfile(ProfileRepository.DEFAULT_PROFILE_NAME);
        }

        configRepository = new ConfigRepository(configManager);
        uiStateRepository = new UIStateRepository(configManager);

        waypointRepository = new WaypointRepository(getDirectory().resolve("waypoints.json"), GSON);
        waypointRepository.refreshCache();
    }

    public static Path getDirectory() {
        return FabricLoader.getInstance().getConfigDir().resolve(FascinatedUtils.MOD_ID);
    }

    public static Path getConfigPath() {
        return getDirectory().resolve("config.json");
    }

    public static ProfileRepository profiles() {
        return INSTANCE.profileRepository;
    }

    public static ConfigRepository config() {
        return INSTANCE.configRepository;
    }

    public static UIStateRepository uiState() {
        return INSTANCE.uiStateRepository;
    }

    public static WaypointRepository waypoints() {
        return INSTANCE.waypointRepository;
    }

    @EventHandler
    private void fascinatedutils$onClientStopping(ClientStoppingEvent event) {
        profileRepository.saveActiveProfile();
        configRepository.save();
        waypointRepository.save();
    }
}
