package cc.fascinated.fascinatedutils.api;

import cc.fascinated.fascinatedutils.FascinatedUtils;
import cc.fascinated.fascinatedutils.api.dto.Presence;
import cc.fascinated.fascinatedutils.api.dto.auth.*;
import cc.fascinated.fascinatedutils.api.dto.friend.FriendEntryDto;
import cc.fascinated.fascinatedutils.api.dto.friend.PendingFriendRequestDto;
import cc.fascinated.fascinatedutils.api.dto.friend.SendFriendRequestRequest;
import cc.fascinated.fascinatedutils.api.dto.presence.UpdatePresenceRequest;
import cc.fascinated.fascinatedutils.client.Client;
import cc.fascinated.fascinatedutils.common.AlumiteEnvironment;
import cc.fascinated.fascinatedutils.common.JsonUtils;
import cc.fascinated.fascinatedutils.event.FascinatedEventBus;
import cc.fascinated.fascinatedutils.event.impl.lifecycle.AlumiteAuthenticatedEvent;
import cc.fascinated.fascinatedutils.event.impl.lifecycle.ClientStartedEvent;
import cc.fascinated.fascinatedutils.event.impl.lifecycle.ClientStoppingEvent;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
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
    private static final String ROUTE_PRESENCE = "/presence";

    private final HttpClient httpClient;
    private final AlumiteTokenStore tokenStore;
    private final AlumiteGateway gateway;

    private volatile String activeAccountKey;
    private volatile String activeAccessToken;
    private volatile String activeRefreshToken;
    private volatile Presence activePreferredPresence = Presence.ONLINE;
    private volatile ScheduledFuture<?> tokenRefreshTask;

    private AlumiteApi() {
        httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
        tokenStore = new AlumiteTokenStore();
        gateway = new AlumiteGateway(httpClient, () -> activeRefreshToken, this::onGatewayAuthExpired);
    }

    private static Presence normalizePreferredPresence(Presence presence) {
        if (presence == null || presence == Presence.OFFLINE) {
            return Presence.ONLINE;
        }
        return presence;
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
        String nextAccountKey = minecraftClient.getUser().getProfileId().toString();
        if (activeAccountKey == null || !activeAccountKey.equals(nextAccountKey)) {
            activePreferredPresence = Presence.ONLINE;
        }
        activeAccountKey = nextAccountKey;

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
            HttpResponse<String> response = sendRaw("POST", "/auth/refresh", GSON.toJson(new RefreshRequest(storedRefreshToken)), null);
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
            VerifyRequest verifyRequest = new VerifyRequest(challenge.challengeId(), challenge.nonce(), minecraftAccessToken);
            HttpResponse<String> verifyResponse = sendRaw("POST", "/auth/minecraft/verify", GSON.toJson(verifyRequest), null);
            if (verifyResponse.statusCode() != 200) {
                Client.LOG.warn("[AlumiteApi] Verify failed with status {}: {}", verifyResponse.statusCode(), verifyResponse.body());
                return;
            }
            VerifyResponse result = GSON.fromJson(verifyResponse.body(), VerifyResponse.class);
            activeAccessToken = result.accessToken();
            activeRefreshToken = result.refreshToken();
            activePreferredPresence = normalizePreferredPresence(result.user().preferredPresence());
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
            long delayMs = Math.max(0L, Instant.parse(accessExpiresAt).toEpochMilli() - System.currentTimeMillis() - 60_000L);
            String accountKey = activeAccountKey;
            String capturedRefresh = activeRefreshToken;
            tokenRefreshTask = FascinatedUtils.SCHEDULED_POOL.schedule(() -> {
                if (capturedRefresh != null && accountKey != null) {
                    Client.LOG.info("[AlumiteApi] Refreshing access token...");
                    tryRefresh(accountKey, capturedRefresh);
                }
            }, delayMs, TimeUnit.MILLISECONDS);
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
        return getList(ROUTE_FRIENDS, FriendEntryDto.class, "get friends", "Failed to load friends.");
    }

    public PendingFriendRequestDto sendFriendRequest(String targetUsername) {
        try {
            HttpResponse<String> response = sendAuthorizedChecked("POST", ROUTE_FRIENDS_REQUESTS, GSON.toJson(new SendFriendRequestRequest(targetUsername)), "send friend request", "Failed to send request.");
            return GSON.fromJson(response.body(), PendingFriendRequestDto.class);
        } catch (AlumiteApiException exception) {
            throw exception;
        } catch (Exception exception) {
            throw wrapRequestException("send friend request", exception, "Failed to send request.");
        }
    }

    public List<PendingFriendRequestDto> getIncomingFriendRequests() {
        return getList(ROUTE_FRIENDS_REQUESTS_INCOMING, PendingFriendRequestDto.class, "get incoming friend requests", "Failed to load incoming requests.");
    }

    public List<PendingFriendRequestDto> getOutgoingFriendRequests() {
        return getList(ROUTE_FRIENDS_REQUESTS_OUTGOING, PendingFriendRequestDto.class, "get outgoing friend requests", "Failed to load outgoing requests.");
    }

    public boolean acceptFriendRequest(int requestId) {
        return postFriendRequestAction(requestId, "accept", "Failed to accept request.");
    }

    public boolean declineFriendRequest(int requestId) {
        return postFriendRequestAction(requestId, "decline", "Failed to decline request.");
    }

    public boolean cancelFriendRequest(int requestId) {
        try {
            sendAuthorizedChecked("DELETE", ROUTE_FRIENDS_REQUESTS + "/" + requestId, null, "cancel friend request", "Failed to cancel request.");
            return true;
        } catch (AlumiteApiException exception) {
            throw exception;
        } catch (Exception exception) {
            throw wrapRequestException("cancel friend request", exception, "Failed to cancel request.");
        }
    }

    public boolean removeFriend(int userId) {
        try {
            sendAuthorizedChecked("DELETE", ROUTE_FRIENDS + "/" + userId, null, "remove friend", "Failed to remove friend.");
            return true;
        } catch (AlumiteApiException exception) {
            throw exception;
        } catch (Exception exception) {
            throw wrapRequestException("remove friend", exception, "Failed to remove friend.");
        }
    }

    public boolean updatePreferredPresence(Presence presence) {
        Presence resolvedPresence = normalizePreferredPresence(presence);
        if (presence == null) {
            throw new IllegalArgumentException("Preferred presence is required.");
        }
        if (presence == Presence.OFFLINE) {
            throw new IllegalArgumentException("Preferred presence cannot be offline.");
        }
        try {
            sendAuthorizedChecked("PATCH", ROUTE_PRESENCE, GSON.toJson(new UpdatePresenceRequest(resolvedPresence)), "update preferred presence", "Failed to update preferred presence.");
            activePreferredPresence = resolvedPresence;
            return true;
        } catch (AlumiteApiException exception) {
            throw exception;
        } catch (Exception exception) {
            throw wrapRequestException("update preferred presence", exception, "Failed to update preferred presence.");
        }
    }

    public Presence currentPreferredPresence() {
        return normalizePreferredPresence(activePreferredPresence);
    }

    public String activeAccessToken() {
        return activeAccessToken;
    }

    private boolean postFriendRequestAction(int requestId, String action, String fallbackMessage) {
        try {
            sendAuthorizedChecked("POST", ROUTE_FRIENDS_REQUESTS + "/" + requestId + "/" + action, "{}", action + " friend request", fallbackMessage);
            return true;
        } catch (AlumiteApiException exception) {
            throw exception;
        } catch (Exception exception) {
            throw wrapRequestException(action + " friend request", exception, fallbackMessage);
        }
    }

    private <T> List<T> getList(String path, Class<T> type, String actionName, String fallbackMessage) {
        try {
            HttpResponse<String> response = sendAuthorizedChecked("GET", path, null, actionName, fallbackMessage);
            return GSON.fromJson(response.body(), TypeToken.getParameterized(List.class, type).getType());
        } catch (AlumiteApiException exception) {
            throw exception;
        } catch (Exception exception) {
            throw wrapRequestException(actionName, exception, fallbackMessage);
        }
    }

    private HttpResponse<String> sendAuthorizedChecked(String method, String path, String body, String actionName, String fallbackMessage) {
        try {
            HttpResponse<String> response = sendAuthorized(method, path, body);
            if (response.statusCode() != 200) {
                Client.LOG.warn("[AlumiteApi] {} failed with status {}: {}", actionName, response.statusCode(), response.body());
                throw parseApiException(response.body(), fallbackMessage);
            }
            return response;
        } catch (AlumiteApiException exception) {
            throw exception;
        } catch (Exception exception) {
            throw wrapRequestException(actionName, exception, fallbackMessage);
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
        HttpRequest.Builder builder = HttpRequest.newBuilder().uri(URI.create(AlumiteEnvironment.API_BASE_URL + path)).header("Content-Type", "application/json").header("User-Agent", AlumiteEnvironment.USER_AGENT).timeout(Duration.ofSeconds(15)).method(method, body != null ? HttpRequest.BodyPublishers.ofString(body) : HttpRequest.BodyPublishers.noBody());
        if (token != null) {
            builder.header("Authorization", "Bearer " + token);
        }
        return builder.build();
    }

    private boolean refreshActiveToken() {
        String refreshToken = activeRefreshToken;
        return refreshToken != null && tryRefresh(activeAccountKey, refreshToken);
    }

    private AlumiteApiException parseApiException(String responseBody, String fallbackMessage) {
        try {
            JsonObject root = GSON.fromJson(responseBody, JsonObject.class);
            Errors error = Errors.fromCode(JsonUtils.stringMember(root, "error"));
            if (error == null) {
                error = Errors.fromCode(JsonUtils.stringMember(root, "code"));
            }
            String message = JsonUtils.stringMember(root, "message", fallbackMessage);
            if ((message == null || message.isBlank()) && error != null) {
                message = error.getDisplayText();
            }
            return new AlumiteApiException(error, message);
        } catch (Exception ignored) {
            return new AlumiteApiException(null, fallbackMessage);
        }
    }

    private AlumiteApiException wrapRequestException(String actionName, Exception exception, String fallbackMessage) {
        Client.LOG.warn("[AlumiteApi] {} failed: {}", actionName, exception.getMessage());
        return new AlumiteApiException(null, fallbackMessage);
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