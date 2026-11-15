package cc.fascinated.fascinatedutils.api.user.json;

import lombok.Getter;
import lombok.experimental.Accessors;

@Getter
@Accessors(fluent = true)
public class UserDTO extends PublicUserDTO {
    private boolean banned;
}
