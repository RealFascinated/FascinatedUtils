package cc.fascinated.fascinatedutils.event.impl.social;

/**
 * Fired when an outgoing friend request is removed (either cancelled by sender or declined by receiver).
 *
 * <p>{@code reason} is one of {@code "cancelled"} or {@code "declined"}.
 */
public record FriendRequestRemovedEvent(int requestId, String reason) {}
