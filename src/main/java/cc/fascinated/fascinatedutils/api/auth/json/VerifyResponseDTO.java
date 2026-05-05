package cc.fascinated.fascinatedutils.api.auth.json;

import cc.fascinated.fascinatedutils.api.user.json.UserDTO;

public record VerifyResponseDTO(String accessToken, String accessExpiresAt, String refreshToken,
                                 String refreshExpiresAt, UserDTO user) {}
