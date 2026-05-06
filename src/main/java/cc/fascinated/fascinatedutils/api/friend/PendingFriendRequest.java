package cc.fascinated.fascinatedutils.api.friend;

import cc.fascinated.fascinatedutils.api.user.User;
import lombok.experimental.Accessors;

@Accessors(fluent = true)
public record PendingFriendRequest(String requestId, User user, String createdAt) {

}
