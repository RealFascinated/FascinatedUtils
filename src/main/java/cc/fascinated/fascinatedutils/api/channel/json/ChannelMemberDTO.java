package cc.fascinated.fascinatedutils.api.channel.json;

import cc.fascinated.fascinatedutils.api.user.json.PublicUserDTO;

public record ChannelMemberDTO(PublicUserDTO user, boolean isOwner) {}
