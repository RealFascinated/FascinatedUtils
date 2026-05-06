package cc.fascinated.fascinatedutils.api.user;

import lombok.Getter;
import lombok.experimental.Accessors;
import org.jspecify.annotations.Nullable;

import java.util.Date;

@Getter
@Accessors(fluent = true)
public class User {

    private final String id;
    private volatile String minecraftUuid;
    private volatile String minecraftName;
    private volatile String role;
    private volatile boolean banned;
    private volatile UserStatus userStatus;
    @Nullable private volatile Activity activity;
    private volatile Date lastSeen;

    public User(String id) {
        this.id = id;
    }

    public User(String id, String minecraftUuid, String minecraftName, String role, boolean banned, UserStatus userStatus, @Nullable Activity activity, Date lastSeen) {
        this.id = id;
        this.minecraftUuid = minecraftUuid;
        this.minecraftName = minecraftName;
        this.role = role;
        this.banned = banned;
        this.userStatus = userStatus;
        this.activity = activity;
        this.lastSeen = lastSeen;
    }
}
