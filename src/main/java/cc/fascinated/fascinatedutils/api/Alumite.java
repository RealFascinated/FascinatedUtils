package cc.fascinated.fascinatedutils.api;

import cc.fascinated.fascinatedutils.AlumiteMod;
import cc.fascinated.fascinatedutils.Constants;
import cc.fascinated.fascinatedutils.api.auth.json.*;
import cc.fascinated.fascinatedutils.api.channel.AlumiteChannels;
import cc.fascinated.fascinatedutils.api.channel.json.*;
import cc.fascinated.fascinatedutils.api.friend.PendingFriendRequest;
import cc.fascinated.fascinatedutils.api.friend.json.FriendEntryDTO;
import cc.fascinated.fascinatedutils.api.friend.json.PendingFriendRequestDTO;
import cc.fascinated.fascinatedutils.api.friend.json.SendFriendRequestBodyDTO;
import cc.fascinated.fascinatedutils.api.internal.AlumiteHttpClient;
import cc.fascinated.fascinatedutils.api.user.Activity;
import cc.fascinated.fascinatedutils.api.user.AlumiteUsers;
import cc.fascinated.fascinatedutils.api.user.UserStatus;
import cc.fascinated.fascinatedutils.api.user.json.PublicUserDTO;
import cc.fascinated.fascinatedutils.api.user.json.UpdateActivityBodyDTO;
import cc.fascinated.fascinatedutils.api.user.json.UpdateUserStatusBodyDTO;
import cc.fascinated.fascinatedutils.api.user.json.UserDTO;
import cc.fascinated.fascinatedutils.client.Client;
import cc.fascinated.fascinatedutils.common.UrlUtils;
import cc.fascinated.fascinatedutils.event.impl.lifecycle.AlumiteAuthenticatedEvent;
import cc.fascinated.fascinatedutils.event.impl.lifecycle.ClientStartedEvent;
import cc.fascinated.fascinatedutils.event.impl.lifecycle.ClientStoppingEvent;
import lombok.Getter;
import lombok.experimental.Accessors;
import meteordevelopment.orbit.EventHandler;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;
import java.util.Map;

@Accessors(fluent = true)
public class Alumite {

    public static Alumite INSTANCE;

    private interface Routes {
        String FRIENDS = "/friends";
        String FRIENDS_REQUESTS = "/friends/requests";
        String FRIENDS_REQUESTS_INCOMING = "/friends/requests/incoming";
        String FRIENDS_REQUESTS_OUTGOING = "/friends/requests/outgoing";
        String USER_STATUS = "/users/status";
        String ACTIVITY = "/users/activity";
        String USERS = "/users";
        String CHANNELS = "/channels";
    }

    private final HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    @Getter
    private final AlumiteHttpClient http;
    private final AlumiteGateway gateway;
    private final AlumiteAuthManager authManager;
    @Getter
    private final AlumiteUsers users;
    @Getter
    private final AlumiteChannels channels;

    public Alumite() {
        INSTANCE = this;
        AlumiteAuthManager auth = new AlumiteAuthManager(new AlumiteTokenStore());
        this.http = new AlumiteHttpClient(httpClient, Constants.GSON, auth::accessToken, auth::tryRefreshActive);
        this.gateway = new AlumiteGateway(httpClient, auth::refreshToken, auth::onGatewayAuthExpired, Constants.GSON);
        this.users = new AlumiteUsers(this);
        this.channels = new AlumiteChannels(this, users);
        auth.init(this, gateway);
        this.authManager = auth;
    }

    public PublicUserDTO fetchUser(String userId) throws AlumiteApiException {
        return http.get(Routes.USERS + "/" + userId).execute(PublicUserDTO.class);
    }

    public List<FriendEntryDTO> fetchFriends() throws AlumiteApiException {
        return http.get(Routes.FRIENDS).executeList(FriendEntryDTO.class);
    }

    public List<PendingFriendRequestDTO> fetchIncomingFriendRequests() throws AlumiteApiException {
        return http.get(Routes.FRIENDS_REQUESTS_INCOMING).executeList(PendingFriendRequestDTO.class);
    }

    public List<PendingFriendRequestDTO> fetchOutgoingFriendRequests() throws AlumiteApiException {
        return http.get(Routes.FRIENDS_REQUESTS_OUTGOING).executeList(PendingFriendRequestDTO.class);
    }

    public List<ChannelDetailDTO> fetchChannels() throws AlumiteApiException {
        return http.get(Routes.CHANNELS).executeList(ChannelDetailDTO.class);
    }

