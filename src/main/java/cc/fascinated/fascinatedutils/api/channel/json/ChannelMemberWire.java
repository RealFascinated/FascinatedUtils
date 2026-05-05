package cc.fascinated.fascinatedutils.api.channel.json;

import cc.fascinated.fascinatedutils.api.user.json.PublicUserWire;

public record ChannelMemberWire(PublicUserWire user, boolean isOwner) {}
