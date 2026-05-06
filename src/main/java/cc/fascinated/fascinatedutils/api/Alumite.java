package cc.fascinated.fascinatedutils.api;

import cc.fascinated.fascinatedutils.Constants;
import cc.fascinated.fascinatedutils.AlumiteMod;
import cc.fascinated.fascinatedutils.api.auth.json.*;
import cc.fascinated.fascinatedutils.api.channel.AlumiteChannels;
import cc.fascinated.fascinatedutils.api.channel.json.ChannelDetailDTO;
import cc.fascinated.fascinatedutils.api.channel.json.OpenDmBodyDTO;
import cc.fascinated.fascinatedutils.api.friend.PendingFriendRequest;
import cc.fascinated.fascinatedutils.api.friend.json.FriendEntryDTO;
import cc.fascinated.fascinatedutils.api.friend.json.PendingFriendRequestDTO;
import cc.fascinated.fascinatedutils.api.friend.json.SendFriendRequestBodyDTO;
import cc.fascinated.fascinatedutils.api.internal.AlumiteHttpClient;
import cc.fascinated.fascinatedutils.api.user.AlumiteUsers;
import cc.fascinated.fascinatedutils.api.user.Presence;
import cc.fascinated.fascinatedutils.api.user.json.PublicUserDTO;
import cc.fascinated.fascinatedutils.api.user.json.UpdatePresenceBodyDTO;
import cc.fascinated.fascinatedutils.api.user.json.UserDTO;
import cc.fascinated.fascinatedutils.client.Client;
import cc.fascinated.fascinatedutils.event.FascinatedEventBus;
import cc.fascinated.fascinatedutils.event.impl.lifecycle.AlumiteAuthenticatedEvent;
import cc.fascinated.fascinatedutils.event.impl.lifecycle.ClientStartedEvent;
import cc.fascinated.fascinatedutils.event.impl.lifecycle.ClientStoppingEvent;
import lombok.Getter;
import lombok.experimental.Accessors;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.Minecraft;

import java.net.http.HttpClient;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

@Accessors(fluent = true)
public class Alumite {

    public static final Alumite INSTANCE = new Alumite();

    private static final String ROUTE_FRIENDS = "/friends";
    private static final String ROUTE_FRIENDS_REQUESTS = "/friends/requests";
    private static final String ROUTE_FRIENDS_REQUESTS_INCOMING = "/friends/requests/incoming";
    private static final String ROUTE_FRIENDS_REQUESTS_OUTGOING = "/friends/requests/outgoing";
    private static final String ROUTE_PRESENCE = "/presence";
    private static final String ROUTE_USERS = "/users";
    private static final String ROUTE_CHANNELS = "/channels";

    private final HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    @Getter
    private final AlumiteHttpClient http;
    private final AlumiteTokenStore tokenStore = new AlumiteTokenStore();
    private final AlumiteGateway gateway;
    
    @Getter
    private final AlumiteUsers users;
    @Getter
    private final AlumiteChannels channels;

    private volatile String activeAccountKey;
    @Getter
    private volatile String activeAccessToken;
    private volatile String activeRefreshToken;

    private volatile ScheduledFuture<?> tokenRefreshTask;

    private Alumite() {
        this.http = new AlumiteHttpClient(httpClient, Constants.GSON, () -> activeAccessToken, this::refreshActiveToken);
        this.gateway = new AlumiteGateway(httpClient, () -> activeRefreshToken, this::onGatewayAuthExpired, Constants.GSON);
        this.users = new AlumiteUsers(this);
        this.channels = new AlumiteChannels(this, users);
    }

    private void authenticate(Minecraft minecraftClient) {
        activeAccountKey = minecraftClient.getUser().getProfileId().toString();

        AlumiteTokenStore.StoredSession stored = tokenStore.load(activeAccountKey);
        if (stored != null) {
            if (isAccessTokenValid(stored.accessExpiresAt())) {
                activeAccessToken = stored.accessToken();
                activeRefreshToken = stored.refreshToken();
                updateSelfUser();
                scheduleTokenRefresh(stored.accessExpiresAt());
                Client.LOG.info("[Alumite] Resumed session from stored access token.");
                gateway.connect();
                FascinatedEventBus.INSTANCE.post(new AlumiteAuthenticatedEvent());
                return;
            }
            if (tryRefresh(activeAccountKey, stored.refreshToken())) {
                gateway.connect();
                FascinatedEventBus.INSTANCE.post(new AlumiteAuthenticatedEvent());
                return;
            }
        }
        tokenStore.clear(activeAccountKey);
        performFullLogin(minecraftClient);
        if (activeAccessToken != null) {
            gateway.connect();
            FascinatedEventBus.INSTANCE.post(new AlumiteAuthenticatedEvent());
        }
    }

