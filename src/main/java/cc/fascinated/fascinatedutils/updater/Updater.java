package cc.fascinated.fascinatedutils.updater;

import cc.fascinated.fascinatedutils.Constants;
import cc.fascinated.fascinatedutils.common.AlumiteEnvironment;
import cc.fascinated.fascinatedutils.common.SystemUtils;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.Comparator;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

@Slf4j
public class Updater {
    public static final Updater INSTANCE = new Updater();
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(30);
    private static final String STAGED_PREFIX = "FascinatedUtils-update-";
    private static final String STAGED_SUFFIX = ".staged";
    private static final String DOWNLOAD_PREFIX = "FascinatedUtils-download-";
    private static final String DOWNLOAD_SUFFIX = ".part";
    private static final Set<String> TRUSTED_DOWNLOAD_HOSTS = Set.of("github.com");

    @Getter private volatile boolean hasUpdate = false;
    private final HttpClient client;

    private Updater() {
        this.client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    public void start() {
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "FascinatedUtils-Updater");
            thread.setDaemon(true);
            return thread;
        });
        scheduler.execute(this::cleanLeftoverFiles);
        scheduler.scheduleAtFixedRate(this::check, 0, 30, TimeUnit.MINUTES);

        Runtime.getRuntime().addShutdownHook(new Thread(this::applyStaged, "FascinatedUtils-Update-Apply"));
    }

    private Path modsDirectory() {
        return FabricLoader.getInstance().getGameDir().resolve("mods");
    }

    /**
     * Deletes partial downloads and legacy staged files that still end in {@code .jar} (which Fabric would load).
     */
    private void cleanLeftoverFiles() {
        Path modsDir = modsDirectory();
        if (!Files.isDirectory(modsDir)) {
            return;
        }
        try (Stream<Path> stream = Files.list(modsDir)) {
            stream.filter(path -> {
                String name = path.getFileName().toString();
                boolean partial = name.startsWith(DOWNLOAD_PREFIX);
                boolean legacyStaged = name.startsWith(STAGED_PREFIX) && name.endsWith(".jar");
                return partial || legacyStaged;
            }).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (Exception exception) {
                    log.info("Could not delete leftover update file {}", path, exception);
                }
            });
        } catch (IOException exception) {
            log.info("Could not clean leftover update files", exception);
        }
    }

    private boolean isTrustedDownloadUrl(String downloadUrl) {
        try {
            URI downloadUri = URI.create(downloadUrl);
            URI apiUri = URI.create(AlumiteEnvironment.API_BASE_URL);
            String host = downloadUri.getHost();
            if (!"https".equalsIgnoreCase(downloadUri.getScheme()) || host == null) {
                return false;
            }
            return host.equalsIgnoreCase(apiUri.getHost()) || TRUSTED_DOWNLOAD_HOSTS.contains(host.toLowerCase());
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    private Path stagedPathFor(Path modsDir, String version) {
        String safeVersion = version.replaceAll("[^A-Za-z0-9._-]", "_");
        return modsDir.resolve(STAGED_PREFIX + safeVersion + STAGED_SUFFIX);
    }

    private void check() {
        try {
            String currentVersion = FabricLoader.getInstance().getModContainer("alumite")
                    .map(mod -> mod.getMetadata().getVersion().getFriendlyString())
                    .orElse(null);

            String url = AlumiteEnvironment.API_BASE_URL + "/updater/check"
                    + (currentVersion != null ? "?version=" + URLEncoder.encode(currentVersion, StandardCharsets.UTF_8) : "");

            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("User-Agent", AlumiteEnvironment.USER_AGENT)
                    .timeout(REQUEST_TIMEOUT)
                    .GET()
                    .build();

            HttpResponse<String> res = client.send(req, HttpResponse.BodyHandlers.ofString());
            if (res.statusCode() != 200) {
                log.info("Updater API returned non-OK status: {}", res.statusCode());
                return;
            }

            UpdateCheckResult result = Constants.GSON.fromJson(res.body(), UpdateCheckResult.class);
            if (result == null || result.isUpToDate()) {
                log.info("Already on latest version");
                return;
            }

            String downloadUrl = result.getDownloadUrl();
            if (downloadUrl == null || downloadUrl.isBlank()) {
                log.info("No download URL provided in update check result");
                return;
            }

            String latestVersion = result.getLatestVersion();
            if (latestVersion == null || latestVersion.isBlank()) {
                log.info("No latest version provided in update check result");
                return;
            }

            if (!isTrustedDownloadUrl(downloadUrl)) {
                log.warn("Refusing to download update from untrusted URL: {}", downloadUrl);
                return;
            }

            hasUpdate = true;
            Path modsDir = modsDirectory();
            if (Files.exists(stagedPathFor(modsDir, latestVersion))) {
                log.info("Update {} is already staged", latestVersion);
                return;
            }
            downloadToStaged(downloadUrl, latestVersion);
        } catch (Throwable throwable) {
            log.warn("Update check failed", throwable);
        }
    }

    private void downloadToStaged(String downloadUrl, String tagName) throws Exception {
        Path modsDir = modsDirectory();
        Files.createDirectories(modsDir);

        Path temp = Files.createTempFile(modsDir, DOWNLOAD_PREFIX, DOWNLOAD_SUFFIX);
        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(downloadUrl))
                    .header("User-Agent", AlumiteEnvironment.USER_AGENT)
                    .timeout(Duration.ofMinutes(2))
                    .GET()
                    .build();

            HttpResponse<InputStream> res = client.send(req, HttpResponse.BodyHandlers.ofInputStream());
            try (InputStream inputStream = res.body()) {
                if (res.statusCode() != 200) {
                    log.warn("Failed to download release asset: HTTP {}", res.statusCode());
                    Files.deleteIfExists(temp);
                    return;
                }
                Files.copy(inputStream, temp, StandardCopyOption.REPLACE_EXISTING);
            }

            try (Stream<Path> stream = Files.list(modsDir)) {
                stream.filter(path -> {
                    String name = path.getFileName().toString();
                    return name.startsWith(STAGED_PREFIX);
                }).forEach(path -> {
                    try {
                        Files.deleteIfExists(path);
                    } catch (Exception ignored) {
                    }
                });
            } catch (IOException exception) {
                log.info("Could not clean old staged files", exception);
            }

            Path staged = stagedPathFor(modsDir, tagName);
            Files.move(temp, staged, StandardCopyOption.REPLACE_EXISTING);
            log.info("Staged update {} -> {}", tagName, staged);
        } catch (Exception exception) {
            try {
                Files.deleteIfExists(temp);
            } catch (Exception ignored) {
            }
            throw exception;
        }
    }

    private void applyStaged() {
        try {
            Path modsDir = modsDirectory();
            try (Stream<Path> stream = Files.list(modsDir)) {
                stream.filter(path -> {
                            String name = path.getFileName().toString();
                            return name.startsWith(STAGED_PREFIX) && name.endsWith(STAGED_SUFFIX);
                        })
                        .max(Comparator.comparingLong(path -> path.toFile().lastModified()))
                        .ifPresent(this::applyStagedJar);
            }
        } catch (Throwable throwable) {
            log.warn("Unexpected error applying staged update", throwable);
        }
    }

    private void applyStagedJar(Path staged) {
        if (!Files.exists(staged)) {
            log.warn("Staged file does not exist: {}", staged);
            return;
        }

        ModContainer mod = FabricLoader.getInstance().getModContainer("alumite").orElse(null);
        if (mod == null) {
            log.warn("Mod container not found; cannot apply staged update");
            return;
        }

        Path current = mod.getOrigin().getPaths().stream().findFirst().orElse(null);
        if (current == null) {
            log.warn("Current mod path unknown; cannot apply staged update");
            return;
        }

        if (SystemUtils.isWindows()) {
            runWindowsUpdater(staged, current);
            return;
        }

        Path backup = null;
        if (Files.isRegularFile(current)) {
            Path backupCandidate = current.resolveSibling(current.getFileName() + ".old");
            try {
                Files.move(current, backupCandidate, StandardCopyOption.REPLACE_EXISTING);
                backup = backupCandidate;
                log.info("Backed up current mod to {}", backup);
            } catch (Exception exception) {
                log.warn("Failed to back up current mod: {}", current, exception);
            }
        }

        try {
            Files.move(staged, current, StandardCopyOption.REPLACE_EXISTING);
            log.info("Applied staged update: {} -> {}", staged, current);
        } catch (Exception exception) {
            log.error("Failed to apply staged update", exception);
            if (backup != null) {
                try {
                    Files.move(backup, current, StandardCopyOption.REPLACE_EXISTING);
                    log.info("Restored previous mod from {}", backup);
                } catch (Exception restoreException) {
                    log.error("Failed to restore previous mod from {}", backup, restoreException);
                }
            }
        }
    }

    private void runWindowsUpdater(Path staged, Path current) {
        String inner = String.format("for /L %%i in (1,1,600) do ( move /Y \"%s\" \"%s\" >nul 2>&1 && exit /b 0 || timeout /t 2 /nobreak >nul )", staged, current);
        try {
            new ProcessBuilder("cmd", "/c", "start \"\" /min cmd /c \"" + inner + "\"").start();
            log.info("Spawned detached update helper: {} -> {}", staged, current);
        } catch (Exception exception) {
            log.error("Failed to spawn Windows update helper", exception);
        }
    }
}
