package cc.fascinated.fascinatedutils.debug.perf.tests.config;

import cc.fascinated.fascinatedutils.client.Client;
import cc.fascinated.fascinatedutils.debug.perf.core.PerfTest;
import cc.fascinated.fascinatedutils.debug.perf.coverage.PerfCoverage;
import cc.fascinated.fascinatedutils.systems.config.ModConfig;
import cc.fascinated.fascinatedutils.systems.config.impl.profiles.Profile;
import cc.fascinated.fascinatedutils.systems.modules.ModuleRegistry;
import cc.fascinated.fascinatedutils.systems.modules.impl.inventorytweaks.InventoryTweaksModule;
import cc.fascinated.fascinatedutils.common.setting.impl.BooleanSetting;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

/**
 * The real profile save path as in {@link ConfigWriteProfilePerfTest}, but every call first flips one setting, so each
 * save writes content that differs from the previous save. The setting is restored after the measured iterations.
 */
public class ConfigWriteChangedProfilePerfTest extends PerfTest {
    private static final UUID PERF_PROFILE_ID = UUID.fromString("00000000-0000-0000-0000-00000000fe50");
    private static final int PROFILE_VERSION = 1;

    private Path directory;
    private BooleanSetting toggledSetting;
    private boolean originalValue;

    public ConfigWriteChangedProfilePerfTest() {
        super("config.save_profile_changed", "Flip one setting, then capture all module settings and write a profile atomically to a temp dir", 3, 30);
        this.warmupMillis = 100;
    }

    @Override
    public void setup() {
        try {
            directory = Files.createTempDirectory("alumite-perf-profile");
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
        toggledSetting = ModuleRegistry.INSTANCE.getModule(InventoryTweaksModule.class).orElseThrow().getQuickMove();
        originalValue = toggledSetting.isEnabled();
    }

    @Override
    public void runOnce() {
        toggledSetting.setValue(!toggledSetting.isEnabled());
        Profile profile = new Profile(PERF_PROFILE_ID, "perf", PROFILE_VERSION, ModConfig.profiles().captureModuleSettings());
        ModConfig.profiles().writeProfile(profile, directory);
    }

    @Override
    public void teardown() {
        if (toggledSetting != null) {
            toggledSetting.setValue(originalValue);
        }
        if (directory == null) {
            return;
        }
        try (Stream<Path> files = Files.walk(directory)) {
            for (Path path : files.sorted(Comparator.reverseOrder()).toList()) {
                Files.deleteIfExists(path);
            }
        } catch (IOException exception) {
            Client.LOG.warn("[perf] could not clean up temporary profile directory {}", directory, exception);
        }
    }

    @Override
    public List<String> covers() {
        return List.of(PerfCoverage.system("config"), PerfCoverage.common("setting"));
    }
}
