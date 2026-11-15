package cc.fascinated.fascinatedutils.api.user.json;

import lombok.Getter;
import lombok.experimental.Accessors;

import java.util.Date;

@Getter
@Accessors(fluent = true)
public class PublicUserDTO {
    private String id;
    private String minecraftUuid;
    private String minecraftName;
    private String role;
    private Date lastSeen;
}
