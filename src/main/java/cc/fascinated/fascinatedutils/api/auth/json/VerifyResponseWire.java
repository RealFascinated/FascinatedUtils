package cc.fascinated.fascinatedutils.api.auth.json;

import cc.fascinated.fascinatedutils.api.user.json.UserMeWire;

public record VerifyResponseWire(
        String accessToken,
        String accessExpiresAt,
        String refreshToken,
        String refreshExpiresAt,
        UserMeWire user
) {
}
