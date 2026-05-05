package cc.fascinated.fascinatedutils.api;

import cc.fascinated.fascinatedutils.Constants;
import cc.fascinated.fascinatedutils.FascinatedUtils;
import cc.fascinated.fascinatedutils.api.auth.json.*;
import cc.fascinated.fascinatedutils.api.channel.AlumiteChannels;
import cc.fascinated.fascinatedutils.api.channel.json.ChannelDetailDTO;
import cc.fascinated.fascinatedutils.api.friend.PendingFriendRequest;
import cc.fascinated.fascinatedutils.api.friend.json.FriendEntryDTO;
import cc.fascinated.fascinatedutils.api.friend.json.PendingFriendRequestDTO;
import cc.fascinated.fascinatedutils.api.friend.json.SendFriendRequestBodyDTO;
import cc.fascinated.fascinatedutils.api.internal.AlumiteHttpClient;
import cc.fascinated.fascinatedutils.api.user.AlumiteUsers;
import cc.fascinated.fascinatedutils.api.user.Presence;
import cc.fascinated.fascinatedutils.api.user.json.PublicUserDTO;
import cc.fascinated.fascinatedutils.api.user.json.UpdatePresenceBodyDTO;
import cc.fascinated.fascinatedutils.api.user.json.UserMeDTO;
import cc.fascinated.fascinatedutils.client.Client;
import cc.fascinated.fascinatedutils.event.FascinatedEventBus;
import cc.fascinated.fascinatedutils.event.impl.lifecycle.AlumiteAuthenticatedEvent;
import cc.fascinated.fascinatedutils.event.impl.lifecycle.ClientStartedEvent;
import cc.fascinated.fascinatedutils.event.impl.lifecycle.ClientStoppingEvent;
import com.google.gson.Gson;
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
    @Getter
    private volatile String activeUserId;
    private volatile Presence activePreferredPresence = Presence.ONLINE;
    private volatile ScheduledFuture<?> tokenRefreshTask;

    private Alumite() {
        this.http = new AlumiteHttpClient(httpClient, Constants.GSON, () -> activeAccessToken, this::refreshActiveToken);
        this.gateway = new AlumiteGateway(httpClient, () -> activeRefreshToken, this::onGatewayAuthExpired, Constants.GSON);
        this.users = new AlumiteUsers(this, http);
        this.channels = new AlumiteChannels(this, http, users);
    }

    private static Presence normalizePreferredPresence(Presence presence) {
        if (presence == null || presence == Presence.OFFLINE) {
            return Presence.ONLINE;
        }
        return presence;
    }

    private static PublicUserDTO toPublicDTO(UserMeDTO user) {
        return new PublicUserDTO(user.id(), user.minecraftUuid(), user.minecraftName(), user.role(), user.status(), user.presence(), user.lastSeen());
    }

    public Gson getGsonForDTO() {
        return Constants.GSON;
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
            activeUserId = null;
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
            var response = http.sendRaw("POST", "/auth/refresh", Constants.GSON.toJson(new RefreshRequestDTO(storedRefreshToken)), null);
            if (response.statusCode() != 200) {
                return false;
            }
            RefreshResponseDTO refreshResponse = Constants.GSON.fromJson(response.body(), RefreshResponseDTO.class);
            activeAccessToken = refreshResponse.accessToken();
            activeRefreshToken = refreshResponse.refreshToken();
            restoreActiveUserContext();
            storeSession(accountKey, refreshResponse.refreshToken());
            scheduleTokenRefresh(refreshResponse.accessExpiresAt());
            Client.LOG.info("[Alumite] Session refreshed.");
            return true;
        } catch (Exception exception) {
            activeAccessToken = null;
            activeRefreshToken = null;
            activeUserId = null;
            activePreferredPresence = Presence.ONLINE;
            Client.LOG.warn("[Alumite] Refresh failed: {}", exception.getMessage());
            return false;
        }
    }

    private void restoreActiveUserContext() {
        UserMeDTO currentUser = http.getObject(ROUTE_USERS + "/@me", UserMeDTO.class, "get current user", "Failed to load current user.");
        activeUserId = currentUser.id();
        activePreferredPresence = normalizePreferredPresence(currentUser.preferredPresence());
        users.setSelfUser(toPublicDTO(currentUser));
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
            activeUserId = result.user().id();
            activePreferredPresence = normalizePreferredPresence(result.user().preferredPresence());
            users.setSelfUser(toPublicDTO(result.user()));
            storeSession(activeAccountKey, result.refreshToken());
            scheduleTokenRefresh(result.accessExpiresAt());
            Client.LOG.info("[Alumite] Authenticated as {}", result.user().minecraftName());
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
            tokenRefreshTask = FascinatedUtils.SCHEDULED_POOL.schedule(() -> {
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
        activeUserId = null;
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

    private boolean refreshActiveToken() {
        String refreshToken = activeRefreshToken;
        return refreshToken != null && tryRefresh(activeAccountKey, refreshToken);
    }

    private void storeSession(String accountKey, String newRefreshToken) {
        tokenStore.save(accountKey, newRefreshToken);
    }

    @EventHandler
    private void fascinatedutils$onAuthenticated(AlumiteAuthenticatedEvent event) {
        FascinatedUtils.SCHEDULED_POOL.execute(this::refreshSocialCaches);
    }

    private void refreshSocialCaches() {
        Client.LOG.info("[Alumite] Fetching channels and social data...");
        try {
            List<ChannelDetailDTO> channelDto = http.getList(ROUTE_CHANNELS, ChannelDetailDTO.class, "get channels", "Failed to load channels.");
            channels.replaceChannelsFromNetwork(channelDto);
            users.replaceSocialFromNetwork(http.getList(ROUTE_FRIENDS, FriendEntryDTO.class, "get friends", "Failed to load friends."), http.getList(ROUTE_FRIENDS_REQUESTS_INCOMING, PendingFriendRequestDTO.class, "get incoming friend requests", "Failed to load incoming requests."), http.getList(ROUTE_FRIENDS_REQUESTS_OUTGOING, PendingFriendRequestDTO.class, "get outgoing friend requests", "Failed to load outgoing requests."));
            Client.LOG.info("[Alumite] Loaded {} channels", channels.all().size());
        } catch (AlumiteApiException exception) {
            users.clearSessionCaches();
            channels.clearSessionCaches();
            Client.LOG.warn("[Alumite] Failed to fetch social data: {}", exception.getDisplayText());
        }
    }

    public Presence currentPreferredPresence() {
        return normalizePreferredPresence(activePreferredPresence);
    }

    public void updatePreferredPresence(Presence presence) throws AlumiteApiException {
        Presence resolvedPresence = normalizePreferredPresence(presence);
        if (presence == null) {
            throw new IllegalArgumentException("Preferred presence is required.");
        }
        if (presence == Presence.OFFLINE) {
            throw new IllegalArgumentException("Preferred presence cannot be offline.");
        }
        http.sendAuthorizedChecked("PATCH", ROUTE_PRESENCE, Constants.GSON.toJson(new UpdatePresenceBodyDTO(resolvedPresence)), "update preferred presence", "Failed to update preferred presence.");
        activePreferredPresence = resolvedPresence;
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
}
