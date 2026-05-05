package cc.fascinated.fascinatedutils.api.user;

import cc.fascinated.fascinatedutils.api.Alumite;
import cc.fascinated.fascinatedutils.api.AlumiteApiException;
import cc.fascinated.fascinatedutils.api.friend.Friend;
import cc.fascinated.fascinatedutils.api.friend.PendingFriendRequest;
import cc.fascinated.fascinatedutils.api.friend.json.FriendEntryWire;
import cc.fascinated.fascinatedutils.api.friend.json.PendingFriendRequestWire;
import cc.fascinated.fascinatedutils.api.internal.AlumiteHttpClient;
import cc.fascinated.fascinatedutils.api.internal.AlumiteModelMapper;
import cc.fascinated.fascinatedutils.api.user.json.PublicUserWire;
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
import java.util.concurrent.ConcurrentHashMap;

@RequiredArgsConstructor
@Accessors(fluent = true)
public class AlumiteUsers {

    private static final String ROUTE_USERS = "/users";

    private final Alumite alumite;
    private final AlumiteHttpClient http;

    private final Map<Integer, User> usersById = new ConcurrentHashMap<>();

    @Getter
    private volatile List<Friend> friends = List.of();

    @Getter
    private volatile List<PendingFriendRequest> incomingFriendRequests = List.of();

    @Getter
    private volatile List<PendingFriendRequest> outgoingFriendRequests = List.of();

    @Getter
    private volatile User selfUser;

    public void clearSessionCaches() {
        usersById.clear();
        friends = List.of();
        incomingFriendRequests = List.of();
        outgoingFriendRequests = List.of();
        selfUser = null;
    }

    public void setSelfUser(PublicUserWire wire) {
        selfUser = wire == null ? null : upsertUser(wire);
    }

    public User cachedUser(int userId) {
        return usersById.get(userId);
    }

    public User user(int userId) {
        return usersById.computeIfAbsent(userId, User::new);
    }

    public User resolveUser(int userId) throws AlumiteApiException {
        User cached = cachedUser(userId);
        if (cached != null && cached.resolved()) {
            return cached;
        }
        return fetchUser(userId);
    }

    private User fetchUser(int userId) throws AlumiteApiException {
        PublicUserWire wire = http.getObject(ROUTE_USERS + "/" + userId, PublicUserWire.class, "get user", "Failed to load user.");
        return upsertUser(wire);
    }

    public SelfUser self() {
        return new SelfUser(alumite);
    }

    public void replaceSocialFromNetwork(List<FriendEntryWire> friendsWire, List<PendingFriendRequestWire> incomingWire, List<PendingFriendRequestWire> outgoingWire) {
        User activeSelfUser = selfUser;
        usersById.clear();
        if (activeSelfUser != null) {
            usersById.put(activeSelfUser.id(), activeSelfUser);
        }
        friends = friendsWire == null ? List.of() : friendsWire.stream().map(this::toFriend).toList();
        incomingFriendRequests = incomingWire == null ? List.of() : incomingWire.stream().map(this::toPendingFriendRequest).toList();
        outgoingFriendRequests = outgoingWire == null ? List.of() : outgoingWire.stream().map(this::toPendingFriendRequest).toList();
    }

    public User upsertUser(PublicUserWire wire) {
        return upsertUser(AlumiteModelMapper.toUser(wire));
    }

    public User upsertUser(User incomingUser) {
        if (incomingUser == null) {
            return null;
        }
        return usersById.compute(incomingUser.id(), (_, existingUser) -> {
            if (existingUser == null) {
                return incomingUser;
            }
            existingUser.mergeFrom(incomingUser);
            return existingUser;
        });
    }

    public void mergePresenceUpdate(int userId, Presence status) {
        usersById.computeIfPresent(userId, (_, existingUser) -> {
            existingUser.setPresence(status);
            return existingUser;
        });
    }

    public void onFriendAdd(FriendEntryWire entry) {
        User user = upsertUser(entry.user());
        boolean wasOutgoing = outgoingFriendRequests.stream().anyMatch(request -> request.user().id() == user.id());
        List<Friend> updatedFriends = new ArrayList<>(friends);
        updatedFriends.add(new Friend(user, entry.since()));
        friends = List.copyOf(updatedFriends);
        outgoingFriendRequests = outgoingFriendRequests.stream().filter(request -> request.user().id() != user.id()).toList();
        incomingFriendRequests = incomingFriendRequests.stream().filter(request -> request.user().id() != user.id()).toList();
        FascinatedEventBus.INSTANCE.post(new FriendAddEvent(user, entry.since(), wasOutgoing));
    }

    public void onFriendRemove(int userId) {
        friends = friends.stream().filter(entry -> entry.user().id() != userId).toList();
        FascinatedEventBus.INSTANCE.post(new FriendRemoveEvent(userId));
    }

    public void onFriendRequestIncoming(PendingFriendRequestWire request) {
        PendingFriendRequest pendingRequest = toPendingFriendRequest(request);
        List<PendingFriendRequest> updatedRequests = new ArrayList<>(incomingFriendRequests);
        updatedRequests.add(pendingRequest);
        incomingFriendRequests = List.copyOf(updatedRequests);
        FascinatedEventBus.INSTANCE.post(new FriendRequestIncomingEvent(request.requestId(), pendingRequest.user(), request.createdAt()));
    }

    public void onFriendRequestRemoved(int requestId, String reason) {
        incomingFriendRequests = incomingFriendRequests.stream().filter(request -> request.requestId() != requestId).toList();
        outgoingFriendRequests = outgoingFriendRequests.stream().filter(request -> request.requestId() != requestId).toList();
        FascinatedEventBus.INSTANCE.post(new FriendRequestRemovedEvent(requestId, reason));
    }

    public PendingFriendRequest addOutgoingFriendRequest(PendingFriendRequestWire wire) {
        PendingFriendRequest pending = toPendingFriendRequest(wire);
        List<PendingFriendRequest> updated = new ArrayList<>(outgoingFriendRequests);
        updated.add(pending);
        outgoingFriendRequests = List.copyOf(updated);
        return pending;
    }

    public String previewAuthorName(int authorId) {
        User user = cachedUser(authorId);
        if (user != null && user.minecraftName() != null && !user.minecraftName().isBlank()) {
            return user.minecraftName();
        }
        return "";
    }

    private Friend toFriend(FriendEntryWire entry) {
        return new Friend(upsertUser(entry.user()), entry.since());
    }

    private PendingFriendRequest toPendingFriendRequest(PendingFriendRequestWire request) {
        return new PendingFriendRequest(request.requestId(), upsertUser(request.user()), request.createdAt());
    }
}
