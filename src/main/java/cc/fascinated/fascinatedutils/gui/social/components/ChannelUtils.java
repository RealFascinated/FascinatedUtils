package cc.fascinated.fascinatedutils.gui.social.components;

import cc.fascinated.fascinatedutils.api.Alumite;
import cc.fascinated.fascinatedutils.api.channel.Channel;
import cc.fascinated.fascinatedutils.api.channel.DmChannel;
import cc.fascinated.fascinatedutils.api.channel.GroupChannel;
import cc.fascinated.fascinatedutils.api.channel.LastMessagePreview;
import cc.fascinated.fascinatedutils.api.channel.json.AttachmentDTO;
import cc.fascinated.fascinatedutils.api.user.User;
import cc.fascinated.fascinatedutils.api.user.UserStatus;
import cc.fascinated.fascinatedutils.gui.theme.UITheme;
import lombok.experimental.UtilityClass;
import net.minecraft.network.chat.Component;

@UtilityClass
public class ChannelUtils {

    public static String title(Channel channel) {
        if (channel == null) {
            return Component.translatable("alumite.social.dm.title").getString();
        }
        GroupChannel groupChannel = channel.asGroupChannel();
        if (groupChannel != null) {
            if (groupChannel.name() != null && !groupChannel.name().isBlank()) {
                return groupChannel.name();
            }
            return "Channel #" + groupChannel.id();
        }
        DmChannel dmChannel = channel.asDmChannel();
        if (dmChannel != null) {
            User recipient = dmChannel.recipient();
            if (recipient != null && recipient.minecraftName() != null && !recipient.minecraftName().isBlank()) {
                return recipient.minecraftName();
            }
            return Component.translatable("alumite.social.dm.unknown_user").getString();
        }
        return Component.translatable("alumite.social.dm.title").getString();
    }

    public static String previewSnippet(Channel channel) {
        LastMessagePreview preview = channel.lastMessagePreview();
        if (preview == null || preview.content() == null || preview.content().isBlank()) {
            if (preview != null) {
                AttachmentDTO attachment = preview.attachments().getFirst();
                if (attachment != null) {
                    return attachment.name();
                }
            }
            return "";
        }

        // Who sent the message
        String authorName = preview.authorName();
        boolean isSelf = authorName != null && authorName.equals(Alumite.INSTANCE.users().selfUser().user().minecraftName());
        String authorPart = (isSelf ? Component.translatable("alumite.social.dm.preview.you").getString() : authorName) + ": ";

        return authorPart + preview.content();
    }

    public static boolean hasUnread(Channel channel) {
        LastMessagePreview preview = channel.lastMessagePreview();
        if (preview == null) {
            return false;
        }
        String lastReadMessageId = channel.lastReadMessageId();
        if (lastReadMessageId == null) {
            return true;
        }
        return preview.messageId().compareTo(lastReadMessageId) > 0;
    }

    public static String dmAvatarMinecraftUuid(Channel channel) {
        if (channel == null) {
            return null;
        }
        DmChannel dmChannel = channel.asDmChannel();
        if (dmChannel == null) {
            return null;
        }
        User recipient = dmChannel.recipient();
        if (recipient == null || recipient.minecraftUuid() == null || recipient.minecraftUuid().isBlank()) {
            return null;
        }
        return recipient.minecraftUuid();
    }

    public static int dmUserStatusColor(Channel channel) {
        if (channel == null) {
            return UITheme.COLOR_ACCENT;
        }
        DmChannel dmChannel = channel.asDmChannel();
        if (dmChannel == null) {
            return UITheme.COLOR_ACCENT;
        }
        User recipient = dmChannel.recipient();
        if (recipient == null) {
            return UserStatus.OFFLINE.color();
        }
        UserStatus status = recipient.userStatus();
        return (status != null ? status : UserStatus.OFFLINE).color();
    }
}
