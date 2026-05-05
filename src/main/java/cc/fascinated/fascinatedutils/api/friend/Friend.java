package cc.fascinated.fascinatedutils.api.friend;

import cc.fascinated.fascinatedutils.api.user.User;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;

@Accessors(fluent = true)
public record Friend(User user, String since) {

}
