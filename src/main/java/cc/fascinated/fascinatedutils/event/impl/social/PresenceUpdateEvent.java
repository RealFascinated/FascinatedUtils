package cc.fascinated.fascinatedutils.event.impl.social;

import cc.fascinated.fascinatedutils.api.user.Presence;

/**
 * Fired when a friend's presence status changes.
 */
public record PresenceUpdateEvent(String userId, Presence status) {}
