package cc.fascinated.fascinatedutils.api.internal;

import cc.fascinated.fascinatedutils.api.channel.ChannelKind;
import cc.fascinated.fascinatedutils.api.channel.ChannelMessage;
import cc.fascinated.fascinatedutils.api.channel.GroupMember;
import cc.fascinated.fascinatedutils.api.channel.LastMessagePreview;
import cc.fascinated.fascinatedutils.api.channel.json.ChannelKindDTO;
import cc.fascinated.fascinatedutils.api.channel.json.ChannelMemberDTO;
import cc.fascinated.fascinatedutils.api.channel.json.ChannelMessageDTO;
import cc.fascinated.fascinatedutils.api.channel.json.LastMessagePreviewDTO;
import cc.fascinated.fascinatedutils.api.user.User;
import cc.fascinated.fascinatedutils.api.user.json.PublicUserDTO;

public class AlumiteModelMapper {

    public static User toUser(PublicUserDTO dto) {
        if (dto == null) {
            return null;
        }
        return new User(dto.id(), dto.minecraftUuid(), dto.minecraftName(), dto.role(), dto.banned(), dto.userStatus(), dto.activity(), dto.lastSeen());
    }

    public static ChannelKind toChannelKind(ChannelKindDTO dto) {
        if (dto == null) {
            return ChannelKind.DM;
        }
        return switch (dto) {
            case DM -> ChannelKind.DM;
            case GROUP -> ChannelKind.GROUP;
        };
    }

    public static LastMessagePreview toLastMessagePreview(LastMessagePreviewDTO dto) {
        if (dto == null) {
            return null;
        }
        return new LastMessagePreview(dto.messageId(), dto.content(), dto.authorName());
    }

    public static ChannelMessage toChannelMessage(ChannelMessageDTO dto) {
        if (dto == null) {
            return null;
        }
        return new ChannelMessage(dto.id(), dto.channelId(), dto.authorId(), dto.content(), dto.createdAt(), dto.editedAt());
    }

    public static GroupMember toGroupMember(ChannelMemberDTO dto) {
        if (dto == null) {
            return null;
        }
        return new GroupMember(toUser(dto.user()), dto.isOwner());
    }
}
