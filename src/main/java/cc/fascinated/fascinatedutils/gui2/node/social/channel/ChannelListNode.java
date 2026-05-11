package cc.fascinated.fascinatedutils.gui2.node.social.channel;

import cc.fascinated.fascinatedutils.api.Alumite;
import cc.fascinated.fascinatedutils.api.channel.Channel;
import cc.fascinated.fascinatedutils.api.channel.DmChannel;
import cc.fascinated.fascinatedutils.api.user.User;
import cc.fascinated.fascinatedutils.gui2.core.ScrollColumnNode;
import cc.fascinated.fascinatedutils.gui2.node.TextNode;
import cc.fascinated.fascinatedutils.gui2.node.social.player.PlayerContextMenuHandler;
import cc.fascinated.fascinatedutils.gui2.theme.UiThemeRepository;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.function.Consumer;

public class ChannelListNode extends ScrollColumnNode {

    public ChannelListNode(String selectedChannelId, Consumer<String> onSelectChannel, PlayerContextMenuHandler contextMenuHandler) {
        setGap(2);

        List<Channel> channels = Alumite.INSTANCE != null ? Alumite.INSTANCE.channels().all() : List.of();
        if (channels.isEmpty()) {
            TextNode emptyLabel = new TextNode(() -> Component.translatable("alumite.social.dm.no_conversations").getString())
                    .setColorArgb(UiThemeRepository.get().textSubtle())
                    .setTextAlign(0.5f, 0.5f);
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
            ChannelRowNode row = new ChannelRowNode(channel);
            row.setAvatarSize(18);
            row.setRowHeight(28);
            row.setTextScale(0.85f);
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
            row.setTrailingAction(new CloseChannelNode(dmChannel), CloseChannelNode.SIZE);
            addChild(row);
        }
    }

}