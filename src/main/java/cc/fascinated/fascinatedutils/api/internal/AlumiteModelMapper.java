package cc.fascinated.fascinatedutils.api.internal;

import cc.fascinated.fascinatedutils.api.channel.*;
import cc.fascinated.fascinatedutils.api.channel.json.*;
import cc.fascinated.fascinatedutils.api.user.User;
import cc.fascinated.fascinatedutils.api.user.json.PublicUserWire;

import java.util.List;

public class AlumiteModelMapper {

    private AlumiteModelMapper() {
    }

    public static User toUser(PublicUserWire wire) {
        if (wire == null) {
            return null;
        }
        return new User(wire.id(), wire.minecraftUuid(), wire.minecraftName(), wire.role(), wire.status(), wire.presence(), wire.lastSeen());
    }

    public static ChannelKind toChannelKind(ChannelKindWire wire) {
        if (wire == null) {
            return ChannelKind.DM;
        }
        return switch (wire) {
            case DM -> ChannelKind.DM;
            case GROUP -> ChannelKind.GROUP;
        };
    }

    public static LastMessagePreview toLastMessagePreview(LastMessagePreviewWire wire) {
        if (wire == null) {
            return null;
        }
        return new LastMessagePreview(wire.messageId(), wire.content(), wire.authorName());
    }

    public static ChannelListItem toChannelSummary(ChannelSummaryWire wire) {
        if (wire == null) {
            return null;
        }
        return new ChannelListItem(wire.id(), toChannelKind(wire.type()), wire.name(), wire.lastMessageAt(), toLastMessagePreview(wire.lastMessagePreview()), wire.lastReadMessageId());
    }

    public static ChannelMessage toChannelMessage(ChannelMessageWire wire) {
        if (wire == null) {
            return null;
        }
        return new ChannelMessage(wire.id(), wire.channelId(), wire.authorId(), wire.content(), wire.createdAt(), wire.editedAt());
    }

    public static GroupMember toGroupMember(ChannelMemberWire wire) {
        if (wire == null) {
            return null;
        }
        return new GroupMember(toUser(wire.user()), wire.isOwner());
    }
}