    private boolean isAccessTokenValid(String accessExpiresAt) {
        if (accessExpiresAt == null) {
            return false;
        }
        try {
            return Instant.parse(accessExpiresAt).toEpochMilli() - System.currentTimeMillis() > 60_000L;
        } catch (Exception exception) {
            return false;
        }
    }

    private boolean tryRefresh(String accountKey, String storedRefreshToken) {
        try {
            var response = http.sendRaw("POST", "/auth/refresh", Constants.GSON.toJson(new RefreshRequestDTO(storedRefreshToken)), null);
            if (response.statusCode() != 200) {
                return false;
            }
            RefreshResponseDTO refreshResponse = Constants.GSON.fromJson(response.body(), RefreshResponseDTO.class);
            activeAccessToken = refreshResponse.accessToken();
            activeRefreshToken = refreshResponse.refreshToken();
            updateSelfUser();
            storeSession(accountKey, refreshResponse.refreshToken(), refreshResponse.accessToken(), refreshResponse.accessExpiresAt());
            scheduleTokenRefresh(refreshResponse.accessExpiresAt());
            Client.LOG.info("[Alumite] Session refreshed.");
            return true;
        } catch (Exception exception) {
            activeAccessToken = null;
            activeRefreshToken = null;
            Client.LOG.warn("[Alumite] Refresh failed: {}", exception.getMessage());
            return false;
        }
    }

    private void updateSelfUser() {
        users.setSelfUser(http.getObject(ROUTE_USERS + "/@me", UserDTO.class, "get current user", "Failed to load current user."));
    }

    private void performFullLogin(Minecraft minecraftClient) {
        String minecraftAccessToken = minecraftClient.getUser().getAccessToken();
        if (minecraftAccessToken.length() < 16) {
            Client.LOG.info("[Alumite] Skipping auth — offline/dev session detected.");
            return;
        }
        try {
            var challengeResponse = http.sendRaw("POST", "/auth/challenge", "{}", null);
            if (challengeResponse.statusCode() != 200) {
                Client.LOG.warn("[Alumite] Challenge request failed with status {}", challengeResponse.statusCode());
                return;
            }
            ChallengeResponseDTO challenge = Constants.GSON.fromJson(challengeResponse.body(), ChallengeResponseDTO.class);
            VerifyRequestDTO verifyRequest = new VerifyRequestDTO(challenge.challengeId(), challenge.nonce(), minecraftAccessToken);
            var verifyResponse = http.sendRaw("POST", "/auth/minecraft/verify", Constants.GSON.toJson(verifyRequest), null);
            if (verifyResponse.statusCode() != 200) {
                Client.LOG.warn("[Alumite] Verify failed with status {}: {}", verifyResponse.statusCode(), verifyResponse.body());
                return;
            }
            VerifyResponseDTO result = Constants.GSON.fromJson(verifyResponse.body(), VerifyResponseDTO.class);
            activeAccessToken = result.accessToken();
            activeRefreshToken = result.refreshToken();
            updateSelfUser();
            storeSession(activeAccountKey, result.refreshToken(), result.accessToken(), result.accessExpiresAt());
            scheduleTokenRefresh(result.accessExpiresAt());
            Client.LOG.info("[Alumite] Authenticated as {}", users.selfUser().user().minecraftName());
        } catch (Exception exception) {
            Client.LOG.warn("[Alumite] Login failed: {}", exception.getMessage());
        }
    }

    private void scheduleTokenRefresh(String accessExpiresAt) {
        if (tokenRefreshTask != null) {
            tokenRefreshTask.cancel(false);
        }
        if (accessExpiresAt == null) {
            return;
        }
        try {
            long delayMs = Math.max(0L, Instant.parse(accessExpiresAt).toEpochMilli() - System.currentTimeMillis() - 60_000L);
            String accountKey = activeAccountKey;
            String capturedRefresh = activeRefreshToken;
            tokenRefreshTask = AlumiteMod.SCHEDULED_POOL.schedule(() -> {
                if (capturedRefresh != null && accountKey != null) {
                    Client.LOG.info("[Alumite] Refreshing access token...");
                    tryRefresh(accountKey, capturedRefresh);
                }
            }, delayMs, TimeUnit.MILLISECONDS);
        } catch (Exception exception) {
            Client.LOG.warn("[Alumite] Failed to schedule token refresh: {}", exception.getMessage());
        }
    }

