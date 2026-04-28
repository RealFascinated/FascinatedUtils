package cc.fascinated.fascinatedutils.api.dto.friend;

public record PendingFriendRequestDto(
        int requestId,
        FriendUserDto user,
        String createdAt
) {}
