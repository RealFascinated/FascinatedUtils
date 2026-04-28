package cc.fascinated.fascinatedutils.api.dto.auth;

import cc.fascinated.fascinatedutils.api.dto.UserDto;

public record VerifyResponse(
        String accessToken,
        String accessExpiresAt,
        String refreshToken,
        String refreshExpiresAt,
        UserDto user
) {}
