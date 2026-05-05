package cc.fascinated.fascinatedutils.api;

import cc.fascinated.fascinatedutils.Constants;
import cc.fascinated.fascinatedutils.FascinatedUtils;
import cc.fascinated.fascinatedutils.api.internal.AlumiteHttpClient;
import cc.fascinated.fascinatedutils.api.internal.AlumiteModelMapper;
import cc.fascinated.fascinatedutils.api.auth.json.ChallengeResponseWire;
import cc.fascinated.fascinatedutils.api.auth.json.RefreshRequestWire;
import cc.fascinated.fascinatedutils.api.auth.json.RefreshResponseWire;
import cc.fascinated.fascinatedutils.api.auth.json.VerifyRequestWire;
import cc.fascinated.fascinatedutils.api.auth.json.VerifyResponseWire;
import cc.fascinated.fascinatedutils.api.channel.AlumiteChannels;
import cc.fascinated.fascinatedutils.api.channel.json.ChannelSummaryWire;
import cc.fascinated.fascinatedutils.api.friend.PendingFriendRequest;
import cc.fascinated.fascinatedutils.api.friend.json.FriendEntryWire;
import cc.fascinated.fascinatedutils.api.friend.json.PendingFriendRequestWire;
import cc.fascinated.fascinatedutils.api.friend.json.SendFriendRequestBodyWire;
import cc.fascinated.fascinatedutils.api.user.AlumiteUsers;
import cc.fascinated.fascinatedutils.api.user.Presence;
import cc.fascinated.fascinatedutils.api.user.json.PublicUserWire;
import cc.fascinated.fascinatedutils.api.user.json.UpdatePresenceBodyWire;
import cc.fascinated.fascinatedutils.api.user.json.UserMeWire;
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
    private volatile Integer activeUserId;
    private volatile Presence activePreferredPresence = Presence.ONLINE;
    private volatile ScheduledFuture<?> tokenRefreshTask;

    private Alumite() {
        this.http = new AlumiteHttpClient(httpClient, Constants.GSON, () -> activeAccessToken, this::refreshActiveToken);
        this.gateway = new AlumiteGateway(httpClient, () -> activeRefreshToken, this::onGatewayAuthExpired, Constants.GSON);
        this.users = new AlumiteUsers(this, http);
        this.channels = new AlumiteChannels(this, http, users);
    }

    public Gson getGsonForWire() {
        return Constants.GSON;
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
            var response = http.sendRaw("POST", "/auth/refresh", Constants.GSON.toJson(new RefreshRequestWire(storedRefreshToken)), null);
            if (response.statusCode() != 200) {
                return false;
            }
            RefreshResponseWire refreshResponse = Constants.GSON.fromJson(response.body(), RefreshResponseWire.class);
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
        UserMeWire currentUser = http.getObject(ROUTE_USERS + "/@me", UserMeWire.class, "get current user", "Failed to load current user.");
        activeUserId = currentUser.id();
        activePreferredPresence = normalizePreferredPresence(currentUser.preferredPresence());
        users.setSelfUser(toPublicWire(currentUser));
    }

    private static PublicUserWire toPublicWire(UserMeWire user) {
        return new PublicUserWire(
                user.id(),
                user.minecraftUuid(),
                user.minecraftName(),
                user.role(),
                user.status(),
                user.presence(),
                user.lastSeen()
        );
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
            ChallengeResponseWire challenge = Constants.GSON.fromJson(challengeResponse.body(), ChallengeResponseWire.class);
            VerifyRequestWire verifyRequest = new VerifyRequestWire(challenge.challengeId(), challenge.nonce(), minecraftAccessToken);
            var verifyResponse = http.sendRaw("POST", "/auth/minecraft/verify", Constants.GSON.toJson(verifyRequest), null);
            if (verifyResponse.statusCode() != 200) {
                Client.LOG.warn("[Alumite] Verify failed with status {}: {}", verifyResponse.statusCode(), verifyResponse.body());
                return;
            }
            VerifyResponseWire result = Constants.GSON.fromJson(verifyResponse.body(), VerifyResponseWire.class);
            activeAccessToken = result.accessToken();
            activeRefreshToken = result.refreshToken();
            activeUserId = result.user().id();
            activePreferredPresence = normalizePreferredPresence(result.user().preferredPresence());
            users.setSelfUser(toPublicWire(result.user()));
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
            List<ChannelSummaryWire> channelWire = http.getList(ROUTE_CHANNELS, ChannelSummaryWire.class, "get channels", "Failed to load channels.");
            channels.replaceSummariesFromNetwork(channelWire);
            users.replaceSocialFromNetwork(
                    http.getList(ROUTE_FRIENDS, FriendEntryWire.class, "get friends", "Failed to load friends."),
                    http.getList(ROUTE_FRIENDS_REQUESTS_INCOMING, PendingFriendRequestWire.class, "get incoming friend requests", "Failed to load incoming requests."),
                    http.getList(ROUTE_FRIENDS_REQUESTS_OUTGOING, PendingFriendRequestWire.class, "get outgoing friend requests", "Failed to load outgoing requests.")
            );
            channels.preloadDetails();
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
        http.sendAuthorizedChecked("PATCH", ROUTE_PRESENCE, Constants.GSON.toJson(new UpdatePresenceBodyWire(resolvedPresence)), "update preferred presence", "Failed to update preferred presence.");
        activePreferredPresence = resolvedPresence;
    }

    public PendingFriendRequest sendFriendRequest(String targetUsername) throws AlumiteApiException {
        var response = http.sendAuthorizedChecked("POST", ROUTE_FRIENDS_REQUESTS, Constants.GSON.toJson(new SendFriendRequestBodyWire(targetUsername)), "send friend request", "Failed to send request.");
        PendingFriendRequestWire wire = Constants.GSON.fromJson(response.body(), PendingFriendRequestWire.class);
        return users.addOutgoingFriendRequest(wire);
    }

    public boolean acceptFriendRequest(int requestId) throws AlumiteApiException {
        http.sendAuthorizedChecked("POST", ROUTE_FRIENDS_REQUESTS + "/" + requestId + "/accept", "{}", "accept friend request", "Failed to accept request.");
        return true;
    }

    public void declineFriendRequest(int requestId) throws AlumiteApiException {
        http.sendAuthorizedChecked("POST", ROUTE_FRIENDS_REQUESTS + "/" + requestId + "/decline", "{}", "decline friend request", "Failed to decline request.");
    }

    public void cancelFriendRequest(int requestId) throws AlumiteApiException {
        http.sendAuthorizedChecked("DELETE", ROUTE_FRIENDS_REQUESTS + "/" + requestId, null, "cancel friend request", "Failed to cancel request.");
    }

    public void removeFriend(int userId) throws AlumiteApiException {
        http.sendAuthorizedChecked("DELETE", ROUTE_FRIENDS + "/" + userId, null, "remove friend", "Failed to remove friend.");
    }
}
