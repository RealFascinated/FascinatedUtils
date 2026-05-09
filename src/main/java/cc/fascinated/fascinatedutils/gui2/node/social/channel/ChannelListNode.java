package cc.fascinated.fascinatedutils.gui2.node.social.channel;

import cc.fascinated.fascinatedutils.api.Alumite;
import cc.fascinated.fascinatedutils.api.channel.Channel;
import cc.fascinated.fascinatedutils.api.channel.DmChannel;
import cc.fascinated.fascinatedutils.api.channel.LastMessagePreview;
import cc.fascinated.fascinatedutils.api.channel.json.AttachmentDTO;
import cc.fascinated.fascinatedutils.api.user.User;
import cc.fascinated.fascinatedutils.gui2.core.ScrollColumnNode;
import cc.fascinated.fascinatedutils.gui2.node.TextNode;
import cc.fascinated.fascinatedutils.gui2.node.social.player.PlayerContextMenuHandler;
import cc.fascinated.fascinatedutils.gui2.node.social.player.PlayerRowNode;
import cc.fascinated.fascinatedutils.gui2.theme.UiThemeRepository;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.function.Consumer;

public class ChannelListNode extends ScrollColumnNode {

    public ChannelListNode(String selectedChannelId, Consumer<String> onSelectChannel, PlayerContextMenuHandler contextMenuHandler) {
        setGap(2);

        List<Channel> channels = Alumite.INSTANCE != null ? Alumite.INSTANCE.channels().all() : List.of();
        if (channels.isEmpty()) {
            TextNode emptyLabel = new TextNode("No conversations yet")
                    .setColorArgb(UiThemeRepository.get().textSubtle())
                    .setTextAlign(0f, 0.5f);
            emptyLabel.fullWidth().height(40);
            addChild(emptyLabel);
            return;
        }

        // todo: add groups support
        for (Channel channel : channels) {
            if (!(channel instanceof DmChannel dmChannel)) {
                continue;
            }
            User recipient = dmChannel.recipient();
            PlayerRowNode row = new PlayerRowNode(() -> recipient, () -> previewSnippet(channel));
            row.setSelected(channel.id().equals(selectedChannelId));
            row.setOnPrimaryClick(() -> {
                if (onSelectChannel != null) {
                    onSelectChannel.accept(channel.id());
                }
            });
            row.setOnSecondaryClick((pointerX, pointerY) -> {
                if (contextMenuHandler != null) {
                    contextMenuHandler.open(recipient, pointerX, pointerY);
                }
            });
            addChild(row);
        }
    }

    private String previewSnippet(Channel channel) {
        LastMessagePreview preview = channel.lastMessagePreview();
        if (preview == null || preview.content() == null || preview.content().isBlank()) {
            if (preview != null) {
                List<AttachmentDTO> attachments = preview.attachments();
                if (attachments != null && !attachments.isEmpty() && attachments.get(0) != null) {
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