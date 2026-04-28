package cc.fascinated.fascinatedutils.api.dto.auth;

public record RefreshResponse(
        String accessToken,
        String accessExpiresAt,
        String refreshToken,
        String refreshExpiresAt
) {}
