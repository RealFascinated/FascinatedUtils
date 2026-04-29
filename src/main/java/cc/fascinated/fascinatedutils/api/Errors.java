package cc.fascinated.fascinatedutils.api;

public enum Errors {
    INVALID_CHALLENGE("invalid_challenge", "The login challenge expired. Try again."),
    INVALID_MINECRAFT_TOKEN("invalid_minecraft_token", "Your Minecraft session is invalid. Rejoin and try again."),
    MINECRAFT_RATE_LIMITED("minecraft_rate_limited", "Minecraft authentication is rate limited right now. Try again in a moment."),
    MINECRAFT_VERIFY_FAILED("minecraft_verify_failed", "Minecraft account verification failed. Try again."),
    INVALID_REFRESH_TOKEN("invalid_refresh_token", "Your social session expired. Log in again."),
    REFRESH_TOKEN_REUSED("refresh_token_reused", "Your social session was invalidated. Log in again."),
    INVALID_ACCESS_TOKEN("invalid_access_token", "Your social session expired. Log in again."),
    SUSPENDED_USER("suspended_user", "This account is suspended from social features."),
    SELF_FRIEND_REQUEST("self_friend_request", "You cannot add yourself."),
    USER_NOT_FOUND("user_not_found", "That user was not found."),
    ALREADY_FRIENDS("already_friends", "You are already friends with that user."),
    FRIEND_REQUEST_EXISTS("friend_request_exists", "A friend request already exists for that user."),
    FRIEND_REQUEST_NOT_FOUND("friend_request_not_found", "That friend request no longer exists."),
    NOT_FRIENDS("not_friends", "You are not friends with that user.");

    private final String code;
    private final String displayText;

    Errors(String code, String displayText) {
        this.code = code;
        this.displayText = displayText;
    }

    public String getCode() {
        return code;
    }

    public String getDisplayText() {
        return displayText;
    }

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