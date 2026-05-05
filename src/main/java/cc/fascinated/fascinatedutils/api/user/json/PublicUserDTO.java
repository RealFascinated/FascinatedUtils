package cc.fascinated.fascinatedutils.api.user.json;

import cc.fascinated.fascinatedutils.api.user.Presence;

import java.util.Date;

public record PublicUserDTO(int id, String minecraftUuid, String minecraftName, String role, String status,
                             Presence presence, Date lastSeen) {}
