package cc.fascinated.fascinatedutils.api.dto;

public record RefreshResponse(
        String accessToken,
        String accessExpiresAt,
        String refreshToken,
        String refreshExpiresAt
) {}
