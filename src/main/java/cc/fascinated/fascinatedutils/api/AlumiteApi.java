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
import java.time.Instant;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

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
    private volatile ScheduledFuture<?> tokenRefreshTask;

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
        activeAccountKey = minecraftClient.getUser().getProfileId().toString();

        String storedRefresh = tokenStore.load(activeAccountKey);
        if (storedRefresh != null && tryRefresh(activeAccountKey, storedRefresh)) {
            gateway.connect();
            FascinatedEventBus.INSTANCE.post(new AlumiteAuthenticatedEvent());
            return;
        }
        tokenStore.clear(activeAccountKey);
        performFullLogin(minecraftClient);
        if (activeAccessToken != null) {
            gateway.connect();
            FascinatedEventBus.INSTANCE.post(new AlumiteAuthenticatedEvent());
        }
    }

    private boolean tryRefresh(String accountKey, String storedRefreshToken) {
        try {
            HttpResponse<String> response = sendRaw("POST", "/auth/refresh",
                    GSON.toJson(new RefreshRequest(storedRefreshToken)), null);
            if (response.statusCode() != 200) {
                return false;
            }
            RefreshResponse refreshResponse = GSON.fromJson(response.body(), RefreshResponse.class);
            activeAccessToken = refreshResponse.accessToken();
            activeRefreshToken = refreshResponse.refreshToken();
            storeSession(accountKey, refreshResponse.refreshToken());
            scheduleTokenRefresh(refreshResponse.accessExpiresAt());
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
            HttpResponse<String> challengeResponse = sendRaw("POST", "/auth/challenge", "{}", null);
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
            HttpResponse<String> verifyResponse = sendRaw("POST", "/auth/minecraft/verify",
                    GSON.toJson(verifyRequest), null);
            if (verifyResponse.statusCode() != 200) {
                Client.LOG.warn("[AlumiteApi] Verify failed with status {}: {}",
                        verifyResponse.statusCode(), verifyResponse.body());
                return;
            }
            VerifyResponse result = GSON.fromJson(verifyResponse.body(), VerifyResponse.class);
            activeAccessToken = result.accessToken();
            activeRefreshToken = result.refreshToken();
            storeSession(activeAccountKey, result.refreshToken());
            scheduleTokenRefresh(result.accessExpiresAt());
            Client.LOG.info("[AlumiteApi] Authenticated as {}", result.user().minecraftName());
        } catch (Exception exception) {
            Client.LOG.warn("[AlumiteApi] Login failed: {}", exception.getMessage());
        }
    }

    private void scheduleTokenRefresh(String accessExpiresAt) {
        ScheduledFuture<?> existing = tokenRefreshTask;
        if (existing != null) {
            existing.cancel(false);
        }
        if (accessExpiresAt == null) {
            return;
        }
        try {
            long delayMs = Math.max(
                    0L,
                    Instant.parse(accessExpiresAt).toEpochMilli() - System.currentTimeMillis() - 60_000L
            );
            String accountKey = activeAccountKey;
            String capturedRefresh = activeRefreshToken;
            tokenRefreshTask = FascinatedUtils.SCHEDULED_POOL.schedule(
                    () -> {
                        if (capturedRefresh != null && accountKey != null) {
                            Client.LOG.info("[AlumiteApi] Refreshing access token...");
                            tryRefresh(accountKey, capturedRefresh);
                        }
                    },
                    delayMs,
                    TimeUnit.MILLISECONDS
            );
        } catch (Exception exception) {
            Client.LOG.warn("[AlumiteApi] Failed to schedule token refresh: {}", exception.getMessage());
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
        return getList(ROUTE_FRIENDS, FriendEntryDto.class, "get friends");
    }

    public PendingFriendRequestDto sendFriendRequest(String targetUsername) {
        try {
            HttpResponse<String> response = sendAuthorized("POST", ROUTE_FRIENDS_REQUESTS,
                    GSON.toJson(new SendFriendRequestRequest(targetUsername)));
            if (response.statusCode() != 200) {
                Client.LOG.warn("[AlumiteApi] Send friend request failed with status {}: {}",
                        response.statusCode(), response.body());
                return null;
            }
            return GSON.fromJson(response.body(), PendingFriendRequestDto.class);
        } catch (Exception exception) {
            Client.LOG.warn("[AlumiteApi] Send friend request failed: {}", exception.getMessage());
            return null;
        }
    }

    public List<PendingFriendRequestDto> getIncomingFriendRequests() {
        return getList(ROUTE_FRIENDS_REQUESTS_INCOMING, PendingFriendRequestDto.class, "get incoming friend requests");
    }

    public List<PendingFriendRequestDto> getOutgoingFriendRequests() {
        return getList(ROUTE_FRIENDS_REQUESTS_OUTGOING, PendingFriendRequestDto.class, "get outgoing friend requests");
    }

    public boolean acceptFriendRequest(int requestId) {
        return postFriendRequestAction(requestId, "accept");
    }

    public boolean declineFriendRequest(int requestId) {
        return postFriendRequestAction(requestId, "decline");
    }

    public boolean cancelFriendRequest(int requestId) {
        try {
            HttpResponse<String> response = sendAuthorized("DELETE",
                    ROUTE_FRIENDS_REQUESTS + "/" + requestId, null);
            if (response.statusCode() != 200) {
                Client.LOG.warn("[AlumiteApi] Cancel friend request failed with status {}: {}",
                        response.statusCode(), response.body());
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
            HttpResponse<String> response = sendAuthorized("DELETE", ROUTE_FRIENDS + "/" + userId, null);
            if (response.statusCode() != 200) {
                Client.LOG.warn("[AlumiteApi] Remove friend failed with status {}: {}",
                        response.statusCode(), response.body());
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

    private boolean postFriendRequestAction(int requestId, String action) {
        try {
            HttpResponse<String> response = sendAuthorized("POST",
                    ROUTE_FRIENDS_REQUESTS + "/" + requestId + "/" + action, "{}");
            if (response.statusCode() != 200) {
                Client.LOG.warn("[AlumiteApi] {} friend request failed with status {}: {}",
                        action, response.statusCode(), response.body());
                return false;
            }
            return true;
        } catch (Exception exception) {
            Client.LOG.warn("[AlumiteApi] {} friend request failed: {}", action, exception.getMessage());
            return false;
        }
    }

    private <T> List<T> getList(String path, Class<T> type, String actionName) {
        try {
            HttpResponse<String> response = sendAuthorized("GET", path, null);
            if (response.statusCode() != 200) {
                Client.LOG.warn("[AlumiteApi] {} failed with status {}", actionName, response.statusCode());
                return List.of();
            }
            return GSON.fromJson(response.body(), TypeToken.getParameterized(List.class, type).getType());
        } catch (Exception exception) {
            Client.LOG.warn("[AlumiteApi] {} failed: {}", actionName, exception.getMessage());
            return List.of();
        }
    }

    @SneakyThrows
    private HttpResponse<String> sendAuthorized(String method, String path, String body) {
        Supplier<HttpRequest> buildRequest = () -> buildRequest(path, method, body, requireAccessToken());
        HttpResponse<String> response = httpClient.send(buildRequest.get(), HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() == 401 && refreshActiveToken()) {
            response = httpClient.send(buildRequest.get(), HttpResponse.BodyHandlers.ofString());
        }
        return response;
    }

    @SneakyThrows
    private HttpResponse<String> sendRaw(String method, String path, String body, String token) {
        return httpClient.send(buildRequest(path, method, body, token), HttpResponse.BodyHandlers.ofString());
    }

    private HttpRequest buildRequest(String path, String method, String body, String token) {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(AlumiteEnvironment.API_BASE_URL + path))
                .header("Content-Type", "application/json")
                .header("User-Agent", AlumiteEnvironment.USER_AGENT)
                .timeout(Duration.ofSeconds(15))
                .method(method, body != null
                        ? HttpRequest.BodyPublishers.ofString(body)
                        : HttpRequest.BodyPublishers.noBody());
        if (token != null) {
            builder.header("Authorization", "Bearer " + token);
        }
        return builder.build();
    }

    private boolean refreshActiveToken() {
        String refreshToken = activeRefreshToken;
        return refreshToken != null && tryRefresh(activeAccountKey, refreshToken);
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
}