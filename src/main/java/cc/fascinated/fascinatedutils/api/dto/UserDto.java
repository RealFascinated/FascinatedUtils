package cc.fascinated.fascinatedutils.api.dto;

public record UserDto(
        int id,
        String minecraftUuid,
        String minecraftName,
        String role,
        String status
) {}
