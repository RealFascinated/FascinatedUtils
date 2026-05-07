package cc.fascinated.fascinatedutils.api.user;

import cc.fascinated.fascinatedutils.api.Alumite;
import cc.fascinated.fascinatedutils.api.AlumiteApiException;
import cc.fascinated.fascinatedutils.api.friend.PendingFriendRequest;
import cc.fascinated.fascinatedutils.api.friend.json.FriendEntryDTO;
import cc.fascinated.fascinatedutils.api.friend.json.PendingFriendRequestDTO;
import cc.fascinated.fascinatedutils.api.internal.AlumiteModelMapper;
import cc.fascinated.fascinatedutils.api.user.json.PublicUserDTO;
import cc.fascinated.fascinatedutils.api.user.json.UserDTO;
import cc.fascinated.fascinatedutils.event.FascinatedEventBus;
import cc.fascinated.fascinatedutils.event.impl.social.FriendAddEvent;
import cc.fascinated.fascinatedutils.event.impl.social.FriendRemoveEvent;
import cc.fascinated.fascinatedutils.event.impl.social.FriendRequestIncomingEvent;
import cc.fascinated.fascinatedutils.event.impl.social.FriendRequestRemovedEvent;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

@RequiredArgsConstructor
@Accessors(fluent = true)
public class AlumiteUsers {

    private final Alumite alumite;

    private final Map<String, User> usersById = new ConcurrentHashMap<>();

    private volatile List<String> friendIds = List.of();

    @Getter
    private volatile List<PendingFriendRequest> incomingFriendRequests = List.of();

    @Getter
    private volatile List<PendingFriendRequest> outgoingFriendRequests = List.of();

    @Getter
    private volatile SelfUser selfUser;

    public void clearSessionCaches() {
        usersById.clear();
        friendIds = List.of();
        incomingFriendRequests = List.of();
        outgoingFriendRequests = List.of();
        selfUser = null;
    }

    public void updateSelfUser() {
        UserDTO dto = alumite.getSelfUser();

        User user = AlumiteModelMapper.toUser(dto);
        selfUser = new SelfUser(alumite, user, dto.preferredUserStatus(), null);
        usersById.put(dto.id(), user);
    }

    public User getUser(String userId) {
        return usersById.get(userId);
    }

    public List<User> getFriends() {
        return friendIds.stream().map(usersById::get).filter(Objects::nonNull).toList();
    }

    public boolean isFriend(String userId) {
        return friendIds.contains(userId);
    }

    public User resolveUser(String userId) throws AlumiteApiException {
        User cached = getUser(userId);
        if (cached != null) {
            return cached;
        }
        return upsertUser(alumite.fetchUser(userId));
    }

    public void refreshFromNetwork() throws AlumiteApiException {
        replaceSocialFromNetwork(alumite.fetchFriends(), alumite.fetchIncomingFriendRequests(), alumite.fetchOutgoingFriendRequests());
    }

    public void replaceSocialFromNetwork(List<FriendEntryDTO> friendsDto, List<PendingFriendRequestDTO> incomingDto, List<PendingFriendRequestDTO> outgoingDto) {
        if (friendsDto != null) {
            friendsDto.forEach(entry -> upsertUser(entry.user()));
        }
        friendIds = friendsDto == null ? List.of() : friendsDto.stream().map(entry -> entry.user().id()).toList();
        incomingFriendRequests = incomingDto == null ? List.of() : incomingDto.stream().map(this::toPendingFriendRequest).toList();
        outgoingFriendRequests = outgoingDto == null ? List.of() : outgoingDto.stream().map(this::toPendingFriendRequest).toList();
    }

    public User upsertUser(PublicUserDTO dto) {
        User existing = usersById.get(dto.id());
        if (existing != null) {
            existing.update(dto.minecraftUuid(), dto.minecraftName(), dto.role(), dto.userStatus(), dto.activity(), dto.lastSeen());
            return existing;
        }
        User user = AlumiteModelMapper.toUser(dto);
        usersById.put(dto.id(), user);
        return user;
    }

    public User upsertUser(User user) {
        usersById.put(user.id(), user);
        return user;
    }

    public void onFriendAdd(FriendEntryDTO entry) {
        User user = upsertUser(entry.user());
        boolean wasOutgoing = outgoingFriendRequests.stream().anyMatch(request -> request.user().id().equals(user.id()));
        List<String> updatedFriendIds = new ArrayList<>(friendIds);
        updatedFriendIds.add(user.id());
        friendIds = List.copyOf(updatedFriendIds);
        outgoingFriendRequests = outgoingFriendRequests.stream().filter(request -> !request.user().id().equals(user.id())).toList();
        incomingFriendRequests = incomingFriendRequests.stream().filter(request -> !request.user().id().equals(user.id())).toList();
        FascinatedEventBus.INSTANCE.post(new FriendAddEvent(user, wasOutgoing));
    }

    public void onFriendRemove(String userId) {
        friendIds = friendIds.stream().filter(id -> !id.equals(userId)).toList();
        FascinatedEventBus.INSTANCE.post(new FriendRemoveEvent(userId));
    }

    public void onFriendRequestIncoming(PendingFriendRequestDTO request) {
        PendingFriendRequest pendingRequest = toPendingFriendRequest(request);
        List<PendingFriendRequest> updatedRequests = new ArrayList<>(incomingFriendRequests);
        updatedRequests.add(pendingRequest);
        incomingFriendRequests = List.copyOf(updatedRequests);
        FascinatedEventBus.INSTANCE.post(new FriendRequestIncomingEvent(request.requestId(), pendingRequest.user(), request.createdAt()));
    }

    public void onFriendRequestRemoved(String requestId, String reason) {
        incomingFriendRequests = incomingFriendRequests.stream().filter(request -> !request.requestId().equals(requestId)).toList();
        outgoingFriendRequests = outgoingFriendRequests.stream().filter(request -> !request.requestId().equals(requestId)).toList();
        FascinatedEventBus.INSTANCE.post(new FriendRequestRemovedEvent(requestId, reason));
    }

    public PendingFriendRequest addOutgoingFriendRequest(PendingFriendRequestDTO dto) {
        PendingFriendRequest pending = toPendingFriendRequest(dto);
        List<PendingFriendRequest> updated = new ArrayList<>(outgoingFriendRequests);
        updated.add(pending);
        outgoingFriendRequests = List.copyOf(updated);
        return pending;
    }

    private PendingFriendRequest toPendingFriendRequest(PendingFriendRequestDTO request) {
        return new PendingFriendRequest(request.requestId(), upsertUser(request.user()), request.createdAt());
    }
}
