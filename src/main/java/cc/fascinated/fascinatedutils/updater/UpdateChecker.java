package cc.fascinated.fascinatedutils.updater;

import cc.fascinated.fascinatedutils.common.types.GitHubAsset;
import cc.fascinated.fascinatedutils.common.types.GitHubRelease;
import cc.fascinated.fascinatedutils.common.types.ReleaseVersionInfo;
import cc.fascinated.fascinatedutils.event.FascinatedEventBus;
import com.google.gson.*;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.client.Minecraft;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public final class UpdateChecker {
    private static final Logger LOG = LoggerFactory.getLogger(UpdateChecker.class);
    private static final String GITHUB_LATEST_API = "https://api.github.com/repos/RealFascinated/FascinatedUtils/releases/latest";
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(30);
    private final HttpClient client;
    private final Gson gson;
    private ScheduledExecutorService scheduler;

    public UpdateChecker() {
        this.client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).followRedirects(HttpClient.Redirect.NORMAL).build();
        this.gson = new GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).create();
    }

    public static void checkForUpdatesAsync() {
        new UpdateChecker().startPeriodicChecks();
    }

    public void startPeriodicChecks() {
        if (this.scheduler != null && !this.scheduler.isShutdown()) {
            return;
        }
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "FascinatedUtils-UpdateChecker");
            t.setDaemon(true);
            return t;
        });
        this.scheduler.scheduleAtFixedRate(() -> {
            try {
                checkForUpdates();
            } catch (Throwable t) {
                LOG.warn("Update check failed", t);
            }
        }, 0, 1, TimeUnit.HOURS);
    }

    public void checkForUpdates() throws Exception {
        GitHubRelease release = fetchLatestRelease();
        if (release == null || release.getTagName() == null || release.getTagName().isEmpty()) {
            return;
        }

        List<GitHubAsset> assets = release.getAssets();
        if (assets == null || assets.isEmpty()) {
            return;
        }

        ModContainer mod = FabricLoader.getInstance().getModContainer("fascinatedutils").orElse(null);
        if (mod == null) {
            return;
        }

        String currentVersion = mod.getMetadata().getVersion().getFriendlyString();
        String tagName = release.getTagName();
        if (currentVersion.equals(tagName)) {
            LOG.debug("Already on latest version: {}", currentVersion);
            return;
        }

        if (!isCompatibleWithRuntime(assets, tagName)) {
            return;
        }

        GitHubAsset jar = pickJarAsset(assets, mod).orElse(null);
        if (jar == null) {
            return;
        }

        Path staged = downloadToStaged(jar.getBrowserDownloadUrl(), tagName);
        if (staged != null) {
            FascinatedEventBus.INSTANCE.post(new UpdateRequiredEvent(tagName, jar.getBrowserDownloadUrl(), staged.toString()));
        }
    }

    private GitHubRelease fetchLatestRelease() throws Exception {
        HttpRequest req = HttpRequest.newBuilder().uri(URI.create(GITHUB_LATEST_API)).header("Accept", "application/vnd.github+json").header("User-Agent", "FascinatedUtils-Updater").timeout(REQUEST_TIMEOUT).GET().build();

        HttpResponse<String> res = client.send(req, HttpResponse.BodyHandlers.ofString());
        if (res.statusCode() != 200) {
            LOG.debug("GitHub API returned non-OK status: {}", res.statusCode());
            return null;
        }
        return gson.fromJson(res.body(), GitHubRelease.class);
    }

    private boolean isCompatibleWithRuntime(List<GitHubAsset> assets, String tagName) {
        String releaseInfoUrl = assets.stream().filter(a -> a != null && "release_info.json".equalsIgnoreCase(a.getName())).map(GitHubAsset::getBrowserDownloadUrl).findFirst().orElse(null);

        if (releaseInfoUrl == null) {
            return true;
        }

        try {
            HttpRequest req = HttpRequest.newBuilder().uri(URI.create(releaseInfoUrl)).timeout(REQUEST_TIMEOUT).GET().build();
            HttpResponse<String> res = client.send(req, HttpResponse.BodyHandlers.ofString());
            if (res.statusCode() != 200) {
                return true;
            }

            ReleaseVersionInfo info = gson.fromJson(res.body(), ReleaseVersionInfo.class);
            String releaseMc = info != null ? info.getMinecraftVersion() : null;
            String runtimeMc = getRuntimeMinecraftVersion();

            if (releaseMc != null && runtimeMc != null && !releaseMc.equals(runtimeMc)) {
                LOG.info("Skipping update {} — release targets MC {} but runtime is {}", tagName, releaseMc, runtimeMc);
                return false;
            }
        } catch (Exception e) {
            LOG.warn("Failed to fetch release_info.json, proceeding anyway", e);
        }
        return true;
    }

    private Path downloadToStaged(String downloadUrl, String tagName) throws Exception {
        Path modsDir = FabricLoader.getInstance().getGameDir().resolve("mods");
        Files.createDirectories(modsDir);

        Path temp = Files.createTempFile(modsDir, "FascinatedUtils-update-temp-", ".jar");
        try {
            HttpRequest req = HttpRequest.newBuilder().uri(URI.create(downloadUrl)).header("User-Agent", "FascinatedUtils-Updater").timeout(Duration.ofMinutes(2)).GET().build();

            HttpResponse<InputStream> res = client.send(req, HttpResponse.BodyHandlers.ofInputStream());
            if (res.statusCode() != 200) {
                LOG.warn("Failed to download release asset: HTTP {}", res.statusCode());
                return null;
            }

            try (InputStream is = res.body()) {
                Files.copy(is, temp, StandardCopyOption.REPLACE_EXISTING);
            }

            // Remove any previously staged updates
            try (var stream = Files.list(modsDir)) {
                stream.filter(p -> p.getFileName().toString().startsWith("FascinatedUtils-update-")).forEach(p -> {
                    try {
                        Files.deleteIfExists(p);
                    } catch (Exception ignored) {
                    }
                });
            } catch (IOException e) {
                LOG.debug("Could not clean old staged files", e);
            }

            Path staged = modsDir.resolve("FascinatedUtils-update-" + tagName + "-" + System.currentTimeMillis() + ".jar");
            Files.move(temp, staged, StandardCopyOption.REPLACE_EXISTING);
            LOG.info("Staged update {} -> {}", tagName, staged);
            return staged;
        } catch (Exception e) {
            try {
                Files.deleteIfExists(temp);
            } catch (Exception ignored) {
            }
            throw e;
        }
    }

    private String getRuntimeMinecraftVersion() {
        try {
            return Minecraft.getInstance().getLaunchedVersion();
        } catch (Throwable t) {
            LOG.debug("Minecraft client not available; falling back to fabric.mod.json", t);
        }
        try (InputStream is = UpdateChecker.class.getClassLoader().getResourceAsStream("fabric.mod.json")) {
            if (is == null) {
                return null;
            }
            JsonObject obj = JsonParser.parseString(new String(is.readAllBytes(), StandardCharsets.UTF_8)).getAsJsonObject();
            JsonObject depends = obj.has("depends") ? obj.getAsJsonObject("depends") : null;
            return depends != null && depends.has("minecraft") ? depends.get("minecraft").getAsString() : null;
        } catch (Exception e) {
            LOG.debug("Failed to read fabric.mod.json", e);
            return null;
        }
    }

    private Optional<GitHubAsset> pickJarAsset(List<GitHubAsset> assets, ModContainer mod) {
        if (assets == null || assets.isEmpty()) {
            return Optional.empty();
        }

        List<GitHubAsset> candidates = assets.stream().filter(a -> a != null && a.getName() != null).filter(a -> a.getName().toLowerCase(Locale.ROOT).endsWith(".jar")).filter(a -> {
            String name = a.getName().toLowerCase(Locale.ROOT);
            return !name.contains("source") && !name.contains("sources") && !name.contains("javadoc") && !name.contains("src") && !name.contains("-sources");
        }).toList();

        if (candidates.isEmpty()) {
            return Optional.empty();
        }

        try {
            String modId = mod.getMetadata().getId().toLowerCase(Locale.ROOT);
            return candidates.stream().filter(a -> a.getName().toLowerCase(Locale.ROOT).contains(modId)).findFirst().or(() -> candidates.stream().findFirst());
        } catch (Throwable t) {
            return candidates.stream().findFirst();
        }
    }
}