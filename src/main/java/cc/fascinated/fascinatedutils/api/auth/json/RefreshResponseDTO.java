package cc.fascinated.fascinatedutils.api.auth.json;

public record RefreshResponseDTO(String accessToken, String accessExpiresAt, String refreshToken,
                                  String refreshExpiresAt) {}
