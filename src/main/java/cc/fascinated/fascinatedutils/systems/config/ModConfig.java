package cc.fascinated.fascinatedutils.systems.config;

import cc.fascinated.fascinatedutils.Constants;
import cc.fascinated.fascinatedutils.AlumiteMod;
import cc.fascinated.fascinatedutils.event.FascinatedEventBus;
import cc.fascinated.fascinatedutils.event.impl.lifecycle.ClientStoppingEvent;
import cc.fascinated.fascinatedutils.systems.config.impl.config.ConfigRepository;
import cc.fascinated.fascinatedutils.systems.config.impl.config.FascinatedConfig;
import cc.fascinated.fascinatedutils.systems.config.impl.profiles.ProfileRepository;
import cc.fascinated.fascinatedutils.systems.config.impl.waypoint.WaypointRepository;
import meteordevelopment.orbit.EventHandler;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Path;

public class ModConfig {
    public static final ModConfig INSTANCE = new ModConfig();

    private final ProfileRepository profileRepository;
    private final ConfigRepository configRepository;
    private final WaypointRepository waypointRepository;

    private ModConfig() {
        FascinatedEventBus.INSTANCE.subscribe(this);

        ConfigManager<FascinatedConfig> configManager = new ConfigManager<>(getConfigPath(), FascinatedConfig.class, FascinatedConfig::defaults, Constants.GSON);
        configManager.load();

        profileRepository = new ProfileRepository(getDirectory().resolve("profiles"), Constants.GSON, configManager);
        profileRepository.refreshCache();
        if (!profileRepository.profileNameExists(ProfileRepository.DEFAULT_PROFILE_NAME)) {
            profileRepository.createProfile(ProfileRepository.DEFAULT_PROFILE_NAME);
        }

        configRepository = new ConfigRepository(configManager);

        waypointRepository = new WaypointRepository(getDirectory().resolve("waypoints.json"), Constants.GSON);
        waypointRepository.refreshCache();
    }

    public static Path getDirectory() {
        return FabricLoader.getInstance().getConfigDir().resolve(AlumiteMod.MOD_ID);
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

    public static WaypointRepository waypoints() {
        return INSTANCE.waypointRepository;
    }

    @EventHandler
    private void alumite$onClientStopping(ClientStoppingEvent event) {
        profileRepository.saveActiveProfile();
        configRepository.save();
        waypointRepository.save();
    }
}
