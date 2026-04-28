package cc.fascinated.fascinatedutils.api.dto;

public record VerifyRequest(
        String challengeId,
        String nonce,
        String minecraftAccessToken
) {}
