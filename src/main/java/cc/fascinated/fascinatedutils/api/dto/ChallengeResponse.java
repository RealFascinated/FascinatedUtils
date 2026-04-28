package cc.fascinated.fascinatedutils.api.dto;

public record ChallengeResponse(
        String challengeId,
        String nonce,
        String expiresAt
) {}
