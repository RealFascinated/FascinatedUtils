package cc.fascinated.fascinatedutils.event.impl.social;

import cc.fascinated.fascinatedutils.api.dto.Presence;

/**
 * Fired when a friend's presence status changes.
 */
public record PresenceUpdateEvent(int userId, Presence status, String lastSeen) {}
