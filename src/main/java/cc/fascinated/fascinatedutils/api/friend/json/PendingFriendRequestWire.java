package cc.fascinated.fascinatedutils.api.friend.json;

import cc.fascinated.fascinatedutils.api.user.json.PublicUserWire;

public record PendingFriendRequestWire(int requestId, PublicUserWire user, String createdAt) {
}
