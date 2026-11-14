package cc.fascinated.fascinatedutils.debug.perf.tests.config;

import cc.fascinated.fascinatedutils.client.Client;
import cc.fascinated.fascinatedutils.debug.perf.core.PerfTest;
import cc.fascinated.fascinatedutils.debug.perf.coverage.PerfCoverage;
import cc.fascinated.fascinatedutils.systems.config.ModConfig;
import cc.fascinated.fascinatedutils.systems.config.impl.profiles.Profile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

/**
 * The real profile save path (capture every module's settings with its version field, serialise, write a temporary
 * file and move it into place) aimed at a temporary directory, so the user's profile is never touched.
 */
public class ConfigWriteProfilePerfTest extends PerfTest {
    private static final UUID PERF_PROFILE_ID = UUID.fromString("00000000-0000-0000-0000-00000000fe4f");
    private static final int PROFILE_VERSION = 1;

    private Path directory;

    public ConfigWriteProfilePerfTest() {
        super("config.save_profile", "Capture all module settings and write a profile atomically to a temp dir", 3, 30);
        this.warmupMillis = 100;
    }

    @Override
    public void setup() {
        try {
            directory = Files.createTempDirectory("alumite-perf-profile");
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }

    @Override
    public void runOnce() {
        Profile profile = new Profile(PERF_PROFILE_ID, "perf", PROFILE_VERSION, ModConfig.profiles().captureModuleSettings());
        ModConfig.profiles().writeProfile(profile, directory);
    }

    @Override
    public void teardown() {
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
