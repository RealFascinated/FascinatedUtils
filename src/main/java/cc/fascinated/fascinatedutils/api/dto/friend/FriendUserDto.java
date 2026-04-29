package cc.fascinated.fascinatedutils.api.dto.friend;

import cc.fascinated.fascinatedutils.api.dto.Presence;

public record FriendUserDto(
        int id,
        String minecraftUuid,
        String minecraftName,
        String role,
        Presence presence
) {}
