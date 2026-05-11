package cc.fascinated.fascinatedutils.gui2.node.social.channel;

import cc.fascinated.fascinatedutils.api.Alumite;
import cc.fascinated.fascinatedutils.api.channel.Channel;
import cc.fascinated.fascinatedutils.api.channel.DmChannel;
import cc.fascinated.fascinatedutils.api.channel.LastMessagePreview;
import cc.fascinated.fascinatedutils.api.channel.json.AttachmentDTO;
import cc.fascinated.fascinatedutils.gui2.node.social.player.PlayerRowNode;
import net.minecraft.network.chat.Component;

import java.util.List;

public class ChannelRowNode extends PlayerRowNode<ChannelRowNode> {

    public ChannelRowNode(Channel channel) {
        super(
                () -> channel instanceof DmChannel dm ? dm.recipient() : null,
                () -> previewSnippet(channel)
        );
    }

    private static String previewSnippet(Channel channel) {
        LastMessagePreview preview = channel.lastMessagePreview();
        if (preview == null || preview.content() == null || preview.content().isBlank()) {
            if (preview != null) {
                List<AttachmentDTO> attachments = preview.attachments();
                if (!attachments.isEmpty() && attachments.get(0) != null) {
                    return attachments.get(0).name();
                }
            }
            return "";
        }

        String authorName = preview.authorName();
        String selfName = Alumite.INSTANCE != null ? Alumite.INSTANCE.users().selfUser().user().minecraftName() : null;
        boolean isSelf = authorName != null && authorName.equals(selfName);
        String authorPart = (isSelf
                ? Component.translatable("alumite.social.dm.preview.you").getString()
                : authorName) + ": ";

        return authorPart + preview.content();
    }
}
