package cc.fascinated.fascinatedutils.api.dto.friend;

public record FriendEntryDto(
        int friendshipId,
        FriendUserDto user,
        String since
) {}
