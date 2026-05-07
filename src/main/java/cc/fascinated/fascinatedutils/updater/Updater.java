package cc.fascinated.fascinatedutils.updater;

import cc.fascinated.fascinatedutils.Constants;
import cc.fascinated.fascinatedutils.common.AlumiteEnvironment;
import cc.fascinated.fascinatedutils.common.SystemUtils;
import lombok.Getter;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.Comparator;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

public class Updater {
    public static final Updater INSTANCE = new Updater();
    private static final Logger LOG = LoggerFactory.getLogger(Updater.class);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(30);

    @Getter private volatile boolean hasUpdate = false;
    private final HttpClient client;

    private Updater() {
        this.client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    public void start() {
        try (ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "FascinatedUtils-Updater");
            thread.setDaemon(true);
            return thread;
        })) {
            scheduler.scheduleAtFixedRate(this::check, 0, 30, TimeUnit.MINUTES);
        }

        Runtime.getRuntime().addShutdownHook(new Thread(this::applyStaged, "FascinatedUtils-Update-Apply"));
    }

    private void check() {
        try {
            String currentVersion = FabricLoader.getInstance().getModContainer("alumite")
                    .map(mod -> mod.getMetadata().getVersion().getFriendlyString())
                    .orElse(null);

            String url = AlumiteEnvironment.API_BASE_URL + "/updater/check"
                    + (currentVersion != null ? "?version=" + currentVersion : "");

            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("User-Agent", AlumiteEnvironment.USER_AGENT)
                    .timeout(REQUEST_TIMEOUT)
                    .GET()
                    .build();

            HttpResponse<String> res = client.send(req, HttpResponse.BodyHandlers.ofString());
            if (res.statusCode() != 200) {
                LOG.debug("Updater API returned non-OK status: {}", res.statusCode());
                return;
            }

            UpdateCheckResult result = Constants.GSON.fromJson(res.body(), UpdateCheckResult.class);
            if (result == null || result.isUpToDate()) {
                LOG.debug("Already on latest version");
                return;
            }

            String downloadUrl = result.getDownloadUrl();
            if (downloadUrl == null || downloadUrl.isBlank()) {
                LOG.debug("No download URL provided in update check result");
                return;
            }

            hasUpdate = true;
            downloadToStaged(downloadUrl, result.getLatestVersion());
        } catch (Throwable throwable) {
            LOG.warn("Update check failed", throwable);
        }
    }

    private void downloadToStaged(String downloadUrl, String tagName) throws Exception {
        Path modsDir = FabricLoader.getInstance().getGameDir().resolve("mods");
        Files.createDirectories(modsDir);

        Path temp = Files.createTempFile(modsDir, "FascinatedUtils-download-", ".jar");
        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(downloadUrl))
                    .header("User-Agent", AlumiteEnvironment.USER_AGENT)
                    .timeout(Duration.ofMinutes(2))
                    .GET()
                    .build();

            HttpResponse<InputStream> res = client.send(req, HttpResponse.BodyHandlers.ofInputStream());
            if (res.statusCode() != 200) {
                LOG.warn("Failed to download release asset: HTTP {}", res.statusCode());
                return;
            }

            try (InputStream inputStream = res.body()) {
                Files.copy(inputStream, temp, StandardCopyOption.REPLACE_EXISTING);
            }

            try (Stream<Path> stream = Files.list(modsDir)) {
                stream.filter(path -> path.getFileName().toString().startsWith("FascinatedUtils-update-")).forEach(path -> {
                    try {
                        Files.deleteIfExists(path);
                    } catch (Exception ignored) {
                    }
                });
            } catch (IOException exception) {
                LOG.debug("Could not clean old staged files", exception);
            }

            Path staged = modsDir.resolve("FascinatedUtils-update-" + tagName + "-" + System.currentTimeMillis() + ".jar");
            Files.move(temp, staged, StandardCopyOption.REPLACE_EXISTING);
            LOG.info("Staged update {} -> {}", tagName, staged);
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
            Path modsDir = FabricLoader.getInstance().getGameDir().resolve("mods");
            try (Stream<Path> stream = Files.list(modsDir)) {
                stream.filter(path -> path.getFileName().toString().startsWith("FascinatedUtils-update-"))
                        .max(Comparator.comparingLong(path -> path.toFile().lastModified()))
                        .ifPresent(this::applyStagedJar);
            }
        } catch (Throwable throwable) {
            LOG.warn("Unexpected error applying staged update", throwable);
        }
    }

    private void applyStagedJar(Path staged) {
        if (!Files.exists(staged)) {
            LOG.warn("Staged file does not exist: {}", staged);
            return;
        }

        ModContainer mod = FabricLoader.getInstance().getModContainer("alumite").orElse(null);
        if (mod == null) {
            LOG.warn("Mod container not found; cannot apply staged update");
            return;
        }

        Path current = mod.getOrigin().getPaths().stream().findFirst().orElse(null);
        if (current == null) {
            LOG.warn("Current mod path unknown; cannot apply staged update");
            return;
        }

        if (SystemUtils.isWindows()) {
            runWindowsUpdater(staged, current);
            return;
        }

        if (Files.isRegularFile(current)) {
            Path backup = current.resolveSibling(current.getFileName() + ".old");
            try {
                Files.move(current, backup, StandardCopyOption.REPLACE_EXISTING);
                LOG.info("Backed up current mod to {}", backup);
            } catch (Exception exception) {
                LOG.warn("Failed to back up current mod: {}", current, exception);
            }
        }

        try {
            Files.move(staged, current, StandardCopyOption.REPLACE_EXISTING);
            LOG.info("Applied staged update: {} -> {}", staged, current);
        } catch (Exception exception) {
            LOG.error("Failed to apply staged update", exception);
        }
    }

    private void runWindowsUpdater(Path staged, Path current) {
        String inner = String.format("for /L %%i in (1,1,600) do ( move /Y \"%s\" \"%s\" >nul 2>&1 && exit /b 0 || timeout /t 2 /nobreak >nul )", staged, current);
        try {
            new ProcessBuilder("cmd", "/c", "start \"\" /min cmd /c \"" + inner + "\"").start();
            LOG.info("Spawned detached update helper: {} -> {}", staged, current);
        } catch (Exception exception) {
            LOG.error("Failed to spawn Windows update helper", exception);
        }
    }
}
