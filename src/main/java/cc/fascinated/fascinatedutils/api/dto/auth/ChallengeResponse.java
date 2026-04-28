package cc.fascinated.fascinatedutils.api.dto.auth;

public record ChallengeResponse(
        String challengeId,
        String nonce,
        String expiresAt
) {}
