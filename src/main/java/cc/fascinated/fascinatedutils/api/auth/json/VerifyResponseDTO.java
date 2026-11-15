package cc.fascinated.fascinatedutils.api.auth.json;

public record VerifyResponseDTO(String accessToken, String accessExpiresAt, String refreshToken,
                                 String refreshExpiresAt) {}
