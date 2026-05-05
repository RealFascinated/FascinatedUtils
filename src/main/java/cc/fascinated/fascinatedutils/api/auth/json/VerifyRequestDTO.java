package cc.fascinated.fascinatedutils.api.auth.json;

public record VerifyRequestDTO(String challengeId, String nonce, String minecraftAccessToken) {}
