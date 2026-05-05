package cc.fascinated.fascinatedutils.api.auth.json;

public record ChallengeResponseWire(String challengeId, String nonce, String expiresAt) {
}
