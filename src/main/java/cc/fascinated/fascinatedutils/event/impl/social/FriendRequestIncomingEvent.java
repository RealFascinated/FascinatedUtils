package cc.fascinated.fascinatedutils.event.impl.social;

import cc.fascinated.fascinatedutils.api.user.User;

public record FriendRequestIncomingEvent(int requestId, User user, String createdAt) {}
