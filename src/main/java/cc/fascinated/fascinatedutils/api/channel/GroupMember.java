package cc.fascinated.fascinatedutils.api.channel;

import cc.fascinated.fascinatedutils.api.user.User;

public record GroupMember(User user, boolean owner) {}