    private void onGatewayAuthExpired() {
        Client.LOG.info("[Alumite] Gateway refresh token expired, re-authenticating...");
        String currentRefresh = activeRefreshToken;
        activeAccessToken = null;
        activeRefreshToken = null;
        AlumiteMod.SCHEDULED_POOL.execute(() -> {
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

    private boolean refreshActiveToken() {
        String refreshToken = activeRefreshToken;
        return refreshToken != null && tryRefresh(activeAccountKey, refreshToken);
    }

    private void storeSession(String accountKey, String newRefreshToken, String newAccessToken, String newAccessExpiresAt) {
        tokenStore.save(accountKey, newRefreshToken, newAccessToken, newAccessExpiresAt);
    }

    public PublicUserDTO fetchUser(String userId) throws AlumiteApiException {
        return http.getObject(ROUTE_USERS + "/" + userId, PublicUserDTO.class, "get user", "Failed to load user.");
    }

    public List<FriendEntryDTO> fetchFriends() throws AlumiteApiException {
        return http.getList(ROUTE_FRIENDS, FriendEntryDTO.class, "get friends", "Failed to load friends.");
    }

    public List<PendingFriendRequestDTO> fetchIncomingFriendRequests() throws AlumiteApiException {
        return http.getList(ROUTE_FRIENDS_REQUESTS_INCOMING, PendingFriendRequestDTO.class, "get incoming friend requests", "Failed to load incoming requests.");
    }

    public List<PendingFriendRequestDTO> fetchOutgoingFriendRequests() throws AlumiteApiException {
        return http.getList(ROUTE_FRIENDS_REQUESTS_OUTGOING, PendingFriendRequestDTO.class, "get outgoing friend requests", "Failed to load outgoing requests.");
    }

    public List<ChannelDetailDTO> fetchChannels() throws AlumiteApiException {
        return http.getList(ROUTE_CHANNELS, ChannelDetailDTO.class, "get channels", "Failed to load channels.");
    }

    public ChannelDetailDTO openDm(String recipientUserId) throws AlumiteApiException {
        return http.postObject(ROUTE_CHANNELS + "/dm", new OpenDmBodyDTO(recipientUserId), ChannelDetailDTO.class, "open dm", "Failed to open direct message.");
    }

    public void updatePreferredPresence(Presence presence) throws AlumiteApiException {
        if (presence == null) {
            throw new IllegalArgumentException("Preferred presence is required.");
        }
        if (presence == Presence.OFFLINE) {
            throw new IllegalArgumentException("Preferred presence cannot be offline.");
        }
        http.sendAuthorizedChecked("PATCH", ROUTE_PRESENCE, Constants.GSON.toJson(new UpdatePresenceBodyDTO(presence)), "update preferred presence", "Failed to update preferred presence.");
        users().selfUser().preferredPresence(presence);
    }

    public PendingFriendRequest sendFriendRequest(String targetUsername) throws AlumiteApiException {
        var response = http.sendAuthorizedChecked("POST", ROUTE_FRIENDS_REQUESTS, Constants.GSON.toJson(new SendFriendRequestBodyDTO(targetUsername)), "send friend request", "Failed to send request.");
        PendingFriendRequestDTO dto = Constants.GSON.fromJson(response.body(), PendingFriendRequestDTO.class);
        return users.addOutgoingFriendRequest(dto);
    }

    public boolean acceptFriendRequest(String requestId) throws AlumiteApiException {
        http.sendAuthorizedChecked("POST", ROUTE_FRIENDS_REQUESTS + "/" + requestId + "/accept", "{}", "accept friend request", "Failed to accept request.");
        return true;
    }

    public void declineFriendRequest(String requestId) throws AlumiteApiException {
        http.sendAuthorizedChecked("POST", ROUTE_FRIENDS_REQUESTS + "/" + requestId + "/decline", "{}", "decline friend request", "Failed to decline request.");
    }

    public void cancelFriendRequest(String requestId) throws AlumiteApiException {
        http.sendAuthorizedChecked("DELETE", ROUTE_FRIENDS_REQUESTS + "/" + requestId, null, "cancel friend request", "Failed to cancel request.");
    }

    public void removeFriend(String userId) throws AlumiteApiException {
        http.sendAuthorizedChecked("DELETE", ROUTE_FRIENDS + "/" + userId, null, "remove friend", "Failed to remove friend.");
    }

    @EventHandler
    private void alumite$onClientStarted(ClientStartedEvent event) {
        AlumiteMod.SCHEDULED_POOL.execute(() -> authenticate(event.minecraftClient()));
    }

    @EventHandler
    private void alumite$onClientStopping(ClientStoppingEvent event) {
        gateway.disconnect();
    }

    @EventHandler
    private void alumite$onAuthenticated(AlumiteAuthenticatedEvent event) {
        AlumiteMod.SCHEDULED_POOL.execute(() -> {
            Client.LOG.info("[Alumite] Fetching channels and social data...");
            try {
                channels.refreshFromNetwork();
                users.refreshFromNetwork();
                Client.LOG.info("[Alumite] Loaded {} channels", channels.all().size());
            } catch (AlumiteApiException exception) {
                users.clearSessionCaches();
                channels.clearSessionCaches();
                Client.LOG.warn("[Alumite] Failed to fetch social data: {}", exception.getDisplayText());
            }
        });
    }
}
