package cc.fascinated.fascinatedutils.api.user.json;

import cc.fascinated.fascinatedutils.api.user.Activity;
import cc.fascinated.fascinatedutils.api.user.UserStatus;
import lombok.Getter;
import lombok.experimental.Accessors;
import org.jspecify.annotations.Nullable;

import java.util.Date;

@Getter
@Accessors(fluent = true)
public class PublicUserDTO {
    private String id;
    private String minecraftUuid;
    private String minecraftName;
    private String role;
    private boolean banned;
    private UserStatus userStatus;
    @Nullable private Activity activity;
    private Date lastSeen;
}
