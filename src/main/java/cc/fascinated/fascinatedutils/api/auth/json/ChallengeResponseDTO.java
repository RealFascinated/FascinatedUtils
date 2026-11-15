package cc.fascinated.fascinatedutils.api.auth.json;

public record ChallengeResponseDTO(String challengeId, String nonce, String expiresAt) {}
