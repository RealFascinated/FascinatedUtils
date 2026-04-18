package cc.fascinated.fascinatedutils.updater;

import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import cc.fascinated.fascinatedutils.common.types.GitHubAsset;
import cc.fascinated.fascinatedutils.common.types.GitHubRelease;
import cc.fascinated.fascinatedutils.common.types.ReleaseVersionInfo;
import cc.fascinated.fascinatedutils.event.FascinatedEventBus;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;

public class UpdateChecker {
    private static final String GITHUB_LATEST_API = "https://api.github.com/repos/RealFascinated/FascinatedUtils/releases/latest";

    public static void checkForUpdatesAsync() {
        CompletableFuture.runAsync(() -> {
            try {
                checkForUpdates();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private static void checkForUpdates() throws Exception {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(GITHUB_LATEST_API))
                .header("Accept", "application/vnd.github+json")
                .build();

        HttpResponse<String> res = client.send(req, HttpResponse.BodyHandlers.ofString());
        if (res.statusCode() != 200) {
            return;
        }

        Gson gson = new GsonBuilder()
            .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
            .create();

        GitHubRelease root = gson.fromJson(res.body(), GitHubRelease.class);
        if (root == null) return;
        String tagName = root.getTagName();
        if (tagName == null) return;

        List<GitHubAsset> assets = root.getAssets();
        if (assets == null || assets.isEmpty()) return;

        Optional<ModContainer> mod = FabricLoader.getInstance().getModContainer("fascinatedutils");
        if (mod.isEmpty()) return;
        String currentVersion = mod.get().getMetadata().getVersion().getFriendlyString();
        if (currentVersion.equals(tagName)) {
            return;
        }

        // If the release includes a release_info.json, fetch it and ensure the minecraft
        // version matches the running game's version before auto-staging the update.
        String releaseInfoUrl = null;
        for (GitHubAsset a : assets) {
            if (a != null && "release_info.json".equals(a.getName())) {
                releaseInfoUrl = a.getBrowserDownloadUrl();
                break;
            }
        }

        if (releaseInfoUrl != null) {
            HttpRequest infoReq = HttpRequest.newBuilder().uri(URI.create(releaseInfoUrl)).build();
            HttpResponse<String> infoRes = client.send(infoReq, HttpResponse.BodyHandlers.ofString());
            if (infoRes.statusCode() == 200) {
                ReleaseVersionInfo info = gson.fromJson(infoRes.body(), ReleaseVersionInfo.class);
                String releaseMc = info != null ? info.getMinecraftVersion() : null;

                String runtimeMc = null;
                try {
                    try {
                        runtimeMc = net.minecraft.client.Minecraft.getInstance().getLaunchedVersion();
                    } catch (Throwable t) {
                        // ignore - not available in this environment
                    }
                } catch (Throwable t) {
                    // ignore
                }

                if ((runtimeMc == null || runtimeMc.isEmpty())) {
                    try (InputStream ris = UpdateChecker.class.getClassLoader().getResourceAsStream("fabric.mod.json")) {
                        if (ris != null) {
                            String fm = new String(ris.readAllBytes(), StandardCharsets.UTF_8);
                            JsonObject fmObj = JsonParser.parseString(fm).getAsJsonObject();
                            if (fmObj.has("depends")) {
                                JsonObject depends = fmObj.getAsJsonObject("depends");
                                if (depends.has("minecraft")) runtimeMc = depends.get("minecraft").getAsString();
                            }
                        }
                    } catch (Exception ex) {
                        // ignore
                    }
                }

                if (releaseMc != null && runtimeMc != null && !releaseMc.equals(runtimeMc)) {
                    // Do not auto-stage updates for a different Minecraft version.
                    return;
                }
            }
        }

        // Find the first JAR asset to download
        String downloadUrl = null;
        for (GitHubAsset a : assets) {
            if (a != null && a.getName() != null && a.getName().toLowerCase().endsWith(".jar")) {
                downloadUrl = a.getBrowserDownloadUrl();
                break;
            }
        }
        if (downloadUrl == null) return;

        Path modsDir = FabricLoader.getInstance().getGameDir().resolve("mods");
        if (!Files.exists(modsDir)) Files.createDirectories(modsDir);
        Path staged = modsDir.resolve("FascinatedUtils-update.jar");

        HttpRequest dlReq = HttpRequest.newBuilder().uri(URI.create(downloadUrl)).build();
        HttpResponse<InputStream> dlRes = client.send(dlReq, HttpResponse.BodyHandlers.ofInputStream());
        if (dlRes.statusCode() == 200) {
            try (InputStream is = dlRes.body()) {
                Files.copy(is, staged, StandardCopyOption.REPLACE_EXISTING);
            }

            // Post event so client code can notify user
            FascinatedEventBus.INSTANCE.post(new UpdateRequiredEvent(tagName, downloadUrl, staged.toString()));
        }
    }
}
