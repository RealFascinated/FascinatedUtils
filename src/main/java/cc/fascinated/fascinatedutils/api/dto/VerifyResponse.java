package cc.fascinated.fascinatedutils.api.dto;

public record VerifyResponse(
        String accessToken,
        String accessExpiresAt,
        String refreshToken,
        String refreshExpiresAt,
        UserDto user
) {}
