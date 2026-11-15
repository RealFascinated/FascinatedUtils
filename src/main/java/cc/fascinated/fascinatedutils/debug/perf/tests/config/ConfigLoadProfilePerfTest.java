package cc.fascinated.fascinatedutils.debug.perf.tests.config;

import cc.fascinated.fascinatedutils.Constants;
import cc.fascinated.fascinatedutils.debug.perf.core.PerfTest;
import cc.fascinated.fascinatedutils.debug.perf.coverage.PerfCoverage;
import cc.fascinated.fascinatedutils.systems.config.ModConfig;
import cc.fascinated.fascinatedutils.systems.config.impl.profiles.Profile;
import com.google.gson.JsonElement;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

/**
 * The profile load path (read the file, parse the JSON, deserialise it into a {@link Profile}) for a profile written by
 * the real save path into a temporary directory. The user's profile is never read or written.
 */
public class ConfigLoadProfilePerfTest extends PerfTest {
    private static final UUID PERF_PROFILE_ID = UUID.fromString("00000000-0000-0000-0000-00000000fe50");
    private static final int PROFILE_VERSION = 1;

    private Path directory;
    private Path profilePath;
    private int sink;

    public ConfigLoadProfilePerfTest() {
        super("config.load_profile", "Read a profile file from a temp dir and deserialise it into a Profile", 3, 30);
        this.warmupMillis = 100;
    }

    @Override
    public void setup() {
        try {
            directory = Files.createTempDirectory("alumite-perf-profile-load");
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
        Profile profile = new Profile(PERF_PROFILE_ID, "perf", PROFILE_VERSION, ModConfig.profiles().captureModuleSettings());
        ModConfig.profiles().writeProfile(profile, directory);
        profilePath = profile.resolveFilePath(directory);
    }

    @Override
    public void runOnce() {
        try {
            String json = Files.readString(profilePath, StandardCharsets.UTF_8);
            JsonElement element = Constants.GSON.fromJson(json, JsonElement.class);
            Profile profile = Profile.defaults().deserialize(element, Constants.GSON);
            sink += profile.getSettings().size();
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
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
            throw new UncheckedIOException(exception);
        }
    }

    @Override
    public List<String> covers() {
        return List.of(PerfCoverage.system("config"));
    }
}
