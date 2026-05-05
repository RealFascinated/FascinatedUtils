package cc.fascinated.fascinatedutils.api.internal;

import cc.fascinated.fascinatedutils.api.channel.ChannelDetail;
import cc.fascinated.fascinatedutils.api.channel.ChannelKind;
import cc.fascinated.fascinatedutils.api.channel.ChannelMessage;
import cc.fascinated.fascinatedutils.api.channel.ChannelSummary;
import cc.fascinated.fascinatedutils.api.channel.GroupMember;
import cc.fascinated.fascinatedutils.api.channel.LastMessagePreview;
import cc.fascinated.fascinatedutils.api.channel.json.ChannelDetailWire;
import cc.fascinated.fascinatedutils.api.channel.json.ChannelKindWire;
import cc.fascinated.fascinatedutils.api.channel.json.ChannelMemberWire;
import cc.fascinated.fascinatedutils.api.channel.json.ChannelMessageWire;
import cc.fascinated.fascinatedutils.api.channel.json.ChannelSummaryWire;
import cc.fascinated.fascinatedutils.api.channel.json.LastMessagePreviewWire;
import cc.fascinated.fascinatedutils.api.user.User;
import cc.fascinated.fascinatedutils.api.user.json.PublicUserWire;

import java.util.List;

public class AlumiteModelMapper {

    public static User toUser(PublicUserWire wire) {
        if (wire == null) {
            return null;
        }
        return new User(
                wire.id(),
                wire.minecraftUuid(),
                wire.minecraftName(),
                wire.role(),
                wire.status(),
                wire.presence(),
                wire.lastSeen()
        );
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

    public static ChannelSummary toChannelSummary(ChannelSummaryWire wire) {
        if (wire == null) {
            return null;
        }
        return new ChannelSummary(
                wire.id(),
                toChannelKind(wire.type()),
                wire.name(),
                wire.lastMessageAt(),
                toLastMessagePreview(wire.lastMessagePreview()),
                wire.lastReadMessageId()
        );
    }

    public static ChannelMessage toChannelMessage(ChannelMessageWire wire) {
        if (wire == null) {
            return null;
        }
        return new ChannelMessage(
                wire.id(),
                wire.channelId(),
                wire.authorId(),
                wire.content(),
                wire.createdAt(),
                wire.editedAt()
        );
    }

    public static GroupMember toGroupMember(ChannelMemberWire wire) {
        if (wire == null) {
            return null;
        }
        return new GroupMember(toUser(wire.user()), wire.isOwner());
    }

    public static ChannelDetail toChannelDetail(ChannelDetailWire wire) {
        if (wire == null) {
            return null;
        }
        if (wire instanceof ChannelDetailWire.DmChannelDetailWire dm) {
            return new ChannelDetail.DmChannelDetail(dm.id(), dm.lastReadMessageId(), toUser(dm.recipient()));
        }
        if (wire instanceof ChannelDetailWire.GroupChannelDetailWire group) {
            Integer ownerUserId = group.ownerUserId();
            int ownerId = 0;
            if (ownerUserId != null) {
                ownerId = ownerUserId.intValue();
            }
            List<GroupMember> members = group.members() == null
                    ? List.of()
                    : group.members().stream().map(AlumiteModelMapper::toGroupMember).toList();
            return new ChannelDetail.GroupChannelDetail(
                    group.id(),
                    group.lastReadMessageId(),
                    group.name(),
                    ownerId,
                    members
            );
        }
        return null;
    }

    private AlumiteModelMapper() {
    }
}
