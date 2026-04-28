package cc.fascinated.fascinatedutils.event.impl.social;

/**
 * Fired when a friend's presence status changes.
 *
 * <p>{@code status} is one of {@code "online"}, {@code "offline"}, {@code "away"}, or
 * {@code "invisible"}. {@code lastSeen} is an ISO-8601 timestamp string and may be
 * {@code null} when the user is currently online.
 */
public record PresenceUpdateEvent(int userId, String status, String lastSeen) {}
