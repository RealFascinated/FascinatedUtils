package cc.fascinated.fascinatedutils.event.impl.social;

import cc.fascinated.fascinatedutils.api.dto.friend.PendingFriendRequestDto;

/**
 * Fired when a new incoming friend request arrives over the gateway.
 */
public record FriendRequestIncomingEvent(PendingFriendRequestDto request) {}
