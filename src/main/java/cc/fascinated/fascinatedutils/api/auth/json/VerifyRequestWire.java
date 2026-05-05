package cc.fascinated.fascinatedutils.api.auth.json;

public record VerifyRequestWire(String challengeId, String nonce, String minecraftAccessToken) {
}
