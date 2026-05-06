package cc.fascinated.fascinatedutils.api.friend;

import cc.fascinated.fascinatedutils.api.user.User;
import lombok.experimental.Accessors;

@Accessors(fluent = true)
public record Friend(User user, String since) {

}
