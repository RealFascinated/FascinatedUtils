package cc.fascinated.fascinatedutils.api;

import cc.fascinated.fascinatedutils.FascinatedUtils;
import cc.fascinated.fascinatedutils.api.dto.ChallengeResponse;
import cc.fascinated.fascinatedutils.api.dto.RefreshRequest;
import cc.fascinated.fascinatedutils.api.dto.RefreshResponse;
import cc.fascinated.fascinatedutils.api.dto.VerifyRequest;
import cc.fascinated.fascinatedutils.api.dto.VerifyResponse;
import cc.fascinated.fascinatedutils.client.Client;
import cc.fascinated.fascinatedutils.common.AlumiteEnvironment;
import cc.fascinated.fascinatedutils.event.impl.lifecycle.ClientStartedEvent;
import com.google.gson.Gson;
import lombok.SneakyThrows;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.Minecraft;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class AlumiteApi {

    public static final AlumiteApi INSTANCE = new AlumiteApi();
    private static final Gson GSON = new Gson();

    private final HttpClient httpClient;
    private final AlumiteTokenStore tokenStore;

    private volatile String activeAccountKey;

    private AlumiteApi() {
        httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        tokenStore = new AlumiteTokenStore();
    }

    @EventHandler
    private void fascinatedutils$onClientStarted(ClientStartedEvent event) {
        FascinatedUtils.SCHEDULED_POOL.execute(() -> authenticate(event.minecraftClient()));
    }

    private void authenticate(Minecraft minecraftClient) {
        String accountKey = minecraftClient.getUser().getProfileId().toString();
        activeAccountKey = accountKey;

        String storedRefresh = tokenStore.load(accountKey);
        if (storedRefresh != null && tryRefresh(accountKey, storedRefresh)) {
            return;
        }
        tokenStore.clear(accountKey);
        performFullLogin(minecraftClient);
    }

    private boolean tryRefresh(String accountKey, String storedRefreshToken) {
        try {
            HttpResponse<String> response = post("/auth/refresh", GSON.toJson(new RefreshRequest(storedRefreshToken)), null);
            if (response.statusCode() != 200) {
                return false;
            }

            RefreshResponse refreshResponse = GSON.fromJson(response.body(), RefreshResponse.class);
            storeSession(accountKey, refreshResponse.refreshToken());
            Client.LOG.info("[AlumiteApi] Session refreshed.");
            return true;
        } catch (Exception exception) {
            Client.LOG.warn("[AlumiteApi] Refresh failed: {}", exception.getMessage());
            return false;
        }
    }

    private void performFullLogin(Minecraft minecraftClient) {
        String minecraftAccessToken = minecraftClient.getUser().getAccessToken();
        if (minecraftAccessToken.length() < 16) {
            Client.LOG.info("[AlumiteApi] Skipping auth — offline/dev session detected.");
            return;
        }
        try {
            HttpResponse<String> challengeResponse = post("/auth/challenge", "{}", null);
            if (challengeResponse.statusCode() != 200) {
                Client.LOG.warn("[AlumiteApi] Challenge request failed with status {}", challengeResponse.statusCode());
                return;
            }

            ChallengeResponse challenge = GSON.fromJson(challengeResponse.body(), ChallengeResponse.class);
            VerifyRequest verifyRequest = new VerifyRequest(
                    challenge.challengeId(),
                    challenge.nonce(),
                    minecraftAccessToken
            );

            HttpResponse<String> verifyResponse = post("/auth/minecraft/verify", GSON.toJson(verifyRequest), null);
            if (verifyResponse.statusCode() != 200) {
                Client.LOG.warn("[AlumiteApi] Verify failed with status {}: {}", verifyResponse.statusCode(), verifyResponse.body());
                return;
            }

            VerifyResponse result = GSON.fromJson(verifyResponse.body(), VerifyResponse.class);
            storeSession(activeAccountKey, result.refreshToken());
            Client.LOG.info("[AlumiteApi] Authenticated as {}", result.user().minecraftName());
        } catch (Exception exception) {
            Client.LOG.warn("[AlumiteApi] Login failed: {}", exception.getMessage());
        }
    }

    private void storeSession(String accountKey, String newRefreshToken) {
        tokenStore.save(accountKey, newRefreshToken);
    }

    @SneakyThrows
    private HttpResponse<String> post(String path, String jsonBody, String bearerToken) {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(AlumiteEnvironment.API_BASE_URL + path))
                .header("Content-Type", "application/json")
                .header("User-Agent", AlumiteEnvironment.USER_AGENT)
                .timeout(Duration.ofSeconds(15))
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody));
        if (bearerToken != null) {
            builder.header("Authorization", "Bearer " + bearerToken);
        }
        return httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }
}
