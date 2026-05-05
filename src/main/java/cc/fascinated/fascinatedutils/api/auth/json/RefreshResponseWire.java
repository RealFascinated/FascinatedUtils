package cc.fascinated.fascinatedutils.api.auth.json;

public record RefreshResponseWire(
        String accessToken,
        String accessExpiresAt,
        String refreshToken,
        String refreshExpiresAt
) {
}