    public ChannelDetailDTO openDm(String recipientUserId) throws AlumiteApiException {
        return http.post(Routes.CHANNELS + "/dm").body(new OpenDmBodyDTO(recipientUserId)).execute(ChannelDetailDTO.class);
    }

    public void sendUpdateUserStatus(UserStatus userStatus) throws AlumiteApiException {
        http.patch(Routes.USER_STATUS).body(new UpdateUserStatusBodyDTO(userStatus)).executeVoid();
    }

    public void sendUpdateActivity(Activity activity) throws AlumiteApiException {
        http.patch(Routes.ACTIVITY).body(new UpdateActivityBodyDTO(activity)).executeVoid();
    }

    public PendingFriendRequest sendFriendRequest(String targetUsername) throws AlumiteApiException {
        PendingFriendRequestDTO dto = http.post(Routes.FRIENDS_REQUESTS).body(new SendFriendRequestBodyDTO(targetUsername)).execute(PendingFriendRequestDTO.class);
        return users.addOutgoingFriendRequest(dto);
    }

    public boolean acceptFriendRequest(String requestId) throws AlumiteApiException {
        http.post(Routes.FRIENDS_REQUESTS + "/" + requestId + "/accept").rawBody("{}").executeVoid();
        return true;
    }

    public void declineFriendRequest(String requestId) throws AlumiteApiException {
        http.post(Routes.FRIENDS_REQUESTS + "/" + requestId + "/decline").rawBody("{}").executeVoid();
    }

    public void cancelFriendRequest(String requestId) throws AlumiteApiException {
        http.delete(Routes.FRIENDS_REQUESTS + "/" + requestId).executeVoid();
    }

    public void removeFriend(String userId) throws AlumiteApiException {
        http.delete(Routes.FRIENDS + "/" + userId).executeVoid();
    }

    public RefreshResponseDTO refreshTokens(String refreshToken) {
        return http.post("/auth/refresh").unauthenticated().body(new RefreshRequestDTO(refreshToken)).execute(RefreshResponseDTO.class);
    }

    public ChallengeResponseDTO requestChallenge() {
        return http.post("/auth/challenge").unauthenticated().rawBody("{}").execute(ChallengeResponseDTO.class);
    }

    public VerifyResponseDTO verifyMinecraft(VerifyRequestDTO request) {
        return http.post("/auth/minecraft/verify").unauthenticated().body(request).execute(VerifyResponseDTO.class);
    }

    public UserDTO getSelfUser() {
        return http.get("/users/@me").execute(UserDTO.class);
    }

    public ChannelMessageDTO sendChannelMessage(String channelId, SendChannelMessageBodyDTO body) {
        return http.post(Routes.CHANNELS + "/" + channelId + "/messages").body(body).execute(ChannelMessageDTO.class);
    }

    public AttachmentDTO uploadChannelAttachment(String channelId, byte[] data, String filename) {
        AttachmentDTO attachment = http.postMultipart(Routes.CHANNELS + "/" + channelId + "/attachments", data, filename, AttachmentDTO.class);
        if (attachment == null) {
            throw new AlumiteApiException(null, "Upload failed: server returned no attachment");
        }
        return attachment;
    }

    public ChannelMessageDTO editChannelMessage(String channelId, String messageId, EditChannelMessageBodyDTO body) {
        return http.patch(Routes.CHANNELS + "/" + channelId + "/messages/" + messageId).body(body).execute(ChannelMessageDTO.class);
    }

    public void deleteChannelMessage(String channelId, String messageId) {
        http.delete(Routes.CHANNELS + "/" + channelId + "/messages/" + messageId).executeVoid();
    }

    public void markChannelRead(String channelId, String lastReadMessageId) {
        http.patch(Routes.CHANNELS + "/" + channelId + "/read").body(new UpdateReadStateBodyDTO(lastReadMessageId)).executeVoid();
    }

    public ChannelMessagePageDTO getChannelMessages(String channelId, Map<String, Object> queryParameters) {
        return http.get(UrlUtils.buildUrl(Routes.CHANNELS + "/" + channelId + "/messages", queryParameters)).execute(ChannelMessagePageDTO.class);
    }

    public void hideDmChannel(String channelId) {
        http.delete(Routes.CHANNELS + "/" + channelId + "/hidden").executeVoid();
    }

    @EventHandler
    private void alumite$onClientStarted(ClientStartedEvent event) {
        // Block until authentication is done, to avoid racing ahead and trying to fetch data before we're authenticated
        authManager.authenticate(event.minecraftClient());
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
