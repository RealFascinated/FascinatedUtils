package cc.fascinated.fascinatedutils.api;

import cc.fascinated.fascinatedutils.FascinatedUtils;
import cc.fascinated.fascinatedutils.api.dto.auth.ChallengeResponse;
import cc.fascinated.fascinatedutils.api.dto.auth.RefreshRequest;
import cc.fascinated.fascinatedutils.api.dto.auth.RefreshResponse;
import cc.fascinated.fascinatedutils.api.dto.auth.VerifyRequest;
import cc.fascinated.fascinatedutils.api.dto.auth.VerifyResponse;
import cc.fascinated.fascinatedutils.api.dto.friend.FriendEntryDto;
import cc.fascinated.fascinatedutils.api.dto.friend.PendingFriendRequestDto;
import cc.fascinated.fascinatedutils.api.dto.friend.SendFriendRequestRequest;
import cc.fascinated.fascinatedutils.client.Client;
import cc.fascinated.fascinatedutils.common.AlumiteEnvironment;
import cc.fascinated.fascinatedutils.event.FascinatedEventBus;
import cc.fascinated.fascinatedutils.event.impl.lifecycle.AlumiteAuthenticatedEvent;
import cc.fascinated.fascinatedutils.event.impl.lifecycle.ClientStartedEvent;
import cc.fascinated.fascinatedutils.event.impl.lifecycle.ClientStoppingEvent;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import lombok.SneakyThrows;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.Minecraft;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;

public class AlumiteApi {

    public static final AlumiteApi INSTANCE = new AlumiteApi();
    private static final Gson GSON = new Gson();
    private static final String ROUTE_FRIENDS = "/friends";
    private static final String ROUTE_FRIENDS_REQUESTS = "/friends/requests";
    private static final String ROUTE_FRIENDS_REQUESTS_INCOMING = "/friends/requests/incoming";
    private static final String ROUTE_FRIENDS_REQUESTS_OUTGOING = "/friends/requests/outgoing";

    private final HttpClient httpClient;
    private final AlumiteTokenStore tokenStore;
    private final AlumiteGateway gateway;

    private volatile String activeAccountKey;
    private volatile String activeAccessToken;
    private volatile String activeRefreshToken;

    private AlumiteApi() {
        httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        tokenStore = new AlumiteTokenStore();
        gateway = new AlumiteGateway(httpClient, () -> activeRefreshToken, this::onGatewayAuthExpired);
    }

    @EventHandler
    private void fascinatedutils$onClientStarted(ClientStartedEvent event) {
        FascinatedUtils.SCHEDULED_POOL.execute(() -> authenticate(event.minecraftClient()));
    }

    @EventHandler
    private void fascinatedutils$onClientStopping(ClientStoppingEvent event) {
        gateway.disconnect();
    }

    private void authenticate(Minecraft minecraftClient) {
        String accountKey = minecraftClient.getUser().getProfileId().toString();
        activeAccountKey = accountKey;

        String storedRefresh = tokenStore.load(accountKey);
        if (storedRefresh != null && tryRefresh(accountKey, storedRefresh)) {
            gateway.connect();
            FascinatedEventBus.INSTANCE.post(new AlumiteAuthenticatedEvent());
            return;
        }
        tokenStore.clear(accountKey);
        performFullLogin(minecraftClient);
        if (activeAccessToken != null) {
            gateway.connect();
            FascinatedEventBus.INSTANCE.post(new AlumiteAuthenticatedEvent());
        }
    }

    private boolean tryRefresh(String accountKey, String storedRefreshToken) {
        try {
            HttpResponse<String> response = post("/auth/refresh", GSON.toJson(new RefreshRequest(storedRefreshToken)), null);
            if (response.statusCode() != 200) {
                return false;
            }

            RefreshResponse refreshResponse = GSON.fromJson(response.body(), RefreshResponse.class);
            activeAccessToken = refreshResponse.accessToken();
            activeRefreshToken = refreshResponse.refreshToken();
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
            activeAccessToken = result.accessToken();
            activeRefreshToken = result.refreshToken();
            storeSession(activeAccountKey, result.refreshToken());
            Client.LOG.info("[AlumiteApi] Authenticated as {}", result.user().minecraftName());
        } catch (Exception exception) {
            Client.LOG.warn("[AlumiteApi] Login failed: {}", exception.getMessage());
        }
    }

    private void onGatewayAuthExpired() {
        Client.LOG.info("[AlumiteApi] Gateway refresh token expired, re-authenticating...");
        String currentRefresh = activeRefreshToken;
        activeAccessToken = null;
        activeRefreshToken = null;
        FascinatedUtils.SCHEDULED_POOL.execute(() -> {
            if (currentRefresh != null && tryRefresh(activeAccountKey, currentRefresh)) {
                gateway.connect();
                return;
            }
            tokenStore.clear(activeAccountKey);
            performFullLogin(Minecraft.getInstance());
            if (activeRefreshToken != null) {
                gateway.connect();
            }
        });
    }

    public List<FriendEntryDto> getFriends() {
        try {
            HttpResponse<String> response = getAuthorized(ROUTE_FRIENDS);
            if (response.statusCode() != 200) {
                Client.LOG.warn("[AlumiteApi] Get friends failed with status {}", response.statusCode());
                return List.of();
            }
            return GSON.fromJson(response.body(), TypeToken.getParameterized(List.class, FriendEntryDto.class).getType());
        } catch (Exception exception) {
            Client.LOG.warn("[AlumiteApi] Get friends failed: {}", exception.getMessage());
            return List.of();
        }
    }

