package cc.fascinated.fascinatedutils.api;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Errors {
    INVALID_CHALLENGE("invalid_challenge", "The login challenge expired. Try again."), INVALID_MINECRAFT_TOKEN("invalid_minecraft_token", "Your Minecraft session is invalid. Rejoin and try again."), MINECRAFT_RATE_LIMITED("minecraft_rate_limited", "Minecraft authentication is rate limited right now. Try again in a moment."), MINECRAFT_VERIFY_FAILED("minecraft_verify_failed", "Minecraft account verification failed. Try again."), INVALID_REFRESH_TOKEN("invalid_refresh_token", "Your social session expired. Log in again."), REFRESH_TOKEN_REUSED("refresh_token_reused", "Your social session was invalidated. Log in again."), INVALID_ACCESS_TOKEN("invalid_access_token", "Your social session expired. Log in again."), SUSPENDED_USER("suspended_user", "This account is suspended from social features."), SELF_FRIEND_REQUEST("self_friend_request", "You cannot add yourself."), USER_NOT_FOUND("user_not_found", "That user was not found."), ALREADY_FRIENDS("already_friends", "You are already friends with that user."), FRIEND_REQUEST_EXISTS("friend_request_exists", "A friend request already exists for that user."), FRIEND_REQUEST_NOT_FOUND("friend_request_not_found", "That friend request no longer exists."), NOT_FRIENDS("not_friends", "You are not friends with that user."), CHANNEL_NOT_FOUND("channel_not_found", "That channel no longer exists."), MESSAGE_NOT_FOUND("message_not_found", "That message no longer exists."), NOT_CHANNEL_OWNER("not_channel_owner", "Only the channel owner can do that."), FORBIDDEN_ACTION("forbidden_action", "You cannot do that in this channel."), CANNOT_REMOVE_OWNER("cannot_remove_owner", "Transfer ownership before removing the owner."), INVALID_READ_CURSOR("invalid_read_cursor", "The selected read position is invalid."), SELF_DM("self_dm", "You cannot open a DM with yourself."), GROUP_FULL("group_full", "That group is already at member limit."), BAD_MESSAGE_BODY("bad_message_body", "One or more provided fields are invalid.");

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