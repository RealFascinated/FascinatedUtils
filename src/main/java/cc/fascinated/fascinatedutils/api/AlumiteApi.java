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
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.Minecraft;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;

public class AlumiteApi {

    private static final Gson GSON = new Gson();

    public static final AlumiteApi INSTANCE = new AlumiteApi();

    private final HttpClient httpClient;
    private final AlumiteTokenStore tokenStore;

    private volatile String accessToken;
    private volatile Instant accessExpiresAt;
    private volatile String refreshToken;

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
        String storedRefresh = tokenStore.load();
        if (storedRefresh != null && tryRefresh(storedRefresh)) {
            return;
        }
        tokenStore.clear();
        performFullLogin(minecraftClient);
    }

    private boolean tryRefresh(String storedRefreshToken) {
        try {
            HttpResponse<String> response = post("/auth/refresh", GSON.toJson(new RefreshRequest(storedRefreshToken)), null);
            if (response.statusCode() != 200) {
                return false;
            }

            RefreshResponse refreshResponse = GSON.fromJson(response.body(), RefreshResponse.class);
            storeSession(
                    refreshResponse.accessToken(),
                    Instant.parse(refreshResponse.accessExpiresAt()),
                    refreshResponse.refreshToken()
            );
            Client.LOG.info("[AlumiteApi] Session refreshed.");
            return true;
        } catch (Exception exception) {
            Client.LOG.warn("[AlumiteApi] Refresh failed: {}", exception.getMessage());
            return false;
        }
    }

    private void performFullLogin(Minecraft minecraftClient) {
        String accessToken = minecraftClient.getUser().getAccessToken();
        if (accessToken.length() < 16) {
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
                    accessToken
            );

            HttpResponse<String> verifyResponse = post("/auth/minecraft/verify", GSON.toJson(verifyRequest), null);
            if (verifyResponse.statusCode() != 200) {
                Client.LOG.warn("[AlumiteApi] Verify failed with status {}: {}", verifyResponse.statusCode(), verifyResponse.body());
                return;
            }

            VerifyResponse result = GSON.fromJson(verifyResponse.body(), VerifyResponse.class);
            storeSession(
                    result.accessToken(),
                    Instant.parse(result.accessExpiresAt()),
                    result.refreshToken()
            );
            Client.LOG.info("[AlumiteApi] Authenticated as {}", result.user().minecraftName());
        } catch (Exception exception) {
            Client.LOG.warn("[AlumiteApi] Login failed: {}", exception.getMessage());
        }
    }

    private void storeSession(String newAccessToken, Instant newAccessExpiresAt, String newRefreshToken) {
        accessToken = newAccessToken;
        accessExpiresAt = newAccessExpiresAt;
        refreshToken = newRefreshToken;
        tokenStore.save(newRefreshToken);
    }

    /**
     * Returns a valid access token for use in API requests, silently refreshing if near expiry.
     *
     * <p>Returns {@code null} if authentication has not completed or has failed.
     *
     * @return the current access token, or {@code null} if unavailable
     */
    public String getAccessToken() {
        if (accessToken != null && accessExpiresAt != null
                && Instant.now().isBefore(accessExpiresAt.minusSeconds(30))) {
            return accessToken;
        }
        if (refreshToken != null) {
            tryRefresh(refreshToken);
        }
        return accessToken;
    }

    private HttpResponse<String> post(String path, String jsonBody, String bearerToken) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(AlumiteEnvironment.API_BASE_URL + path))
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(15))
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody));
        if (bearerToken != null) {
            builder.header("Authorization", "Bearer " + bearerToken);
        }
        return httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }
}
