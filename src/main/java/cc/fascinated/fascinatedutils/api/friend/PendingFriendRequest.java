package cc.fascinated.fascinatedutils.api.friend;

import cc.fascinated.fascinatedutils.api.user.User;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;

@Getter
@RequiredArgsConstructor
@Accessors(fluent = true)
public class PendingFriendRequest {

    private final int requestId;
    private final User user;
    private final String createdAt;
}
