package cc.fascinated.fascinatedutils.event.impl.social;

import cc.fascinated.fascinatedutils.api.user.User;

public record FriendRequestIncomingEvent(String requestId, User user, String createdAt) {}
