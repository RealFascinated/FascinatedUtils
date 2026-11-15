package cc.fascinated.fascinatedutils.api;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Errors {
    INVALID_CHALLENGE("invalid_challenge", "The login challenge expired. Try again."), INVALID_MINECRAFT_TOKEN("invalid_minecraft_token", "Your Minecraft session is invalid. Rejoin and try again."), MINECRAFT_RATE_LIMITED("minecraft_rate_limited", "Minecraft authentication is rate limited right now. Try again in a moment."), MINECRAFT_VERIFY_FAILED("minecraft_verify_failed", "Minecraft account verification failed. Try again."), INVALID_REFRESH_TOKEN("invalid_refresh_token", "Your session expired. Log in again."), REFRESH_TOKEN_REUSED("refresh_token_reused", "Your session was invalidated. Log in again."), INVALID_ACCESS_TOKEN("invalid_access_token", "Your session expired. Log in again."), SUSPENDED_USER("suspended_user", "This account is suspended."), USER_NOT_FOUND("user_not_found", "That user was not found.");

    private final String code;
    private final String displayText;

    public static Errors fromCode(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }

        for (Errors error : values()) {
            if (error.code.equals(code)) {
                return error;
            }
        }

        return null;
    }
}