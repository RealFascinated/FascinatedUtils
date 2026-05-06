package cc.fascinated.fascinatedutils.api.user;

import lombok.Getter;
import lombok.experimental.Accessors;

import java.util.Date;

@Getter
@Accessors(fluent = true)
public class User {

    private final String id;
    private volatile String minecraftUuid;
    private volatile String minecraftName;
    private volatile String role;
    private volatile boolean banned;
    private volatile Presence presence;
    private volatile Date lastSeen;
    private volatile boolean resolved;

    public User(String id) {
        this.id = id;
    }

    public User(String id, String minecraftUuid, String minecraftName, String role, boolean banned, Presence presence, Date lastSeen) {
        this.id = id;
        this.minecraftUuid = minecraftUuid;
        this.minecraftName = minecraftName;
        this.role = role;
        this.banned = banned;
        this.presence = presence;
        this.lastSeen = lastSeen;
        this.resolved = true;
    }

    private static String mergeString(String existingValue, String incomingValue) {
        if (incomingValue == null || incomingValue.isBlank()) {
            return existingValue;
        }
        return incomingValue;
    }

    void mergeFrom(User incomingUser) {
        if (incomingUser == null) {
            return;
        }
        if (!incomingUser.id().equals(id)) {
            throw new IllegalArgumentException("Cannot merge user data with a different id.");
        }
        minecraftUuid = mergeString(minecraftUuid, incomingUser.minecraftUuid());
        minecraftName = mergeString(minecraftName, incomingUser.minecraftName());
        role = mergeString(role, incomingUser.role());
        if (incomingUser.resolved()) {
            banned = incomingUser.banned();
        }
        if (incomingUser.presence() != null) {
            presence = incomingUser.presence();
        }
        if (incomingUser.lastSeen() != null) {
            lastSeen = incomingUser.lastSeen();
        }
        resolved = resolved || incomingUser.resolved();
    }

    void setPresence(Presence presence) {
        this.presence = presence;
    }
}
