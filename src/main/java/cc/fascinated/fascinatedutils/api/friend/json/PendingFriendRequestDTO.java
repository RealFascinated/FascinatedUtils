package cc.fascinated.fascinatedutils.api.friend.json;

import cc.fascinated.fascinatedutils.api.user.json.PublicUserDTO;

public record PendingFriendRequestDTO(int requestId, PublicUserDTO user, String createdAt) {}