    public PendingFriendRequestDto sendFriendRequest(String targetUsername) {
        try {
            SendFriendRequestRequest request = new SendFriendRequestRequest(targetUsername);
            HttpResponse<String> response = post(ROUTE_FRIENDS_REQUESTS, GSON.toJson(request), requireAccessToken());
            if (response.statusCode() != 200) {
                Client.LOG.warn("[AlumiteApi] Send friend request failed with status {}: {}", response.statusCode(), response.body());
                return null;
            }
            return GSON.fromJson(response.body(), PendingFriendRequestDto.class);
        } catch (Exception exception) {
            Client.LOG.warn("[AlumiteApi] Send friend request failed: {}", exception.getMessage());
            return null;
        }
    }

    public List<PendingFriendRequestDto> getIncomingFriendRequests() {
        try {
            HttpResponse<String> response = getAuthorized(ROUTE_FRIENDS_REQUESTS_INCOMING);
            if (response.statusCode() != 200) {
                Client.LOG.warn("[AlumiteApi] Get incoming friend requests failed with status {}", response.statusCode());
                return List.of();
            }
            return GSON.fromJson(response.body(), TypeToken.getParameterized(List.class, PendingFriendRequestDto.class).getType());
        } catch (Exception exception) {
            Client.LOG.warn("[AlumiteApi] Get incoming friend requests failed: {}", exception.getMessage());
            return List.of();
        }
    }

    public List<PendingFriendRequestDto> getOutgoingFriendRequests() {
        try {
            HttpResponse<String> response = getAuthorized(ROUTE_FRIENDS_REQUESTS_OUTGOING);
            if (response.statusCode() != 200) {
                Client.LOG.warn("[AlumiteApi] Get outgoing friend requests failed with status {}", response.statusCode());
                return List.of();
            }
            return GSON.fromJson(response.body(), TypeToken.getParameterized(List.class, PendingFriendRequestDto.class).getType());
        } catch (Exception exception) {
            Client.LOG.warn("[AlumiteApi] Get outgoing friend requests failed: {}", exception.getMessage());
            return List.of();
        }
    }

    public boolean acceptFriendRequest(int requestId) {
        return postFriendRequestAction(requestId, "accept", "accept friend request");
    }

    public boolean declineFriendRequest(int requestId) {
        return postFriendRequestAction(requestId, "decline", "decline friend request");
    }

    public boolean cancelFriendRequest(int requestId) {
        try {
            HttpResponse<String> response = deleteAuthorized(ROUTE_FRIENDS_REQUESTS + "/" + requestId);
            if (response.statusCode() != 200) {
                Client.LOG.warn("[AlumiteApi] Cancel friend request failed with status {}: {}", response.statusCode(), response.body());
                return false;
            }
            return true;
        } catch (Exception exception) {
            Client.LOG.warn("[AlumiteApi] Cancel friend request failed: {}", exception.getMessage());
            return false;
        }
    }

    public boolean removeFriend(int userId) {
        try {
            HttpResponse<String> response = deleteAuthorized(ROUTE_FRIENDS + "/" + userId);
            if (response.statusCode() != 200) {
                Client.LOG.warn("[AlumiteApi] Remove friend failed with status {}: {}", response.statusCode(), response.body());
                return false;
            }
            return true;
        } catch (Exception exception) {
            Client.LOG.warn("[AlumiteApi] Remove friend failed: {}", exception.getMessage());
            return false;
        }
    }

    public String activeAccessToken() {
        return activeAccessToken;
    }

    private boolean postFriendRequestAction(int requestId, String action, String actionName) {
        try {
            HttpResponse<String> response = post(ROUTE_FRIENDS_REQUESTS + "/" + requestId + "/" + action, "{}", requireAccessToken());
            if (response.statusCode() != 200) {
                Client.LOG.warn("[AlumiteApi] {} failed with status {}: {}", actionName, response.statusCode(), response.body());
                return false;
            }
            return true;
        } catch (Exception exception) {
            Client.LOG.warn("[AlumiteApi] {} failed: {}", actionName, exception.getMessage());
            return false;
        }
    }

    @SneakyThrows
    private HttpResponse<String> getAuthorized(String path) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(AlumiteEnvironment.API_BASE_URL + path))
                .header("Content-Type", "application/json")
                .header("User-Agent", AlumiteEnvironment.USER_AGENT)
                .header("Authorization", "Bearer " + requireAccessToken())
                .timeout(Duration.ofSeconds(15))
                .GET()
                .build();
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    @SneakyThrows
    private HttpResponse<String> deleteAuthorized(String path) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(AlumiteEnvironment.API_BASE_URL + path))
                .header("Content-Type", "application/json")
                .header("User-Agent", AlumiteEnvironment.USER_AGENT)
                .header("Authorization", "Bearer " + requireAccessToken())
                .timeout(Duration.ofSeconds(15))
                .DELETE()
                .build();
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private String requireAccessToken() {
        if (activeAccessToken == null || activeAccessToken.isBlank()) {
            throw new IllegalStateException("No active Alumite access token available");
        }
        return activeAccessToken;
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
