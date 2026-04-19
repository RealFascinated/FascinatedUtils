package cc.fascinated.fascinatedutils.updater;

import cc.fascinated.fascinatedutils.common.types.GitHubAsset;
import cc.fascinated.fascinatedutils.common.types.GitHubRelease;
import cc.fascinated.fascinatedutils.common.types.ReleaseVersionInfo;
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
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

public class UpdateChecker {
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
        this.scheduler = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "FascinatedUtils-UpdateChecker");
            thread.setDaemon(true);
            return thread;
        });
        this.scheduler.scheduleAtFixedRate(() -> {
            try {
                checkForUpdates();
            } catch (Throwable throwable) {
                LOG.warn("Update check failed", throwable);
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

        Optional<ReleaseVersionInfo> releaseInfo = tryFetchReleaseVersionInfo(assets);
        if (releaseInfo.isPresent() && !isMinecraftVersionCompatible(releaseInfo.get(), tagName)) {
            return;
        }

        Optional<String> normalizedReleaseHash = releaseInfo.flatMap(info -> normalizedArtifactSha256(info.getArtifactSha256()));

        if (normalizedReleaseHash.isPresent()) {
            Optional<Path> modJar = primaryModJarPath(mod);
            if (modJar.isPresent()) {
                String installedHash = sha256Hex(modJar.get());
                if (normalizedReleaseHash.get().equals(installedHash)) {
                    LOG.debug("Installed mod jar matches release artifact_sha256; skipping update.");
                    return;
                }
            }
            Path modsDir = FabricLoader.getInstance().getGameDir().resolve("mods");
            Optional<Path> existingStaged = findStagedUpdateWithSha(modsDir, normalizedReleaseHash.get());
            if (existingStaged.isPresent()) {
                LOG.debug("Staged update jar already matches release artifact_sha256; skipping re-download.");
                return;
            }
        } else {
            if (currentVersion.equals(tagName)) {
                LOG.debug("Already on latest version: {}", currentVersion);
                return;
            }
        }

        GitHubAsset jar = pickJarAsset(assets, mod).orElse(null);
        if (jar == null) {
            return;
        }

        downloadToStaged(jar.getBrowserDownloadUrl(), tagName);
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

    private Optional<ReleaseVersionInfo> tryFetchReleaseVersionInfo(List<GitHubAsset> assets) {
        String releaseInfoUrl = assets.stream().filter(asset -> asset != null && "release_info.json".equalsIgnoreCase(asset.getName())).map(GitHubAsset::getBrowserDownloadUrl).findFirst().orElse(null);

        if (releaseInfoUrl == null) {
            return Optional.empty();
        }

        try {
            HttpRequest req = HttpRequest.newBuilder().uri(URI.create(releaseInfoUrl)).timeout(REQUEST_TIMEOUT).GET().build();
            HttpResponse<String> res = client.send(req, HttpResponse.BodyHandlers.ofString());
            if (res.statusCode() != 200) {
                return Optional.empty();
            }

            ReleaseVersionInfo parsed = gson.fromJson(res.body(), ReleaseVersionInfo.class);
            return Optional.ofNullable(parsed);
        } catch (Exception exception) {
            LOG.warn("Failed to fetch release_info.json, proceeding without artifact hash", exception);
            return Optional.empty();
        }
    }

    private boolean isMinecraftVersionCompatible(ReleaseVersionInfo info, String tagName) {
        String releaseMc = info != null ? info.getMinecraftVersion() : null;
        String runtimeMc = getRuntimeMinecraftVersion();

        if (releaseMc != null && runtimeMc != null && !releaseMc.equals(runtimeMc)) {
            LOG.info("Skipping update {} — release targets MC {} but runtime is {}", tagName, releaseMc, runtimeMc);
            return false;
        }
        return true;
    }

    private Optional<String> normalizedArtifactSha256(String raw) {
        if (raw == null) {
            return Optional.empty();
        }
        String trimmed = raw.trim().toLowerCase(Locale.ROOT);
        if (trimmed.length() != 64) {
            return Optional.empty();
        }
        for (int index = 0; index < trimmed.length(); index++) {
            char character = trimmed.charAt(index);
            if ((character < '0' || character > '9') && (character < 'a' || character > 'f')) {
                return Optional.empty();
            }
        }
        return Optional.of(trimmed);
    }

    private Optional<Path> primaryModJarPath(ModContainer mod) {
        try {
            for (Path root : mod.getRootPaths()) {
                if (Files.isRegularFile(root) && root.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".jar")) {
                    return Optional.of(root);
                }
            }
        } catch (Throwable ignored) {
        }
        return Optional.empty();
    }

    private Optional<Path> findStagedUpdateWithSha(Path modsDir, String releaseSha256Lower) {
        if (!Files.isDirectory(modsDir)) {
            return Optional.empty();
        }
        try (Stream<Path> entries = Files.list(modsDir)) {
            List<Path> candidates = entries.filter(path -> Files.isRegularFile(path)).filter(path -> path.getFileName().toString().startsWith("FascinatedUtils-update-")).filter(path -> path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".jar")).toList();
            for (Path candidate : candidates) {
                try {
                    if (releaseSha256Lower.equals(sha256Hex(candidate))) {
                        return Optional.of(candidate);
                    }
                } catch (IOException exception) {
                    LOG.debug("Could not hash staged candidate {}", candidate, exception);
                }
            }
        } catch (IOException exception) {
            LOG.debug("Could not scan mods directory for staged updates", exception);
        }
        return Optional.empty();
    }

    private static String sha256Hex(Path path) throws IOException {
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 not available", exception);
        }
        try (InputStream inputStream = Files.newInputStream(path)) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = inputStream.read(buffer)) >= 0) {
                digest.update(buffer, 0, read);
            }
        }
        return hexLower(digest.digest());
    }

    private static String hexLower(byte[] digestBytes) {
        StringBuilder builder = new StringBuilder(digestBytes.length * 2);
        for (byte singleByte : digestBytes) {
            builder.append(String.format(Locale.ROOT, "%02x", singleByte));
        }
        return builder.toString();
    }

    private Path downloadToStaged(String downloadUrl, String tagName) throws Exception {
        Path modsDir = FabricLoader.getInstance().getGameDir().resolve("mods");
        Files.createDirectories(modsDir);

        Path temp = Files.createTempFile(modsDir, "FascinatedUtils-download-", ".jar");
        try {
            HttpRequest req = HttpRequest.newBuilder().uri(URI.create(downloadUrl)).header("User-Agent", "FascinatedUtils-Updater").timeout(Duration.ofMinutes(2)).GET().build();

            HttpResponse<InputStream> res = client.send(req, HttpResponse.BodyHandlers.ofInputStream());
            if (res.statusCode() != 200) {
                LOG.warn("Failed to download release asset: HTTP {}", res.statusCode());
                return null;
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
            return staged;
        } catch (Exception exception) {
            try {
                Files.deleteIfExists(temp);
            } catch (Exception ignored) {
            }
            throw exception;
        }
    }

    private String getRuntimeMinecraftVersion() {
        try {
            return Minecraft.getInstance().getLaunchedVersion();
        } catch (Throwable throwable) {
            LOG.debug("Minecraft client not available; falling back to fabric.mod.json", throwable);
        }
        try (InputStream inputStream = UpdateChecker.class.getClassLoader().getResourceAsStream("fabric.mod.json")) {
            if (inputStream == null) {
                return null;
            }
            JsonObject object = JsonParser.parseString(new String(inputStream.readAllBytes(), StandardCharsets.UTF_8)).getAsJsonObject();
            JsonObject depends = object.has("depends") ? object.getAsJsonObject("depends") : null;
            return depends != null && depends.has("minecraft") ? depends.get("minecraft").getAsString() : null;
        } catch (Exception exception) {
            LOG.debug("Failed to read fabric.mod.json", exception);
            return null;
        }
    }

    private Optional<GitHubAsset> pickJarAsset(List<GitHubAsset> assets, ModContainer mod) {
        if (assets == null || assets.isEmpty()) {
            return Optional.empty();
        }

        List<GitHubAsset> candidates = assets.stream().filter(asset -> asset != null && asset.getName() != null).filter(asset -> {
            String name = asset.getName().toLowerCase(Locale.ROOT);
            return name.endsWith(".jar") && !name.contains("source") && !name.contains("sources") && !name.contains("javadoc") && !name.contains("src") && !name.contains("-sources");
        }).toList();

        if (candidates.isEmpty()) {
            return Optional.empty();
        }

        try {
            String modId = mod.getMetadata().getId().toLowerCase(Locale.ROOT);
            return candidates.stream().filter(asset -> asset.getName().toLowerCase(Locale.ROOT).contains(modId)).findFirst().or(() -> candidates.stream().findFirst());
        } catch (Throwable throwable) {
            return candidates.stream().findFirst();
        }
    }
}
