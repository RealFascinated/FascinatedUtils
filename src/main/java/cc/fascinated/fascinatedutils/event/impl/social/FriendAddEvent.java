package cc.fascinated.fascinatedutils.event.impl.social;

import cc.fascinated.fascinatedutils.api.user.User;

public record FriendAddEvent(User user, String since, boolean fromOutgoingRequest) {}
