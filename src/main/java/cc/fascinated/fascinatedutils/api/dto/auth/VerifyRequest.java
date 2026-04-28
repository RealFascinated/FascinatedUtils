package cc.fascinated.fascinatedutils.api.dto.auth;

public record VerifyRequest(
        String challengeId,
        String nonce,
        String minecraftAccessToken
) {}
