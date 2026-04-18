package cc.fascinated.fascinatedutils.updater;

import cc.fascinated.fascinatedutils.client.Client;
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
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public final class UpdateChecker {
    private static final String GITHUB_LATEST_API = "https://api.github.com/repos/RealFascinated/FascinatedUtils/releases/latest";
    private static final Duration DEFAULT_REQUEST_TIMEOUT = Duration.ofSeconds(30);

    private final HttpClient client;
    private final Gson gson;

    public UpdateChecker() {
        this.client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).followRedirects(HttpClient.Redirect.NORMAL).build();
        this.gson = new GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).create();
    }

    public static void checkForUpdatesAsync() {
        CompletableFuture.runAsync(() -> {
            try {
                new UpdateChecker().checkForUpdates();
            } catch (Throwable t) {
                Client.LOG.warn("Update check failed", t);
            }
        });
    }

    public void checkForUpdates() {
        try {
            HttpRequest req = HttpRequest.newBuilder().uri(URI.create(GITHUB_LATEST_API)).header("Accept", "application/vnd.github+json").header("User-Agent", "FascinatedUtils-Updater").timeout(DEFAULT_REQUEST_TIMEOUT).GET().build();

            HttpResponse<String> res = client.send(req, HttpResponse.BodyHandlers.ofString());
            if (res.statusCode() != 200) {
                Client.LOG.debug("GitHub API returned non-OK status: {}", res.statusCode());
                return;
            }

            GitHubRelease root = gson.fromJson(res.body(), GitHubRelease.class);
            if (root == null) {
                return;
            }
            String tagName = root.getTagName();
            if (tagName == null || tagName.isEmpty()) {
                return;
            }

            List<GitHubAsset> assets = root.getAssets();
            if (assets == null || assets.isEmpty()) {
                return;
            }

            Optional<ModContainer> mod = FabricLoader.getInstance().getModContainer("fascinatedutils");
            if (mod.isEmpty()) {
                return;
            }
            ModContainer modContainer = mod.get();
            String currentVersion = modContainer.getMetadata().getVersion().getFriendlyString();
            if (currentVersion.equals(tagName)) {
                Client.LOG.debug("Already on latest version: {}", currentVersion);
                return;
            }

            String releaseInfoUrl = assets.stream().filter(a -> a != null && a.getName() != null && "release_info.json".equalsIgnoreCase(a.getName())).map(GitHubAsset::getBrowserDownloadUrl).findFirst().orElse(null);

            if (releaseInfoUrl != null) {
                try {
                    HttpRequest infoReq = HttpRequest.newBuilder().uri(URI.create(releaseInfoUrl)).timeout(DEFAULT_REQUEST_TIMEOUT).GET().build();
                    HttpResponse<String> infoRes = client.send(infoReq, HttpResponse.BodyHandlers.ofString());
                    if (infoRes.statusCode() == 200) {
                        ReleaseVersionInfo info = gson.fromJson(infoRes.body(), ReleaseVersionInfo.class);
                        String releaseMc = info != null ? info.getMinecraftVersion() : null;
                        String runtimeMc = getRuntimeMinecraftVersion();
                        if (releaseMc != null && runtimeMc != null && !releaseMc.equals(runtimeMc)) {
                            Client.LOG.info("Skipping update {} — release targets MC {} but runtime is {}", tagName, releaseMc, runtimeMc);
                            return;
                        }
                    }
                } catch (Exception e) {
                    Client.LOG.warn("Failed to fetch release_info.json, continuing with caution", e);
                }
            }

            GitHubAsset chosen = pickJarAsset(assets, modContainer).orElse(null);
            if (chosen == null) {
                return;
            }
            String downloadUrl = chosen.getBrowserDownloadUrl();

            Path modsDir = FabricLoader.getInstance().getGameDir().resolve("mods");
            try {
                Files.createDirectories(modsDir);
            } catch (IOException ioe) {
                Client.LOG.warn("Unable to create mods directory: {}", modsDir, ioe);
                return;
            }

            String stagedFileName = "FascinatedUtils-update-" + tagName + "-" + System.currentTimeMillis() + ".jar";
            Path staged = modsDir.resolve(stagedFileName);

            HttpRequest dlReq = HttpRequest.newBuilder().uri(URI.create(downloadUrl)).header("User-Agent", "FascinatedUtils-Updater").timeout(Duration.ofMinutes(2)).GET().build();

            HttpResponse<InputStream> dlRes = client.send(dlReq, HttpResponse.BodyHandlers.ofInputStream());
            if (dlRes.statusCode() == 200) {
                try (InputStream is = dlRes.body()) {
                    Files.copy(is, staged, StandardCopyOption.REPLACE_EXISTING);
                    Client.LOG.info("Staged update {} -> {}", tagName, staged);
                    FascinatedEventBus.INSTANCE.post(new UpdateRequiredEvent(tagName, downloadUrl, staged.toString()));
                }
            }
            else {
                Client.LOG.warn("Failed to download release asset: HTTP {}", dlRes.statusCode());
            }
        } catch (Exception e) {
            Client.LOG.warn("Unhandled error during update check", e);
        }
    }

    private String getRuntimeMinecraftVersion() {
        try {
            return Minecraft.getInstance().getLaunchedVersion();
        } catch (Throwable t) {
            Client.LOG.debug("Minecraft client not available; falling back to fabric.mod.json", t);
            try (InputStream ris = UpdateChecker.class.getClassLoader().getResourceAsStream("fabric.mod.json")) {
                if (ris != null) {
                    String fm = new String(ris.readAllBytes(), StandardCharsets.UTF_8);
                    JsonObject fmObj = JsonParser.parseString(fm).getAsJsonObject();
                    if (fmObj.has("depends")) {
                        JsonObject depends = fmObj.getAsJsonObject("depends");
                        if (depends.has("minecraft")) {
                            return depends.get("minecraft").getAsString();
                        }
                    }
                }
            } catch (Exception ex) {
                Client.LOG.debug("Failed to read fabric.mod.json", ex);
            }
        }
        return null;
    }

    private Optional<GitHubAsset> pickJarAsset(List<GitHubAsset> assets, ModContainer mod) {
        if (assets == null || assets.isEmpty()) {
            return Optional.empty();
        }

        List<GitHubAsset> jars = assets.stream().filter(a -> a != null && a.getName() != null && a.getName().toLowerCase(Locale.ROOT).endsWith(".jar")).toList();
        if (jars.isEmpty()) {
            return Optional.empty();
        }

        List<GitHubAsset> nonSource = jars.stream().filter(a -> {
            String nameLower = a.getName().toLowerCase(Locale.ROOT);
            return !(nameLower.contains("source") || nameLower.contains("sources") || nameLower.contains("javadoc") || nameLower.contains("-src") || nameLower.contains("_src") || nameLower.contains("-sources") || nameLower.contains("src"));
        }).toList();

        List<GitHubAsset> candidates = nonSource.isEmpty() ? jars : nonSource;

        String modId = null;
        try {
            modId = mod.getMetadata().getId();
        } catch (Throwable t) {
            // ignore
        }

        if (modId != null) {
            String mid = modId.toLowerCase(Locale.ROOT);
            Optional<GitHubAsset> byName = candidates.stream().filter(a -> a.getName().toLowerCase(Locale.ROOT).contains(mid)).findFirst();
            if (byName.isPresent()) {
                return byName;
            }
        }

        return candidates.stream().findFirst();
    }
}
