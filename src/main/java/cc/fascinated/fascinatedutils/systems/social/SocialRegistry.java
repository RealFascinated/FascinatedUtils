package cc.fascinated.fascinatedutils.systems.social;

import cc.fascinated.fascinatedutils.FascinatedUtils;
import cc.fascinated.fascinatedutils.api.AlumiteApi;
import cc.fascinated.fascinatedutils.api.dto.friend.FriendEntryDto;
import cc.fascinated.fascinatedutils.api.dto.friend.PendingFriendRequestDto;
import cc.fascinated.fascinatedutils.client.Client;
import cc.fascinated.fascinatedutils.event.impl.lifecycle.AlumiteAuthenticatedEvent;
import cc.fascinated.fascinatedutils.event.impl.social.FriendAddEvent;
import cc.fascinated.fascinatedutils.event.impl.social.FriendRemoveEvent;
import cc.fascinated.fascinatedutils.event.impl.social.FriendRequestIncomingEvent;
import cc.fascinated.fascinatedutils.event.impl.social.FriendRequestRemovedEvent;
import cc.fascinated.fascinatedutils.event.impl.social.PresenceUpdateEvent;
import lombok.Getter;
import meteordevelopment.orbit.EventHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Getter
public class SocialRegistry {

    public static final SocialRegistry INSTANCE = new SocialRegistry();

    private volatile List<FriendEntryDto> friends = List.of();
    private volatile List<PendingFriendRequestDto> incomingFriendRequests = List.of();
    private volatile List<PendingFriendRequestDto> outgoingFriendRequests = List.of();
    private final Map<Integer, PresenceUpdateEvent> presenceStatuses = new ConcurrentHashMap<>();

    @EventHandler
    private void fascinatedutils$onAuthenticated(AlumiteAuthenticatedEvent event) {
        FascinatedUtils.SCHEDULED_POOL.execute(this::fetchAll);
    }

    @EventHandler
    private void fascinatedutils$onFriendAdd(FriendAddEvent event) {
        List<FriendEntryDto> updated = new ArrayList<>(friends);
        updated.add(event.entry());
        friends = List.copyOf(updated);
        outgoingFriendRequests = outgoingFriendRequests.stream()
                .filter(req -> req.user().id() != event.entry().user().id())
                .toList();
    }

    @EventHandler
    private void fascinatedutils$onFriendRemove(FriendRemoveEvent event) {
        friends = friends.stream()
                .filter(entry -> entry.user().id() != event.userId())
                .toList();
        presenceStatuses.remove(event.userId());
    }

    @EventHandler
    private void fascinatedutils$onFriendRequestIncoming(FriendRequestIncomingEvent event) {
        List<PendingFriendRequestDto> updated = new ArrayList<>(incomingFriendRequests);
        updated.add(event.request());
        incomingFriendRequests = List.copyOf(updated);
    }

    @EventHandler
    private void fascinatedutils$onFriendRequestRemoved(FriendRequestRemovedEvent event) {
        incomingFriendRequests = incomingFriendRequests.stream()
                .filter(req -> req.requestId() != event.requestId())
                .toList();
        outgoingFriendRequests = outgoingFriendRequests.stream()
                .filter(req -> req.requestId() != event.requestId())
                .toList();
    }

    public void addOutgoingFriendRequest(PendingFriendRequestDto dto) {
        List<PendingFriendRequestDto> updated = new ArrayList<>(outgoingFriendRequests);
        updated.add(dto);
        outgoingFriendRequests = List.copyOf(updated);
    }

    @EventHandler
    private void fascinatedutils$onPresenceUpdate(PresenceUpdateEvent event) {
        presenceStatuses.put(event.userId(), event);
    }

    private void fetchAll() {
        Client.LOG.info("[SocialRegistry] Fetching social data...");
        friends = AlumiteApi.INSTANCE.getFriends();
        incomingFriendRequests = AlumiteApi.INSTANCE.getIncomingFriendRequests();
        outgoingFriendRequests = AlumiteApi.INSTANCE.getOutgoingFriendRequests();
        Client.LOG.info("[SocialRegistry] Loaded {} friends, {} incoming requests, {} outgoing requests",
                friends.size(), incomingFriendRequests.size(), outgoingFriendRequests.size());
    }
}
